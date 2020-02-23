package videomeeting.pcClient.controller

import java.io.File

import videomeeting.pcClient.scene.FindScene.FindSceneListener
import akka.actor.typed.ActorRef
import javafx.geometry.{Insets, Pos}
import javafx.scene.Group
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control._
import javafx.scene.layout.VBox
import javafx.stage.{FileChooser, Stage}
import videomeeting.pcClient.Boot
import videomeeting.pcClient.common._
import videomeeting.pcClient.core.RmManager
import videomeeting.pcClient.scene.FindScene
import videomeeting.pcClient.utils.RMClient
import videomeeting.pcClient.Boot.executor
import videomeeting.pcClient.component.WarningDialog
import videomeeting.pcClient.core.RmManager.{GetRecordDetail, GetRoomDetail, GoToWatch}
import videomeeting.protocol.ptcl.CommonInfo.{RecordInfo, MeetingInfo}
import org.slf4j.LoggerFactory

/**
  * Author: Administrator
  * Date: 2020/2/6/006
  * Time: 23:23
  */
class FindController (
  context: StageContext,
  findScene: FindScene,
  rmManager: ActorRef[RmManager.RmCommand]
) {
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  //  private var roomList: List[RoomInfo] = Nil
  //  private var recordList: List[RecordInfo] = Nil
  var hasWaitingGif = false

  def refreshList = {
    Boot.addToPlatform {
      showLoading()
      updateRoomList()
    }
  }

  def updateRoomList(): Unit = {
    RMClient.getRoomList.map {
      case Right(rst) =>
        if (rst.errCode == 0) {
          Boot.addToPlatform {
            removeLoading()
            findScene.roomList = rst.meetingList.get
            findScene.updateRoomList(roomList = findScene.roomList)
          }
        } else {
          removeLoading()
          Boot.addToPlatform(
            WarningDialog.initWarningDialog(s"${rst.msg}")
          )
        }
      case Left(e) =>
        log.error(s"get room list error: $e")
        removeLoading()
        Boot.addToPlatform(
          WarningDialog.initWarningDialog("获取房间列表失败")
        )
    }
  }

  findScene.setListener(new FindSceneListener {
    override def enter(meetingId: Long, timestamp: Long = 0L): Unit = {
      Boot.addToPlatform {
        showLoading()
        if (findScene.roomList.exists(_.meetingId == meetingId)) {
          rmManager ! GetRoomDetail(findScene.roomList.find(_.meetingId == meetingId).get.meetingId)
        } else {
          removeLoading()
        }
      }
    }

    override def refresh(): Unit = {
      refreshList
    }


    override def gotoHomeScene(): Unit = {
      rmManager ! RmManager.BackToHome
    }
  })

  def showScene(): Unit = {
    Boot.addToPlatform {
      updateRoomList()
      context.switchScene(findScene.getScene, title = "直播间online")
    }
  }

  def showLoading(): Unit = {
    Boot.addToPlatform {
      if (!hasWaitingGif) {
        findScene.group.getChildren.add(findScene.waitingGif)
        hasWaitingGif = true
      }
    }
  }

  def removeLoading(): Unit = {
    Boot.addToPlatform {
      if (hasWaitingGif) {
        findScene.group.getChildren.remove(findScene.waitingGif)
        hasWaitingGif = false
      }
    }
  }

}
