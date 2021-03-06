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
import videomeeting.pcClient.core.RmManager.GetRoomDetail
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}

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
    val rspFuture = RMClient.getRoomList
    val rsp = Await.result(rspFuture, 1.second)
    rsp match {
      case Right(rst) =>
        if (rst.errCode == 0) {
          Boot.addToPlatform {
            removeLoading()
            findScene.roomList = rst.meetingList.get
            log.info(findScene.roomList.toString())
//            findScene.updateRoomList(roomList = findScene.roomList)
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
    override def enter(meetingId: Int, timestamp: Long = 0L): Unit = {
      Boot.addToPlatform {
        refreshList
        if (findScene.roomList.exists(_.meetingId == meetingId)) {
          rmManager ! GetRoomDetail(findScene.roomList.find(_.meetingId == meetingId).get.meetingId)
        } else {
          WarningDialog.initWarningDialog("会议号不存在")
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
      context.switchScene(findScene.getScene, title = "会议室online")
    }
  }

  def showLoading(): Unit = {
//    Boot.addToPlatform {
//      if (!hasWaitingGif) {
//        findScene.group.getChildren.add(findScene.waitingGif)
//        hasWaitingGif = true
//      }
//    }
  }

  def removeLoading(): Unit = {
//    Boot.addToPlatform {
//      if (hasWaitingGif) {
//        findScene.group.getChildren.remove(findScene.waitingGif)
//        hasWaitingGif = false
//      }
//    }
  }

}
