package videomeeting.pcClient.controller

import akka.actor.typed.ActorRef
import videomeeting.pcClient.Boot
import videomeeting.pcClient.common.{Constants, StageContext}
import videomeeting.pcClient.component.WarningDialog
import videomeeting.pcClient.core.RmManager
import videomeeting.pcClient.core.RmManager.HeartBeat
import videomeeting.pcClient.scene.StartScene
import videomeeting.pcClient.scene.StartScene.{SpeakListInfo, StartSceneListener}
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol._
import org.slf4j.LoggerFactory
import videomeeting.pcClient.utils.RMClient
import videomeeting.pcClient.Boot.executor
import videomeeting.pcClient.core.collector.CaptureActor

/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 12:33
  */
class StartController(
  context: StageContext,
  startScene: StartScene,
  rmManager: ActorRef[RmManager.RmCommand]
) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)
  var isSaying = false
  var isLive = false

  def showScene(): Unit = {
    Boot.addToPlatform(
      if (RmManager.userInfo.nonEmpty && RmManager.roomInfo.nonEmpty) {
        context.switchScene(startScene.getScene, title = s"${RmManager.userInfo.get.userName}的直播间-${RmManager.roomInfo.get.meetingName}")
      } else {
        WarningDialog.initWarningDialog(s"无房间信息！")
      }
    )
  }

  startScene.setListener(new StartSceneListener {
    override def startLive(): Unit = {
      //这个主播是否一进入就开播
      rmManager ! RmManager.HostLiveReq
    }

    override def stopLive(): Unit = {
      rmManager ! RmManager.StopLive
    }

    //只能一个人发言
    override def audienceAcceptance(userId: Long, accept: Boolean, newRequest: SpeakListInfo): Unit = {
      if (!isSaying) {
        rmManager ! RmManager.AudienceAcceptance(userId, accept)
        // startScene.audObservableList.remove(newRequest)
      } else {
        if (isSaying && !accept) {
          rmManager ! RmManager.AudienceAcceptance(userId, accept)
          //  startScene.audObservableList.remove(newRequest)
        } else {
          Boot.addToPlatform {
            WarningDialog.initWarningDialog(s"无法让多人同时发言")
          }
        }
      }
    }

    override def shutJoin(): Unit = {
      rmManager ! RmManager.ShutJoin
    }

    override def gotoHomeScene(): Unit = {
      rmManager ! RmManager.BackToHome
    }

    override def ask4Loss(): Unit = {
      rmManager ! RmManager.GetPackageLoss
    }
  }
  )

  def wsMessageHandle(data: WsMsgRm): Unit = {
    data match {

      case msg: HeatBeat =>
        //        log.debug(s"heartbeat: ${msg.ts}")
        rmManager ! HeartBeat

      case msg: StartLiveRsp =>
        //        log.debug(s"get StartLiveRsp: $msg")
        if (msg.errCode == 0) {
          rmManager ! RmManager.StartLive(msg.liveInfo.get.liveId, msg.liveInfo.get.liveCode)
        } else {
          Boot.addToPlatform {
            WarningDialog.initWarningDialog(s"${msg.msg}")
          }
        }

      case msg: AudienceJoin =>
        //将该条信息展示在host页面(TableView)
        log.debug(s"Audience-${msg.userName} send join req.")
        Boot.addToPlatform {
//          startScene.updateAudienceList(msg.userId, msg.userName)
        }


//      case msg: AudienceJoinRsp =>
//        if (msg.errCode == 0) {
//          //显示连线观众信息
//          rmManager ! RmManager.JoinBegin(msg.joinInfo.get)
//
////          Boot.addToPlatform {
//          ////            if (!startScene.tb2.isSelected) {
//          ////              startScene.tb2.setGraphic(startScene.connectionIcon1)
//          ////            }
//          ////            startScene.connectionStateText.setText(s"与${msg.joinInfo.get.userName}连线中")
//          ////            startScene.connectStateBox.getChildren.add(startScene.shutConnectionBtn)
//          ////            isConnecting = true
//          ////          }
//
//        } else {
//          Boot.addToPlatform {
//            WarningDialog.initWarningDialog(s"观众加入出错:${msg.msg}")
//          }
//        }

      case AudienceDisconnect(liveId) =>
        //观众断开，提醒主播，去除连线观众信息
        rmManager ! RmManager.JoinStop
        Boot.addToPlatform {
//          if (!startScene.tb3.isSelected) {
//            startScene.tb3.setGraphic(startScene.connectionIcon1)
//          }
//          startScene.connectionStateText.setText(s"目前状态：无连接")
//          startScene.connectStateBox.getChildren.remove(startScene.shutConnectionBtn)
//          isConnecting = false
        }

      case msg: RcvComment =>
        //判断userId是否为-1，是的话当广播处理
        //        log.info(s"receive comment msg: ${msg.userName}-${msg.comment}")
        Boot.addToPlatform {
//          startScene.commentBoard.updateComment(msg)
//          startScene.barrage.updateBarrage(msg)
        }

      case msg: UpdateAudienceInfo =>
        //        log.info(s"update audienceList.")
        Boot.addToPlatform {
//          startScene.watchingList.updateWatchingList(msg.AudienceList)
        }


      case HostStopPushStream2Client =>
        Boot.addToPlatform {
          WarningDialog.initWarningDialog("直播成功停止，已通知所有观众。")
        }

      case BanOnAnchor =>
        Boot.addToPlatform {
          WarningDialog.initWarningDialog("你的直播已被管理员禁止！")
        }
        rmManager ! RmManager.BackToHome

      case x =>
        log.warn(s"host recv unknown msg from rm: $x")
    }

  }

}

