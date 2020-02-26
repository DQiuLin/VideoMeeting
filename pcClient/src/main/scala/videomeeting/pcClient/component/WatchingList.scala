package videomeeting.pcClient.component

import javafx.beans.property.{ObjectProperty, SimpleObjectProperty, SimpleStringProperty, StringProperty}
import javafx.collections.{FXCollections, ObservableList}
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.{Button, TableColumn, TableView, ToggleButton}
import javafx.scene.image.ImageView
import javafx.scene.text.Text
import videomeeting.pcClient.common.Pictures
import videomeeting.protocol.ptcl.CommonInfo
import videomeeting.protocol.ptcl.CommonInfo.{UserDes, UserInfo}
import org.slf4j.LoggerFactory


/**
  * Author: zwq
  * Date: 2019/9/12
  * Time: 15:16
  */
object WatchingList{

  case class WatchingListInfo(
    header: ObjectProperty[ImageView],
    userInfo: StringProperty
  )
  {
    def getHeader: ImageView = header.get()

    def setHeader(headerImg: ImageView): Unit = header.set(headerImg)

    def getUserInfo: String = userInfo.get()

    def setUserInfo(info: String): Unit = userInfo.set(info)
  }

}
class WatchingList(headerColWidth: Double, infoColWidth: Double, tableHeight: Double, tb: Option[ToggleButton]) {
  import WatchingList._
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  val audienceIcon1 = new ImageView("img/watching1.png")
  audienceIcon1.setFitWidth(20)
  audienceIcon1.setFitHeight(20)

  var watchingList: ObservableList[WatchingListInfo] = FXCollections.observableArrayList()
  var watchingNum = 0

  val watchingState = new Text(s"有${watchingNum}人正在参加会议")
  watchingState.getStyleClass.add("hostScene-leftArea-text")

  /*update*/
  def updateWatchingList(list: List[UserInfo]): Unit = {
    if(tb.nonEmpty){
      if (!tb.get.isSelected) {
        tb.get.setGraphic(audienceIcon1)
      }
    }
    watchingState.setText(s"有${list.length}人正在参加会议:")
    if (list.size < watchingList.size()) { // Audience leave, reduce from watchingList.
      var removePos = 0
      for (i <- 0 until watchingList.size()) {
        if (list.filter(l => s"StringProperty [value: ${l.userName}(${l.userId})]" == watchingList.get(i).userInfo.toString) == List()) {
          removePos = i
        }
      }
      watchingList.remove(removePos)
    }
    if (list.size > watchingList.size()) { // Audience come, add to watchingList.
      var addList = List[CommonInfo.UserDes]()
      list.foreach { l =>
        var add = l
        for (i <- 0 until watchingList.size()) {
          if (watchingList.get(i).userInfo.toString == s"StringProperty [value: ${l.userName}(${l.userId})]")
            add = null
        }
        if (add == l) {
          addList = add :: addList
        }
      }
//      log.debug(s"addList:$addList")
      addList.foreach { l =>
        val imgUrl = if (l.headImgUrl.nonEmpty) l.headImgUrl else "img/header.png"
        val headerImg = Pictures.getPic(imgUrl)
        headerImg.setFitHeight(25)
        headerImg.setFitWidth(25)
        val newRequest = WatchingListInfo(
          new SimpleObjectProperty[ImageView](headerImg),
          new SimpleStringProperty(s"${l.userName}(${l.userId})")
        )
        watchingList.add(0, newRequest)
      }

    }

  }


  /*table*/
  val watchingTable = new TableView[WatchingListInfo]()
  watchingTable.getStyleClass.add("table-view")

  val headerCol = new TableColumn[WatchingListInfo, ImageView]("头像")
  headerCol.setCellValueFactory(new PropertyValueFactory[WatchingListInfo, ImageView]("header"))
//  headerCol.setPrefWidth(width * 0.1)
  headerCol.setPrefWidth(headerColWidth)


  val userInfoCol = new TableColumn[WatchingListInfo, String]("用户信息")
  userInfoCol.setCellValueFactory(new PropertyValueFactory[WatchingListInfo, String]("userInfo"))
//  userInfoCol.setPrefWidth(width * 0.15)
  userInfoCol.setPrefWidth(infoColWidth)

  val toBeHostCol = new TableColumn[WatchingList.WatchingListInfo, Button]("成为主持人")
 // toBeHostCol.setPrefWidth(width * 0.08)
  toBeHostCol.setPrefWidth(infoColWidth)
  toBeHostCol.setCellValueFactory(new PropertyValueFactory[WatchingList.WatchingListInfo, Button]("beHost"))

  val exitCol = new TableColumn[WatchingList.WatchingListInfo, Button]("踢出")
//  exitCol.setPrefWidth(width * 0.04)
  exitCol.setPrefWidth(infoColWidth)
  exitCol.setCellValueFactory(new PropertyValueFactory[WatchingList.WatchingListInfo, Button]("exit"))

  val soundCol = new TableColumn[WatchingList.WatchingListInfo, Button]("声音")
 // soundCol.setPrefWidth(width * 0.04)
  soundCol.setPrefWidth(infoColWidth)
  soundCol.setCellValueFactory(new PropertyValueFactory[WatchingList.WatchingListInfo, Button]("sound"))

  val imageCol = new TableColumn[WatchingList.WatchingListInfo, Button]("图像")
//  imageCol.setPrefWidth(width * 0.04)
  imageCol.setPrefWidth(infoColWidth)
  imageCol.setCellValueFactory(new PropertyValueFactory[WatchingList.WatchingListInfo,Button]("image"))

  watchingTable.setItems(watchingList)
  watchingTable.getColumns.addAll(headerCol, userInfoCol,toBeHostCol,exitCol,soundCol,imageCol)
//  watchingTable.setPrefHeight(height * 0.8)
  watchingTable.setPrefHeight(tableHeight)


}
