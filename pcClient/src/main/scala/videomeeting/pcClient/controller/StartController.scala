package videomeeting.pcClient.controller

import akka.actor.typed.ActorRef
import videomeeting.pcClient.{Boot, component}
import videomeeting.pcClient.common.{Constants, StageContext}
import videomeeting.pcClient.component.WarningDialog
import videomeeting.pcClient.core.RmManager
import videomeeting.pcClient.core.RmManager.HeartBeat
import videomeeting.pcClient.scene.StartScene
import videomeeting.pcClient.scene.StartScene.{JoinListInfo, SpeakListInfo, StartSceneListener}
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol._
import org.slf4j.LoggerFactory
import videomeeting.pcClient.utils.RMClient
import videomeeting.pcClient.Boot.executor
import videomeeting.pcClient.core.collector.CaptureActor
import videomeeting.protocol.ptcl.CommonInfo.UserDes

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
      if (RmManager.userInfo.nonEmpty ) {
        context.switchScene(startScene.getScene, title = s"${RmManager.userInfo.get.userName}的直播间")
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
    override def audienceAcceptance(userId: Int, accept: Boolean, newRequest: SpeakListInfo): Unit = {
      if (!isSaying) {
        rmManager ! RmManager.AudienceAcceptance(userId, accept)
        startScene.speakObservableList.remove(newRequest)
      } else {
        if (isSaying && !accept) {
//          rmManager ! RmManager.AudienceAcceptance(userId, accept)
          startScene.speakObservableList.remove(newRequest)
        } else {
          Boot.addToPlatform {
            WarningDialog.initWarningDialog(s"无法让多人同时发言")
          }
        }
      }
    }

    override def joinAcceptance(userId: Int, accept: Boolean, newRequest: JoinListInfo): Unit = {
      rmManager ! RmManager.JoinAcceptance(userId, accept)
      startScene.joinObservableList.remove(newRequest)
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

    override def inviteAudience(meetingId: String, email: String): Unit = {
      rmManager ! RmManager.InviteAudience(meetingId, email)
    }

    override def modifyRoomInfo(name: Option[String], des: Option[String]): Unit = {
      rmManager ! RmManager.ModifyRoom(name, des)
    }

    override def forceExit(userId: Int, userName: String): Unit = {
      rmManager ! RmManager.ForceExit(userId, userName)
    }

    override def beHost(): Unit = {

    }

    override def closeImage(userId: Int): Unit = {
      rmManager ! RmManager.CloseUser(userId, Some(false), None)
    }

    override def closeSound(userId: Int): Unit = {
      rmManager ! RmManager.CloseUser(userId, None, Some(false))
    }

    //audience
    override def joinReq(meetingId: Int): Unit = {
      if (RmManager.userInfo.nonEmpty) {
        WarningDialog.initWarningDialog("加入会议申请已发送")
        rmManager ! RmManager.JoinRoomReq(meetingId)
      } else {
        WarningDialog.initWarningDialog("请先登录哦~")
      }

    }

    override def quitJoin(meetingId: Int, userId: Int): Unit = {
      if (RmManager.userInfo.nonEmpty) {
        rmManager ! RmManager.ExitJoin(meetingId, userId)
      } else {
        WarningDialog.initWarningDialog("请先登录哦~")
      }
    }

    override def changeOption(needImage: Boolean, needSound: Boolean): Unit = {
      rmManager ! RmManager.ChangeOption4Audience(needImage, needSound)
    }


    override def applySpeak(meetingId: Int): Unit = {
      rmManager ! RmManager.ApplySpeak(meetingId)
    }


  }

  )

  def wsMessageHandle(data: WsMsgRm): Unit = {
    data match {

      case msg: HeatBeat =>
        //        log.debug(s"heartbeat: ${msg.ts}")
        rmManager ! HeartBeat

      case msg: StartLiveRsp =>
        log.info(s"===========get StartLiveRsp: $msg")
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
          startScene.updateJoinList(msg.userId, msg.userName)
        }

      case msg: AudienceApply =>
        log.debug(s"Attendance-${msg.userName} send apply req.")
        Boot.addToPlatform {
          startScene.updateAudienceList(msg.userId, msg.userName)
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

      case InviteRsp =>
        Boot.addToPlatform {
          WarningDialog.initWarningDialog("邀请邮件已发送")
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

      case msg: HostSetSpeaker =>
        rmManager ! RmManager.SpeakerChange(msg.userId)

      case msg: ModifyRoomRsp =>
        //若失败，信息改成之前的信息
        if (msg.errCode == 0) {
          Boot.addToPlatform {
            WarningDialog.initWarningDialog("更改房间信息成功！")
          }
        } else {
          log.debug(s"更改房间信息失败！原房间信息为：${startScene.roomInfoMap}")
          Boot.addToPlatform {
            val roomName = startScene.roomInfoMap(RmManager.roomInfo.get.meetingId).head
            val roomDes = startScene.roomInfoMap(RmManager.roomInfo.get.meetingId)(1)
            startScene.roomNameField.setText(roomName)
            startScene.roomDesArea.setText(roomDes)
          }
        }

      case msg: UpdateAudienceInfo =>
        Boot.addToPlatform {
          startScene.watchingList.updateWatchingList(msg.AudienceList.map(u => UserDes(u.userId, u.userName, u.headImgUrl)))
        }

      case msg:AudienceJoinRsp=>
        Boot.addToPlatform {
          startScene.updateAudienceList(msg.joinInfo.get.userId, msg.joinInfo.get.userName)
        }
        rmManager ! RmManager.JoinBegin(msg.joinInfo.get,msg.mixID)

      case x =>
        log.warn(s"host recv unknown msg from rm: $x")
    }

  }

}

