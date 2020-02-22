package videomeeting.pcClient.controller

import java.io.File

import akka.Done
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.util.{ByteString, ByteStringBuilder}
import javafx.scene.control.Tooltip
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import videomeeting.pcClient.Boot
import videomeeting.pcClient.controller.AudienceController
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol._
import videomeeting.pcClient.Boot.{executor, materializer, scheduler, system, timeout}
import videomeeting.pcClient.common.Constants.{AudienceStatus, HostStatus}
import videomeeting.pcClient.common._
import videomeeting.pcClient.component.WarningDialog
import videomeeting.pcClient.core.player.VideoPlayer
import videomeeting.pcClient.core.stream.LiveManager.{JoinInfo, WatchInfo}
import videomeeting.pcClient.core.stream.LiveManager
import videomeeting.pcClient.scene.{AudienceScene, FindScene, HomeScene, StartScene}
import videomeeting.pcClient.utils.RMClient
import videomeeting.protocol.ptcl.CommonInfo._
import org.slf4j.LoggerFactory
import videomeeting.player.sdk.MediaPlayer
import videomeeting.pcClient.core.RmManager
import videomeeting.pcClient.core.RmManager.HeartBeat
import videomeeting.pcClient.scene.AudienceScene.AudienceSceneListener

import scala.concurrent.Future
import scala.util.{Failure, Success}
import concurrent.duration._
import scala.collection.mutable
//import scala.concurrent.Future

/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 17:00
  */
class AudienceController(
  context: StageContext,
  audienceScene: AudienceScene,
  rmManager: ActorRef[RmManager.RmCommand]
) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)
//  var likeNum: Int = audienceScene.getRoomInfo.like
  var updateRecCmt = true

  def showScene(): Unit = {
    Boot.addToPlatform {
      //每 5秒更新一次留言
      context.switchScene(audienceScene.getScene, title = s"${audienceScene.getRoomInfo.userId}的直播间-${audienceScene.getRoomInfo.meetingId}")
    }

  }


  audienceScene.setListener(new AudienceSceneListener {

    override def joinReq(meetingId: Long): Unit = {
      if (RmManager.userInfo.nonEmpty) {
        WarningDialog.initWarningDialog("连线申请已" +
                                        "发送！")
        rmManager ! RmManager.JoinRoomReq(meetingId)
      } else {
        WarningDialog.initWarningDialog("请先登录哦~")
      }

    }

    override def quitJoin(meetingId: Long): Unit = {
      if (RmManager.userInfo.nonEmpty) {
        rmManager ! RmManager.ExitJoin(meetingId)
      } else {
        WarningDialog.initWarningDialog("请先登录哦~")
      }
    }

    override def gotoHomeScene(): Unit = {
      updateRecCmt = false
      rmManager ! RmManager.BackToHome
    }



    override def changeOption(needImage: Boolean, needSound: Boolean): Unit = {
      rmManager ! RmManager.ChangeOption4Audience(needImage, needSound)
    }

    override def ask4Loss(): Unit = {
      rmManager ! RmManager.GetPackageLoss
    }


  })

  def wsMessageHandle(data: WsMsgRm): Unit = {

    Boot.addToPlatform {
      data match {
        case msg: HeatBeat =>
//          log.debug(s"heartbeat: ${msg.ts}")
          rmManager ! HeartBeat


        case msg: JoinRsp =>
          if (msg.errCode == 0) {
            rmManager ! RmManager.StartJoin(msg.hostLiveId.get, msg.joinInfo.get)
            audienceScene.hasReqJoin = false
          } else if (msg.errCode == 300001) {
            WarningDialog.initWarningDialog("房主未开通连线功能!")
            audienceScene.hasReqJoin = false
          } else if (msg.errCode == 300002) {
            WarningDialog.initWarningDialog("房主拒绝连线申请!")
            audienceScene.hasReqJoin = false
          }

        case msg:Join4AllRsp=>
          if (msg.errCode == 0) {
            rmManager ! RmManager.PullFromProcessor(msg.mixLiveId.get)
          }else{
            WarningDialog.initWarningDialog("转接错误 test")
          }

        case HostDisconnect(liveId) =>
          Boot.addToPlatform {
            WarningDialog.initWarningDialog("主播已断开连线~")
          }
          rmManager ! RmManager.StopJoinAndWatch(liveId)


        case HostCloseRoom() =>
          Boot.addToPlatform {
            WarningDialog.initWarningDialog("房主连接断开，互动功能已关闭！")
          }

        case AudienceDisconnect(liveId) =>
          if(audienceScene.audienceStatus == AudienceStatus.LIVE){
            Boot.addToPlatform {
              WarningDialog.initWarningDialog("连线者已断开连线~")
            }
            rmManager ! RmManager.StopJoinAndWatch(liveId)
          }

        case msg: UpdateAudienceInfo =>
          //          log.info(s"update audienceList.")
          Boot.addToPlatform {
            audienceScene.watchingList.updateWatchingList(msg.AudienceList)
          }

        case msg: LikeRoomRsp =>
        //          log.debug(s"audience receive likeRoomRsp: ${msg}")
        case HostStopPushStream2Client =>
          Boot.addToPlatform({
            WarningDialog.initWarningDialog("会议已停止，请退出该房间~")
          })

        case x =>
          log.warn(s"audience recv unknown msg from rm: $x")


      }
    }
  }


}
