package videomeeting.pcClient.controllor

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
import videomeeting.pcClient.controllor.AudienceController
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol._
import videomeeting.pcClient.Boot.{executor, materializer, scheduler, system, timeout}
import videomeeting.pcClient.common.Constants.{AudienceStatus, HostStatus}
import videomeeting.pcClient.common._
import videomeeting.pcClient.component.WarningDialog
import videomeeting.pcClient.core.player.VideoPlayer
import videomeeting.pcClient.core.stream.LiveManager.{JoinInfo, WatchInfo}
import videomeeting.pcClient.core.stream.LiveManager
import videomeeting.pcClient.scene.{AudienceScene, HomeScene,StartScene,FindScene}
import videomeeting.pcClient.utils.RMClient
import videomeeting.protocol.ptcl.CommonInfo._
import org.slf4j.LoggerFactory
import videomeeting.player.sdk.MediaPlayer
import videomeeting.pcClient.core.RmManager
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
  var likeNum: Int = audienceScene.getRoomInfo.like
  var updateRecCmt = true

  def showScene(): Unit = {
    Boot.addToPlatform {
      //每 5秒更新一次留言
      if(audienceScene.getIsRecord) {
        Future{
          while (updateRecCmt) {
            updateRecCommentList()
            Thread.sleep(5000)
          }
        }
      }
      context.switchScene(audienceScene.getScene, title = s"${audienceScene.getRoomInfo.userId}的直播间-${audienceScene.getRoomInfo.roomId}")
    }

  }

  def updateRecCommentList(): Unit = {
    RMClient.getRecCommentList(audienceScene.getRecordInfo.roomId, audienceScene.getRecordInfo.startTime).map {
      case Right(rst) =>
        if (rst.errCode == 0) {
          Boot.addToPlatform {
//            log.debug(s"${System.currentTimeMillis()},update recCommentList success:${rst.recordCommentList}")
            audienceScene.barrage.refreshRecBarrage(rst.recordCommentList)
            audienceScene.recCommentBoard.updateCommentsList(rst.recordCommentList)
          }
        } else {
          Boot.addToPlatform(
            WarningDialog.initWarningDialog(s"${rst.msg}")
          )
        }
      case Left(e) =>
        log.error(s"getRecCommentList error: $e")
        Boot.addToPlatform(
          WarningDialog.initWarningDialog(s"获取评论失败:$e")
        )
    }
  }

  def addRecComment(
    roomId:Long,          //录像的房间id
    recordTime:Long,      //录像的时间戳
    comment:String,       //评论内容
    commentTime:Long,     //评论的时间
    relativeTime:Long,    //相对视频的时间
    commentUid:Long,      //评论的用户id
    authorUidOpt:Option[Long] = None
  ): Unit = {
    RMClient.addRecComment(roomId, recordTime, comment, commentTime, relativeTime, commentUid, authorUidOpt).map {
      case Right(rst) =>
        if (rst.errCode == 0) {
          log.debug(s"audience send recordComment success: ${(roomId, recordTime, comment, commentTime, relativeTime, commentUid, authorUidOpt)}")
          //发送评论后重新获取评论列表
          updateRecCommentList()
        } else {
          log.debug(s"rst: $rst")
          Boot.addToPlatform(
            WarningDialog.initWarningDialog(s"audience send recComment failed: ${rst.msg}")
          )
        }
      case Left(e) =>
        log.error(s"addRecComment error: $e")
        Boot.addToPlatform(
          WarningDialog.initWarningDialog(s"send recComment failed: $e")
        )
    }
  }

  audienceScene.setListener(new AudienceSceneListener {
    override def sendCmt(comment: Comment): Unit = {
      if (RmManager.userInfo.nonEmpty) {
        log.debug(s"audience send comment：$comment")
        rmManager ! RmManager.SendComment(comment)
      } else {
        WarningDialog.initWarningDialog("请先登录哦~")
      }
    }

    override def joinReq(roomId: Long): Unit = {
      if (RmManager.userInfo.nonEmpty) {
        WarningDialog.initWarningDialog("连线申请已" +
                                        "发送！")
        rmManager ! RmManager.JoinRoomReq(roomId)
      } else {
        WarningDialog.initWarningDialog("请先登录哦~")
      }

    }

    override def quitJoin(roomId: Long): Unit = {
      if (RmManager.userInfo.nonEmpty) {
        rmManager ! RmManager.ExitJoin(roomId)
      } else {
        WarningDialog.initWarningDialog("请先登录哦~")
      }
    }

    override def gotoHomeScene(): Unit = {
      updateRecCmt = false
      rmManager ! RmManager.BackToHome
    }

    override def setFullScreen(isRecord: Boolean): Unit = {
      if (!audienceScene.isFullScreen) {
        audienceScene.removeAllElement()
        //        context.getStage.setFullScreenExitHint("s")
        context.getStage.setFullScreen(true)
        if (isRecord) {
          audienceScene.recView.setLayoutX(0)
          audienceScene.recView.setLayoutY(0)
          audienceScene.recView.setFitWidth(context.getStageWidth)
          audienceScene.recView.setFitHeight(context.getStageHeight)
        }
        else {
          audienceScene.imgView.setLayoutX(0)
          audienceScene.imgView.setLayoutY(0)
          audienceScene.imgView.setWidth(context.getStageWidth)
          audienceScene.imgView.setHeight(context.getStageHeight)
          audienceScene.statisticsCanvas.setLayoutX(0)
          audienceScene.statisticsCanvas.setLayoutY(0)
          audienceScene.statisticsCanvas.setWidth(context.getStageWidth)
          audienceScene.statisticsCanvas.setHeight(context.getStageHeight)
          audienceScene.gc.drawImage(audienceScene.backImg, 0, 0, context.getStageWidth, context.getStageHeight)
        }
        audienceScene.isFullScreen = true
      }
    }

    override def exitFullScreen(isRecord: Boolean): Unit = {
      if (audienceScene.isFullScreen) {
        audienceScene.imgView.setWidth(Constants.DefaultPlayer.width)
        audienceScene.imgView.setHeight(Constants.DefaultPlayer.height)
        audienceScene.barrageCanvas.setWidth(Constants.DefaultPlayer.width)
        audienceScene.barrageCanvas.setHeight(Constants.DefaultPlayer.height)
        audienceScene.statisticsCanvas.setWidth(Constants.DefaultPlayer.width)
        audienceScene.statisticsCanvas.setHeight(Constants.DefaultPlayer.height)

        if (isRecord) {
          audienceScene.recView.setFitWidth(Constants.DefaultPlayer.width)
          audienceScene.recView.setFitHeight(Constants.DefaultPlayer.height)
        }

        audienceScene.addAllElement()
        context.getStage.setFullScreen(false)
        audienceScene.isFullScreen = false
      }
    }

    override def like(userId: Long, roomId: Long, UpDown: Int): Unit = {
      if (RmManager.userInfo.nonEmpty) {
        rmManager ! RmManager.SendLikeRoom(LikeRoom(userId, roomId, UpDown))
        if (UpDown == 1) {
          log.debug(s"audience send a like.")
        } else {
          log.debug(s"audience send un unlike.")
        }
      } else {
        WarningDialog.initWarningDialog("请先登录哦~")
      }
    }

    override def changeOption(needImage: Boolean, needSound: Boolean): Unit = {
      rmManager ! RmManager.ChangeOption4Audience(needImage, needSound)
    }

    override def ask4Loss(): Unit = {
      rmManager ! RmManager.GetPackageLoss
    }

    override def continuePlayRec(recordInfo: RecordInfo): Unit = {
      rmManager ! RmManager.ContinuePlayRec(recordInfo)

    }

    override def pausePlayRec(recordInfo: RecordInfo): Unit = {
      rmManager ! RmManager.PausePlayRec(recordInfo)

    }

    override def sendRecCmt(comment:String, commentTime:Long, relativeTime: Long, authorUidOpt:Option[Long]): Unit = {
      log.debug(s"audience send recCommend: comment:$comment, commentTime: $commentTime, relativeTime: $relativeTime, authorUidOpt: $authorUidOpt")
      addRecComment(audienceScene.getRecordInfo.roomId, audienceScene.getRecordInfo.startTime, comment, commentTime, relativeTime, RmManager.userInfo.get.userId, authorUidOpt)

    }

    override def refreshRecCmt(): Unit = {
//      updateRecCommentList()

    }

  })

  def wsMessageHandle(data: WsMsgRm): Unit = {

    Boot.addToPlatform {
      data match {
        case msg: HeatBeat =>
//          log.debug(s"heartbeat: ${msg.ts}")
          rmManager ! HeartBeat

        case msg: RcvComment =>
          //判断userId是否为-1，是的话当广播处理
//          log.debug(s"receive comment: ${msg.comment}")
          Boot.addToPlatform {
            audienceScene.commentBoard.updateComment(msg)
            audienceScene.barrage.updateBarrage(msg)
          }


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

        case msg: JudgeLikeRsp =>
          //          log.debug(s"audience receive judgeLikeRsp: ${msg.like}")
          Boot.addToPlatform {
            if (msg.like) { //已经点过赞
              audienceScene.likeBtn.setSelected(true)
              audienceScene.likeBtn.setGraphic(audienceScene.likeIcon)
            } else { //没有点过赞
              audienceScene.likeBtn.setSelected(false)
              audienceScene.likeBtn.setGraphic(audienceScene.unLikeIcon)
            }
          }

        case msg: LikeRoomRsp =>
        //          log.debug(s"audience receive likeRoomRsp: ${msg}")

        case msg: ReFleshRoomInfo =>
          //          log.debug(s"audience receive likeNum update: ${msg.roomInfo.like}")
          likeNum = msg.roomInfo.like
          Boot.addToPlatform {
            audienceScene.likeNum.setText(likeNum.toString)
          }

        case HostStopPushStream2Client =>
          Boot.addToPlatform({
            WarningDialog.initWarningDialog("主播已停止直播，请换个房间观看哦~")
            audienceScene.BtnDis()
          })

        case x =>
          log.warn(s"audience recv unknown msg from rm: $x")


      }
    }
  }


}
