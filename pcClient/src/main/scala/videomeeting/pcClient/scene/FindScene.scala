package videomeeting.pcClient.scene

import javafx.beans.property.{ObjectProperty, SimpleObjectProperty, SimpleStringProperty, StringProperty}
import javafx.collections.{FXCollections, ObservableList}
import javafx.geometry.{Insets, Pos, Side}
import javafx.scene.control._
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.effect.{DropShadow, Glow}
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout._
import javafx.scene.text.{Font, Text}
import javafx.scene.{Group, Scene}
import videomeeting.pcClient.Boot
import videomeeting.pcClient.Boot.executor
import videomeeting.pcClient.common._
import videomeeting.protocol.ptcl.CommonInfo.MeetingInfo
import org.slf4j.LoggerFactory
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import videomeeting.pcClient.component.{Common, WarningDialog}
import videomeeting.pcClient.component.Common._
import videomeeting.pcClient.utils.{RMClient, TimeUtil}

import scala.collection.mutable
import scala.concurrent.Future
/**
  * Author: Administrator
  * Date: 2020/2/6/006
  * Time: 23:23
  */
object FindScene {
  case class RoomListInfo(
    roomId: StringProperty,
    roomName: StringProperty,
    roomDes: StringProperty,
    userId: StringProperty,
    enterBtn: ObjectProperty[Button]
  ) {
    def getRoomId: String = roomId.get()

    def setRoomId(id: String): Unit = roomId.set(id)

    def getRoomName: String = roomName.get()

    def setRoomName(id: String): Unit = roomName.set(id)

    def getRoomDes: String = roomDes.get()

    def setRoomDes(id: String): Unit = roomDes.set(id)

    def getUserId: String = userId.get()

    def setUserId(id: String): Unit = userId.set(id)

    def getEnterBtn: Button = enterBtn.get()

    def setEnterBtn(btn: Button): Unit = enterBtn.set(btn)

  }

  trait FindSceneListener {

    def enter(roomId: Long, timestamp: Long = 0L)

    //    def create()

    def refresh()

    def gotoHomeScene()
  }
}

class FindScene {
  import FindScene._
  private val log = LoggerFactory.getLogger(this.getClass)

  private val width = Constants.AppWindow.width * 0.9
  private val height = Constants.AppWindow.height * 0.75

  val group = new Group()
  val backIcon = new ImageView("img2/3background.jpg")
  backIcon.setFitHeight(height)
  backIcon.setFitWidth(width)
  private val scene = new Scene(group, width, height)

  scene.getStylesheets.add(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
  )

  def getScene: Scene = this.scene

  var roomList: List[MeetingInfo] = Nil

  val waitingGif = new ImageView("img/waiting.gif")
  waitingGif.setFitHeight(50)
  waitingGif.setFitWidth(50)
  waitingGif.setLayoutX(width / 2 - 25)
  waitingGif.setLayoutY(height / 2 - 25)

  /*buttons*/
  private val refreshBtn = new Button("", new ImageView("img/refreshBtn.png"))
  refreshBtn.getStyleClass.add("roomScene-refreshBtn")

  val backBtn = new Button("", new ImageView("img/backBtn1.png"))
  backBtn.getStyleClass.add("roomScene-backBtn")

  val shadow = new DropShadow()

  refreshBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
    refreshBtn.setEffect(shadow)
  })
  refreshBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
    refreshBtn.setEffect(null)
  })

  backBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
    backBtn.setEffect(shadow)
  })
  backBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
    backBtn.setEffect(null)
  })

  refreshBtn.setOnAction(_ => listener.refresh())
  backBtn.setOnAction(_ => listener.gotoHomeScene())

  /*layout*/
  val backBtnBox = new HBox()
  backBtnBox.getChildren.add(backBtn)
  backBtnBox.setPadding(new Insets(15, 0, 0, 20))
  backBtnBox.setAlignment(Pos.TOP_LEFT)

  val refreshBtnBox = new HBox()
  refreshBtnBox.getChildren.add(refreshBtn)
  refreshBtnBox.setPadding(new Insets(15, 20, 0, 0))
  refreshBtnBox.setAlignment(Pos.TOP_RIGHT)

  //  //这个以后加个 现有会议室 上去
  //  val roomTableLabelBox = new HBox(25)
  //  roomTableLabelBox.setPadding(new Insets(0, 0, 15, 0))
  //  roomTableLabelBox.setAlignment(Pos.BOTTOM_CENTER)

  /*liveBox*/
  val liveInfo = new Text("")
  liveInfo.setFont(Font.font(25))
  val liveBox = new HBox(20, liveInfo)
  liveBox.setAlignment(Pos.CENTER_LEFT)
  liveBox.setPadding(new Insets(0, 0, 15, 0))

  val topBox = new HBox()
  topBox.getChildren.addAll(backBtnBox, liveBox, refreshBtnBox)
  //topBox.getStyleClass.add("hostScene-leftArea-wholeBox")
  topBox.setPrefSize(width, height * 0.15)
  topBox.setSpacing(width * 0.33)
  topBox.setAlignment(Pos.CENTER)



  def createOnePage(pageIndex: Int, itemsPerPage: Int, roomList: List[MeetingInfo]): VBox = {
    val vBox = new VBox()
    vBox.setPadding(new Insets(10, 110, 20, 110))
    vBox.setSpacing(30)
    val hBox1 = new HBox()
    hBox1.setSpacing(25)
    val hBox2 = new HBox()
    hBox2.setSpacing(25)
    val totalLen = roomList.length
    val start = pageIndex * itemsPerPage + 1
    val end = (pageIndex + 1) * itemsPerPage
    for (i <- start to (start + 2)) {
      if (i <= totalLen) {
        val roomBox = new VBox(3)
        // stackPane: roomPic & picBar(userName & viewNum & likeNum)
        val roomPic = Pictures.getPic(roomList(i - 1).coverImgUrl, isHeader = false)
        roomPic.setFitHeight(Constants.DefaultPlayer.height / 2.5)
        roomPic.setFitWidth(Constants.DefaultPlayer.width / 2.5)
        roomPic.addEventHandler(MouseEvent.MOUSE_CLICKED, (_: MouseEvent) => {
          listener.enter(roomList(i - 1).meetingId)
        })


        val userName = new Label(s"${roomList(i - 1).username}")
        userName.setPrefWidth(120)
        userName.getStyleClass.add("roomScene-userName")

        val audienceNumIcon = Common.getImageView("img/roomScene-view.png", 25, 25)
        val audienceNum = new Label(s"${roomList(i - 1).attendanceNum}", audienceNumIcon)
        audienceNum.setPrefWidth(80)
        audienceNum.getStyleClass.add("roomScene-userName")

        val picBar = new HBox(userName, audienceNum)
        picBar.setMaxSize(roomPic.getFitWidth, roomPic.getFitHeight * 0.15)
        picBar.setPadding(new Insets(3,0,3,0))
        picBar.setAlignment(Pos.CENTER_LEFT)
        picBar.getStyleClass.add("roomScene-picBar")

        val picPane = new StackPane()
        picPane.setAlignment(Pos.BOTTOM_CENTER)
        picPane.getChildren.addAll(roomPic)

        // roomName
        val roomName = new Label(s"${roomList(i - 1).meetingName}")
        roomName.setPrefWidth(200)
        roomName.getStyleClass.add("roomScene-roomName")

        // timeBox(startTime & duration)
        val timeIcon = getImageView("img/date.png", 20, 20)
//        val liveTime = if (roomList(i - 1).timestamp != 0L) new Label(TimeUtil.timeStamp2DetailDate(roomList(i - 1).timestamp), timeIcon) else new Label("")
//        liveTime.setPrefWidth(160)
//        liveTime.getStyleClass.add("roomScene-time")

        val durationIcon = getImageView("img/clock.png", 20, 20)
//        val duration = if(roomList(i - 1).timestamp != 0L) new Label(s"${roomList(i - 1).duration}", durationIcon) else new Label("")
//        duration.setPrefWidth(100)
//        duration.getStyleClass.add("roomScene-time")
//
//        val timeBox = new HBox(liveTime, duration)
//        timeBox.setAlignment(Pos.CENTER_LEFT)

        //roomBox
        roomBox.getChildren.addAll(picPane, roomName)
        roomBox.setStyle("-fx-cursor: hand;")
        val shadow = new DropShadow(10, Color.GRAY)
        roomBox.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
          picPane.getChildren.add(picBar)
          roomPic.setEffect(shadow)
        })
        roomBox.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
          picPane.getChildren.remove(picBar)
          roomPic.setEffect(null)
        })
        hBox1.getChildren.add(roomBox)
      }
    }
    for (i <- (start + 3) to end) {

      if (i <= totalLen) {
        //        println(s"i${i}, sum: ${totalLen}")
        val roomBox = new VBox(3)
        // stackPane: roomPic & picBar(userName & viewNum & likeNum)
        val roomPic = Pictures.getPic(roomList(i - 1).coverImgUrl, isHeader = false)
        roomPic.setFitHeight(Constants.DefaultPlayer.height / 2.5)
        roomPic.setFitWidth(Constants.DefaultPlayer.width / 2.5)
        roomPic.addEventHandler(MouseEvent.MOUSE_CLICKED, (_: MouseEvent) => {
          listener.enter(roomList(i - 1).meetingId)
        })

        val userName = new Label(s"${roomList(i - 1).meetingName}")
        userName.setPrefWidth(120)
        userName.getStyleClass.add("roomScene-userName")

        val audienceNumIcon = Common.getImageView("img/roomScene-view.png", 25, 25)
        val audienceNum = new Label(s"${roomList(i - 1).attendanceNum}", audienceNumIcon)
        audienceNum.setPrefWidth(80)
        audienceNum.getStyleClass.add("roomScene-userName")

//        val likeNumIcon = Common.getImageView("img/roomScene-like.png", 20, 20)
//        val likeNum = new Label(s"${roomList(i - 1).like}", likeNumIcon)
//        likeNum.setPrefWidth(80)
//        likeNum.getStyleClass.add("roomScene-userName")

        val picBar = new HBox(userName, audienceNum)
        picBar.setMaxSize(roomPic.getFitWidth, roomPic.getFitHeight * 0.2)
        picBar.setPadding(new Insets(3,0,3,0))
        picBar.setAlignment(Pos.CENTER_LEFT)
        picBar.getStyleClass.add("roomScene-picBar")

        val picPane = new StackPane()
        picPane.setAlignment(Pos.BOTTOM_CENTER)
        picPane.getChildren.addAll(roomPic)

        // roomName
        val roomName = new Label(s"${roomList(i - 1).meetingName}")
        roomName.setPrefWidth(200)
        roomName.getStyleClass.add("roomScene-roomName")

        // timeBox(startTime & duration)
//        val timeIcon = getImageView("img/date.png", 20, 20)
//        val liveTime = if (roomList(i - 1).timestamp != 0L) new Label(TimeUtil.timeStamp2DetailDate(roomList(i - 1).timestamp), timeIcon) else new Label("")
//        liveTime.setPrefWidth(160)
//        liveTime.getStyleClass.add("roomScene-time")
//
//        val durationIcon = getImageView("img/clock.png", 20, 20)
//        val duration = new Label(s"${roomList(i - 1).duration}", durationIcon)
//        duration.setPrefWidth(100)
//        duration.getStyleClass.add("roomScene-time")
//
//        val timeBox = new HBox(liveTime, duration)
//        timeBox.setAlignment(Pos.CENTER_LEFT)

        //roomBox
        roomBox.getChildren.addAll(picPane, roomName)
        roomBox.setStyle("-fx-cursor: hand;")
        val shadow = new DropShadow(10, Color.GRAY)
        roomBox.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
          picPane.getChildren.add(picBar)
          roomPic.setEffect(shadow)
        })
        roomBox.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
          picPane.getChildren.remove(picBar)
          roomPic.setEffect(null)
        })
        hBox2.getChildren.add(roomBox)
      }
    }
    //两行是Hbox,把两行放在VBOX里
    vBox.getChildren.addAll(hBox1, hBox2)
    vBox
  }

  val loading = new Label("房间加载中……")
  loading.setFont(Font.font("Verdana", 30))
  loading.setPadding(new Insets(200, 0, 0, 0))
  val borderPane = new BorderPane()
  borderPane.setTop(topBox)
  borderPane.setCenter(loading)
  group.getChildren.addAll(backIcon,borderPane)

  /**
    * update roomList  func
    *
    **/
  def updateRoomList(roomList: List[MeetingInfo] = Nil): Unit = {
    //    log.debug(s"update room list: r$roomList")
    if (roomList.isEmpty) {
      val label = new Label("暂无房间")
      label.setFont(Font.font("Verdana", 30))
      label.setPadding(new Insets(200, 0, 0, 0))
      borderPane.setCenter(label)
    } else {
      val itemsPerPage = 6
      val pageNum = if (roomList.length % itemsPerPage.toInt == 0) {
        roomList.length / itemsPerPage.toInt
      }
      else {
        roomList.length / itemsPerPage.toInt + 1
      }
      val pagination = new Pagination(pageNum, 0)
      pagination.setPageFactory((pageIndex: Integer) => {
        if (pageIndex >= pageNum)
          null
        else {
          createOnePage(pageIndex, itemsPerPage, roomList)
        }
      })
      val center = new VBox(10)
      liveInfo.setText(s"当前共有${roomList.length}个会议室")
      center.getChildren.addAll(pagination)
      borderPane.setCenter(center)
    }

  }


  var listener: FindSceneListener = _

  def setListener(listener: FindSceneListener): Unit = {
    this.listener = listener
  }
}
