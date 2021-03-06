package videomeeting.pcClient.scene

import java.io.File

import javafx.animation.{Animation, KeyFrame, Timeline}
import javafx.beans.property.{ObjectProperty, SimpleObjectProperty, SimpleStringProperty, StringProperty}
import javafx.collections.{FXCollections, ObservableList}
import javafx.geometry.Insets
import javafx.scene.{Group, Scene}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.control._
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.effect.{DropShadow, Glow}
import javafx.scene.image.{Image, ImageView}
import javafx.scene.input.MouseEvent
import javafx.scene.layout._
import javafx.scene.text.Text
import videomeeting.pcClient.common.{AlbumInfo, Constants, Ids, Pictures}
import videomeeting.pcClient.core.RmManager
import videomeeting.protocol.ptcl.CommonInfo
import videomeeting.protocol.ptcl.CommonInfo.RecordInfo
import videomeeting.pcClient.common._
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol.Comment
import org.slf4j.LoggerFactory
import javafx.geometry.Pos
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.{DirectoryChooser, Stage}
import javafx.util.Duration
import videomeeting.capture.sdk.DeviceUtil
import videomeeting.capture.sdk.DeviceUtil.VideoOption
import videomeeting.pcClient.Boot
import videomeeting.pcClient.component.Common.getImageView
import videomeeting.pcClient.component._
import videomeeting.pcClient.core.stream.StreamPuller.{BandWidthInfo, PackageLossInfo}
import videomeeting.pcClient.scene.StartScene.{JoinListInfo, SpeakListInfo, StartSceneListener}
import videomeeting.pcClient.component.WatchingList.WatchingListInfo

import scala.collection.mutable
/**
  * Author: Administrator
  * Date: 2020/2/6/006
  * Time: 23:23
  */
object StartScene {

  case class SpeakListInfo(
                            userInfo: StringProperty,
                            agreeBtn: ObjectProperty[Button],
                            refuseBtn: ObjectProperty[Button]
                          ) {
    def getUserInfo: String = userInfo.get()

    def setUserInfo(info: String): Unit = userInfo.set(info)

    def getAgreeBtn: Button = agreeBtn.get()

    def setAgreeBtn(btn: Button): Unit = agreeBtn.set(btn)

    def getRefuseBtn: Button = refuseBtn.get()

    def setRefuseBtn(btn: Button): Unit = refuseBtn.set(btn)

  }

  case class JoinListInfo(
    userInfo: StringProperty,
    agreeBtn: ObjectProperty[Button],
    refuseBtn: ObjectProperty[Button]
  ) {
    def getUserInfo: String = userInfo.get()

    def setUserInfo(info: String): Unit = userInfo.set(info)

    def getAgreeBtn: Button = agreeBtn.get()

    def setAgreeBtn(btn: Button): Unit = agreeBtn.set(btn)

    def getRefuseBtn: Button = refuseBtn.get()

    def setRefuseBtn(btn: Button): Unit = refuseBtn.set(btn)

  }

  trait StartSceneListener {
    /*audience*/
    def joinReq(roomId: Int)

    def quitJoin(roomId: Int, userId:Int)


    def changeOption(needImage: Boolean = true, needSound: Boolean = true)


    def applySpeak(meetingId: Int)


    /*host*/
    def startLive()

    def stopLive()

    def audienceAcceptance(userId: Int, accept: Boolean, newRequest: SpeakListInfo)

    def shutJoin()

    def gotoHomeScene()

    def modifyRoomInfo(
                        name: Option[String] = None,
                        des: Option[String] = None
                      )

    def ask4Loss()

    def inviteAudience(
                        meetingId: String,
                        email: String
                      )

    def forceExit(userId: Int, userName: String)

    def beHost()

    def closeImage(userId: Int)

    def closeSound(userId: Int)

    def joinAcceptance(userId:Int,accept: Boolean, newRequest: JoinListInfo)

  }

}

class StartScene(stage: Stage) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  private val width = Constants.AppWindow.width * 0.9
  private val height = Constants.AppWindow.height * 0.75

  private val group = new Group()
  private val scene = new Scene(group, width, height)
  scene.getStylesheets.add(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
  )

  private val timeline = new Timeline()

  def startPackageLoss(): Unit = {
    log.info("start to get package loss.")
    timeline.setCycleCount(Animation.INDEFINITE)
    val keyFrame = new KeyFrame(Duration.millis(2000), { _ =>
      listener.ask4Loss()
    })
    timeline.getKeyFrames.add(keyFrame)
    timeline.play()
  }

  def stopPackageLoss(): Unit = {
    timeline.stop()
  }

  //一些参数
  var isLive = false
  var roomInfoMap = Map.empty[Int, List[String]]
  val speakObservableList: ObservableList[SpeakListInfo] = FXCollections.observableArrayList()
  val watchObservableList: ObservableList[WatchingListInfo] = FXCollections.observableArrayList()
  val joinObservableList: ObservableList[JoinListInfo] = FXCollections.observableArrayList()
  // val audObservableList: ObservableList[AudienceListInfo] = FXCollections.observableArrayList()
  var commentPrefix = "effectType0"

  var listener: StartSceneListener = _

  val fullScreenImage = new StackPane()

  //连线相关
  val connectionStateText = new Text("目前状态：无连接")
  connectionStateText.getStyleClass.add("hostScene-leftArea-text")
  val connectStateBox = new HBox()
  connectStateBox.getChildren.add(connectionStateText)
  connectStateBox.setSpacing(10)
  connectStateBox.setAlignment(Pos.CENTER_LEFT)
  connectStateBox.setSpacing(10)

  /**
    * 左侧导航栏
    *
    **/
  var roomNameField = new TextField(s"${RmManager.roomInfo.get.meetingName}")
  roomNameField.setPrefWidth(width * 0.15)
  var roomDesArea = new TextArea(s"${RmManager.roomInfo.get.roomDes}")
  roomDesArea.setPrefSize(width * 0.15, height * 0.1)
  var emailArea = new TextField("")
  emailArea.setPrefWidth(width * 0.15)

  val roomInfoIcon = new ImageView("img/roomInfo.png")
  roomInfoIcon.setFitWidth(20)
  roomInfoIcon.setFitHeight(20)

  val userIcon = new ImageView("img2/2user.png")
  userIcon.setFitWidth(20)
  userIcon.setFitHeight(20)
  val applyIcon = new ImageView("img2/2apply.png")
  applyIcon.setFitWidth(20)
  applyIcon.setFitHeight(20)
  val applyIcon1 = new ImageView("img2/2apply1.png")
  applyIcon1.setFitWidth(20)
  applyIcon1.setFitHeight(20)

  val joinIcon = new ImageView("img2/2apply.png")
  joinIcon.setFitWidth(20)
  joinIcon.setFitHeight(20)
  val joinIcon1 = new ImageView("img2/2apply1.png")
  joinIcon1.setFitWidth(20)
  joinIcon1.setFitHeight(20)


  val tb1 = new ToggleButton("参会成员", userIcon)
  tb1.getStyleClass.add("hostScene-leftArea-toggleButton")
  val tb2 = new ToggleButton("申请发言", applyIcon)
  tb2.getStyleClass.add("hostScene-leftArea-toggleButton")
  val tb3 = new ToggleButton("房间", roomInfoIcon)
  tb3.getStyleClass.add("hostScene-leftArea-toggleButton")
  val tb4 = new ToggleButton("加入",  joinIcon)
  tb4.getStyleClass.add("hostScene-leftArea-toggleButton")

  val liveToggleButton = new ToggleButton("开始会议")
  liveToggleButton.getStyleClass.add("hostScene-rightArea-liveBtn")
  liveToggleButton.setOnAction(_ => {
    if (liveToggleButton.isSelected) {
      liveToggleButton.setText("结束会议")
      //开会
      listener.startLive()
    } else {
      liveToggleButton.setText("开始会议")
      //结束会议
      listener.stopLive()
    }
  }
  )


  /**
    * canvas
    *
    **/
  val liveImage = new Canvas(Constants.DefaultPlayer.width, Constants.DefaultPlayer.height)
  val gc: GraphicsContext = liveImage.getGraphicsContext2D
  val backImg = new Image("img/background.jpg")
  val connectionBg = new Image("img/connectionBg.jpg")
  gc.drawImage(backImg, 0, 0, Constants.DefaultPlayer.width, Constants.DefaultPlayer.height)

  //这个barrage还要吗
  val barrage: Barrage = new Barrage(Constants.WindowStatus.HOST, liveImage.getWidth, liveImage.getHeight)
  val barrageCanvas: Canvas = barrage.barrageView

  val statisticsCanvas = new Canvas(Constants.DefaultPlayer.width, Constants.DefaultPlayer.height)
  val ctx: GraphicsContext = statisticsCanvas.getGraphicsContext2D

  val waitPulling = new Image("img/waitPulling.gif")

  val content = new VBox()

  def resetBack(): Unit = {
    val sWidth = gc.getCanvas.getWidth
    val sHeight = gc.getCanvas.getHeight
    gc.drawImage(connectionBg, 0, 0, sWidth, sHeight)
    gc.drawImage(waitPulling, sWidth / 2, sHeight / 4, sWidth / 2, sHeight / 2)
    gc.drawImage(waitPulling, 0, sHeight / 4, sWidth / 2, sHeight / 2)
    //gc.setFont(Font.font(emojiFont, 25))
    gc.setFill(Color.BLACK)
    gc.fillText(s"连线中", liveImage.getWidth / 2 - 40, liveImage.getHeight / 8)
  }

  def resetLoading(): Unit = {
    val sWidth = gc.getCanvas.getWidth
    val sHeight = gc.getCanvas.getHeight
    gc.drawImage(waitPulling, 0, 0, sWidth, sHeight)
  }

  /*观看列表*/
  val watchingList = new WatchingList(width * 0.05, width * 0.04, height * 0.8, Some(tb1), listener)
  val watchingState: Text = watchingList.watchingState
  val watchingTable: TableView[WatchingList.WatchingListInfo] = watchingList.watchingTable

  /*host or audience*/
  var isHost = true
  var hasReqJoin = false

  /*layout*/
  var leftArea: VBox = addLeftArea()
  var rightArea: VBox = addRightArea()

  val borderPane = new BorderPane
  borderPane.setLeft(leftArea)
  borderPane.setRight(rightArea)
  group.getChildren.add(borderPane)



  /**
    * 更新连线请求
    *
    **/
  def updateAudienceList(audienceId: Int, audienceName: String): Unit = {
    if (!tb2.isSelected) {
      tb2.setGraphic(applyIcon1)
    }
    val agreeBtn = new Button("", new ImageView("img/agreeBtn.png"))
    val refuseBtn = new Button("", new ImageView("img/refuseBtn.png"))

    agreeBtn.getStyleClass.add("hostScene-middleArea-tableBtn")
    refuseBtn.getStyleClass.add("hostScene-middleArea-tableBtn")
    val glow = new Glow()
    agreeBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
      agreeBtn.setEffect(glow)
    })
    agreeBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
      agreeBtn.setEffect(null)
    })
    refuseBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
      refuseBtn.setEffect(glow)
    })
    refuseBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
      refuseBtn.setEffect(null)
    })
    val newRequest = SpeakListInfo(
      new SimpleStringProperty(s"$audienceName($audienceId)"),
      new SimpleObjectProperty[Button](agreeBtn),
      new SimpleObjectProperty[Button](refuseBtn)
    )
    speakObservableList.add(newRequest)

    agreeBtn.setOnAction {
      _ =>
        listener.audienceAcceptance(userId = audienceId, accept = true, newRequest)
    }
    refuseBtn.setOnAction {
      _ =>
        listener.audienceAcceptance(userId = audienceId, accept = false, newRequest)
    }

  }

  def updateJoinList(audienceId: Int, audienceName: String): Unit = {
    if (!tb4.isSelected) {
      tb4.setGraphic(joinIcon1)
    }
    val agreeBtn = new Button("", new ImageView("img/agreeBtn.png"))
    val refuseBtn = new Button("", new ImageView("img/refuseBtn.png"))

    agreeBtn.getStyleClass.add("hostScene-middleArea-tableBtn")
    refuseBtn.getStyleClass.add("hostScene-middleArea-tableBtn")
    val glow = new Glow()
    agreeBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
      agreeBtn.setEffect(glow)
    })
    agreeBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
      agreeBtn.setEffect(null)
    })
    refuseBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
      refuseBtn.setEffect(glow)
    })
    refuseBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
      refuseBtn.setEffect(null)
    })
    val newRequest = JoinListInfo(
      new SimpleStringProperty(s"$audienceName($audienceId)"),
      new SimpleObjectProperty[Button](agreeBtn),
      new SimpleObjectProperty[Button](refuseBtn)
    )
    joinObservableList.add(newRequest)

    agreeBtn.setOnAction {
      _ =>
        listener.joinAcceptance(userId = audienceId, accept = true, newRequest)
    }
    refuseBtn.setOnAction {
      _ =>
        listener.joinAcceptance(userId = audienceId, accept = false, newRequest)
    }

  }

  def getScene: Scene = this.scene

  def setListener(listener: StartSceneListener): Unit = {
    this.listener = listener
  }

  def addLeftArea(): VBox = {
    val leftAreaBox = new VBox()
    println(s"check host value ${isHost}")
    if(isHost){
      println("is host ====================")
      tb3.setSelected(true)

      val group = new ToggleGroup
      tb1.setToggleGroup(group)
      tb2.setToggleGroup(group)
      tb3.setToggleGroup(group)
      tb4.setToggleGroup(group)


      val tbBox = new HBox()
      tbBox.getChildren.addAll(tb3, tb1, tb2, tb4)

      val left1Area = addLeftChild1Area()
      val left2Area = addLeftChild2Area()
      val left3Area = addLeftChild3Area()
      val left4Area = addLeftChild4Area()

      content.getChildren.add(left3Area)
      content.setPrefSize(width * 0.32, height)

      tb1.setOnAction(_ => {
        tb1.setGraphic(userIcon)
        content.getChildren.clear()
        content.getChildren.add(left1Area)
      }
      )
      tb2.setOnAction(_ => {
        tb2.setGraphic(applyIcon)
        content.getChildren.clear()
        content.getChildren.add(left2Area)
      }
      )

      tb3.setOnAction(_ => {
        tb3.setGraphic(roomInfoIcon)
        content.getChildren.clear()
        content.getChildren.add(left3Area)
      }
      )

      tb4.setOnAction(_ => {
        tb4.setGraphic(applyIcon)
        content.getChildren.clear()
        content.getChildren.add(left4Area)
      })

      leftAreaBox.getChildren.addAll(tbBox, content)

    }else{
      content.setPrefSize(width * 0.32, height)
      println("is audience ====================")
      def createRoomInfoBox: VBox = {
        //roomName
        val roomNameIcon = Common.getImageView("img/roomName.png", 30, 30)
        val roomNameText = new Text(RmManager.roomInfo.get.meetingName)
        roomNameText.setWrappingWidth(width * 0.2)
        roomNameText.getStyleClass.add("audienceScene-leftArea-roomNameText")

        val roomName = new HBox()
        roomName.getChildren.addAll(roomNameIcon, roomNameText)
        roomName.setPadding(new Insets(20, 0, 0, 0))
        roomName.setAlignment(Pos.CENTER_LEFT)
        roomName.setSpacing(8)

        val infoBox = new VBox(roomName)
        infoBox.setSpacing(20)
        infoBox.setPadding(new Insets(0, 0, 40, 0))

        infoBox
      }

      def createButtonBox: HBox = {
        val linkBtn = new Button("申请发言", new ImageView("img/link.png"))
        linkBtn.getStyleClass.add("audienceScene-leftArea-linkBtn")
        linkBtn.setOnAction{ _ =>
          listener.applySpeak(RmManager.roomInfo.get.meetingId)
        }
        Common.addButtonEffect(linkBtn)

        val exitBtn = new Button("中断发言", new ImageView("img/shutdown.png"))
        exitBtn.getStyleClass.add("audienceScene-leftArea-linkBtn")
        exitBtn.setOnAction( _ => {
          //TODO 中断发言
          //        listener.quitJoin(room.meetingId)
        })
        Common.addButtonEffect(exitBtn)

        val buttonBox = new HBox(linkBtn, exitBtn)
        buttonBox.setSpacing(15)
        buttonBox.setAlignment(Pos.CENTER)

        buttonBox

      }

      val liveToggleButton = new ToggleButton("加入会议")
      liveToggleButton.getStyleClass.add("hostScene-rightArea-liveBtn")
      liveToggleButton.setOnAction(_ => {
        if(liveToggleButton.isSelected){
          liveToggleButton.setText("退出会议")
          //加入会议
          if(!hasReqJoin) {
            listener.joinReq(RmManager.roomInfo.get.meetingId)
            hasReqJoin = true
          }
          else WarningDialog.initWarningDialog("已经发送过申请啦~")
        }else{
          liveToggleButton.setText("加入会议")
          //退出会议
          listener.quitJoin(RmManager.roomInfo.get.meetingId, RmManager.roomInfo.get.userId)
        }
      }
      )

      val change = new Button("测试")
      change.getStyleClass.add("hostScene-middleArea-tableBtn")
      change.setOnAction {
        _ =>
          println(s"check host 222 ${isHost}")
          if(isHost){
            isHost=false
            changeAllElement()
          }
          else{
            isHost=true
            changeAllElement()
          }
      }

      content.getChildren.addAll(createRoomInfoBox, createButtonBox, liveToggleButton,change)
      leftAreaBox.getChildren.addAll(content)
    }
    leftAreaBox
  }

  def addLeftChild1Area(): VBox = {
    val vBox = new VBox()
    vBox.getChildren.addAll(watchingState, watchingTable)
    vBox.setSpacing(20)
    vBox.setPrefHeight(height)
    vBox.setPadding(new Insets(20, 10, 5, 10))
    vBox.getStyleClass.add("hostScene-leftArea-wholeBox")
    //后期应该把watchingList的搬过来，因为button要在这里调listener
    vBox
  }

  def addLeftChild2Area(): VBox = {
    val vBox = new VBox()
    vBox.getChildren.addAll(connectStateBox, createCntTbArea)
    vBox.setSpacing(20)
    vBox.setPrefHeight(height)
    vBox.setPadding(new Insets(20, 10, 5, 10))
    vBox.getStyleClass.add("hostScene-leftArea-wholeBox")

    def createCntTbArea: TableView[SpeakListInfo] = {
      val AudienceTable = new TableView[SpeakListInfo]()
      AudienceTable.getStyleClass.add("table-view")

      val userInfoCol = new TableColumn[SpeakListInfo, String]("申请用户")
      userInfoCol.setPrefWidth(width * 0.15)
      userInfoCol.setCellValueFactory(new PropertyValueFactory[SpeakListInfo, String]("userInfo"))

      val agreeBtnCol = new TableColumn[SpeakListInfo, Button]("同意")
      agreeBtnCol.setCellValueFactory(new PropertyValueFactory[SpeakListInfo, Button]("agreeBtn"))
      agreeBtnCol.setPrefWidth(width * 0.08)

      val refuseBtnCol = new TableColumn[SpeakListInfo, Button]("拒绝")
      refuseBtnCol.setCellValueFactory(new PropertyValueFactory[SpeakListInfo, Button]("refuseBtn"))
      refuseBtnCol.setPrefWidth(width * 0.08)

      AudienceTable.setItems(speakObservableList)
      AudienceTable.getColumns.addAll(userInfoCol, agreeBtnCol, refuseBtnCol)
      AudienceTable.setPrefHeight(height * 0.8)
      AudienceTable
    }

    vBox

  }

  def addLeftChild3Area(): VBox = {
    val backIcon = new ImageView("img/hideBtn.png")
    val backBtn = new Button("", backIcon)
    backBtn.getStyleClass.add("roomScene-backBtn")
    backBtn.setOnAction(_ => listener.gotoHomeScene())
    Common.addButtonEffect(backBtn)

    val leftAreaBox = new VBox()
    leftAreaBox.getChildren.addAll(createRoomInfoLabel, createRoomInfoBox)
    leftAreaBox.setSpacing(10)
    leftAreaBox.setPadding(new Insets(5, 0, 0, 0))
    leftAreaBox.getStyleClass.add("hostScene-leftArea-wholeBox")
    leftAreaBox.setPrefHeight(height)


    def createRoomInfoLabel: HBox = {
      val box = new HBox()
      box.getChildren.add(backBtn)
      box.setSpacing(210)
      box.setAlignment(Pos.CENTER_LEFT)
      box.setPadding(new Insets(0, 0, 0, 5))
      box

    }

    def createRoomInfoBox: VBox = {
      val roomId = new Text(s"房间 ID：${RmManager.roomInfo.get.meetingId}")
      roomId.getStyleClass.add("hostScene-leftArea-text")

      val userId = new Text(s"房主 ID：${RmManager.roomInfo.get.userId}")
      userId.getStyleClass.add("hostScene-leftArea-text")

      val roomNameText = new Text("房间名:")
      roomNameText.getStyleClass.add("hostScene-leftArea-text")

      val confirmIcon1 = new ImageView("img/confirm.png")
      confirmIcon1.setFitHeight(15)
      confirmIcon1.setFitWidth(15)

      val roomNameBtn = new Button("确认", confirmIcon1)
      roomNameBtn.getStyleClass.add("hostScene-leftArea-confirmBtn")
      roomNameBtn.setOnAction {
        _ =>
          roomInfoMap = Map(RmManager.roomInfo.get.meetingId -> List(RmManager.roomInfo.get.meetingName, RmManager.roomInfo.get.roomDes))
          listener.modifyRoomInfo(name = Option(roomNameField.getText()))
      }
      Common.addButtonEffect(roomNameBtn)

      val roomName = new HBox()
      roomName.setAlignment(Pos.CENTER_LEFT)
      roomName.getChildren.addAll(roomNameField, roomNameBtn)
      roomName.setSpacing(5)

      val roomDesText = new Text("房间描述:")
      roomDesText.getStyleClass.add("hostScene-leftArea-text")

      val confirmIcon2 = new ImageView("img/confirm.png")
      confirmIcon2.setFitHeight(15)
      confirmIcon2.setFitWidth(15)

      val roomDesBtn = new Button("确认", confirmIcon2)
      roomDesBtn.getStyleClass.add("hostScene-leftArea-confirmBtn")
      roomDesBtn.setOnAction {
        _ =>
          roomInfoMap = Map(RmManager.roomInfo.get.meetingId -> List(RmManager.roomInfo.get.meetingName, RmManager.roomInfo.get.roomDes))
          listener.modifyRoomInfo(des = Option(roomDesArea.getText()))
      }
      Common.addButtonEffect(roomDesBtn)

      val roomDes = new HBox()
      roomDes.setAlignment(Pos.CENTER_LEFT)
      roomDes.getChildren.addAll(roomDesArea, roomDesBtn)
      roomDes.setSpacing(5)

      val emailText = new Text("邀请他人（请输入邮箱）:")
      emailText.getStyleClass.add("hostScene-leftArea-text")

      val emailBtn = new Button("确认", confirmIcon2)
      emailBtn.getStyleClass.add("hostScene-leftArea-confirmBtn")
      emailBtn.setOnAction {
        _ =>
          //邀请参会人员
          if (emailArea.getText.nonEmpty) {
            if (RmManager.roomInfo.nonEmpty){
              listener.inviteAudience(RmManager.roomInfo.get.meetingId.toString,emailArea.getText)
              emailArea.setText("")
            }
            else
              Boot.addToPlatform(
                WarningDialog.initWarningDialog("请先修改房间信息")
              )
          } else {
            Boot.addToPlatform(
              WarningDialog.initWarningDialog("输入不能为空")
            )
          }
      }
      Common.addButtonEffect(emailBtn)

      val findBox = new HBox()
      findBox.setAlignment(Pos.CENTER_LEFT)
      findBox.getChildren.addAll(emailArea, emailBtn)
      findBox.setSpacing(5)

      //test change
      val change = new Button("测试")
      change.getStyleClass.add("hostScene-middleArea-tableBtn")
      change.setOnAction {
        _ =>
          println(s"check host 222 ${isHost}")
          if(isHost){
            isHost=false
            changeAllElement()
          }
          else{
            isHost=true
            changeAllElement()
          }
      }

      val roomInfoBox = new VBox()
      roomInfoBox.getChildren.addAll(roomId, userId, roomNameText, roomName, roomDesText, roomDes, liveToggleButton, emailText, findBox,change)
      roomInfoBox.setPadding(new Insets(5, 30, 0, 30))
      roomInfoBox.setSpacing(15)
      roomInfoBox
    }


    leftAreaBox

  }

  def addLeftChild4Area(): VBox = {
    val vBox = new VBox()
    vBox.getChildren.addAll(connectStateBox, createCntTbArea)
    vBox.setSpacing(20)
    vBox.setPrefHeight(height)
    vBox.setPadding(new Insets(20, 10, 5, 10))
    vBox.getStyleClass.add("hostScene-leftArea-wholeBox")

    def createCntTbArea: TableView[JoinListInfo] = {
      val AudienceTable = new TableView[JoinListInfo]()
      AudienceTable.getStyleClass.add("table-view")

      val userInfoCol = new TableColumn[JoinListInfo, String]("申请用户")
      userInfoCol.setPrefWidth(width * 0.15)
      userInfoCol.setCellValueFactory(new PropertyValueFactory[JoinListInfo, String]("userInfo"))

      val agreeBtnCol = new TableColumn[JoinListInfo, Button]("同意")
      agreeBtnCol.setCellValueFactory(new PropertyValueFactory[JoinListInfo, Button]("agreeBtn"))
      agreeBtnCol.setPrefWidth(width * 0.08)

      val refuseBtnCol = new TableColumn[JoinListInfo, Button]("拒绝")
      refuseBtnCol.setCellValueFactory(new PropertyValueFactory[JoinListInfo, Button]("refuseBtn"))
      refuseBtnCol.setPrefWidth(width * 0.08)

      AudienceTable.setItems(joinObservableList)
      AudienceTable.getColumns.addAll(userInfoCol, agreeBtnCol, refuseBtnCol)
      AudienceTable.setPrefHeight(height * 0.8)
      AudienceTable
    }

    vBox

  }

  def addRightArea(): VBox = {
    def createLivePane = {

      val livePane = new StackPane()
      livePane.setAlignment(Pos.BOTTOM_RIGHT)
      livePane.getChildren.addAll(liveImage, statisticsCanvas, barrageCanvas)

      livePane
    }

    val vBox = new VBox(createLivePane)
    vBox.getStyleClass.add("hostScene-rightArea-wholeBox")
    vBox.setSpacing(10)
    vBox.setPadding(new Insets(15, 35, 5, 30))
    vBox.setAlignment(Pos.TOP_CENTER)

    vBox
  }


  def changeAllElement(): Unit = {
    println(s"check host 333 ${isHost}")
    group.getChildren.clear()
    borderPane.getChildren.clear()
    leftArea.getChildren.clear()
    content.getChildren.clear()
    leftArea=addLeftArea()
    borderPane.setLeft(leftArea)
    borderPane.setRight(rightArea)
    group.getChildren.add(borderPane)

  }

//  def drawPackageLoss(info: mutable.Map[String, PackageLossInfo], bandInfo: Map[String, BandWidthInfo]): Unit = {
//    ctx.save()
//    //    println(s"draw loss, ${ctx.getCanvas.getWidth}, ${ctx.getCanvas.getHeight}")
//    ctx.setFont(new Font("Comic Sans Ms", 20))
//    ctx.setFill(Color.WHITE)
//    val loss: Double = if (info.values.headOption.nonEmpty) info.values.head.lossScale2 else 0
//    val band: Double = if (bandInfo.values.headOption.nonEmpty) bandInfo.values.head.bandWidth2s else 0
//    val CPUMemInfo = NetUsage.getCPUMemInfo
//    //    info.values.headOption.foreach(
//    //      i =>
//    //        bandInfo.values.headOption.foreach(
//    //          j =>
//    //            ctx.fillText(f"丢包率：${i.lossScale2}%.3f" + " %" + f"带宽：${j.bandWidth2s}%.2f" + " %", Constants.DefaultPlayer.width / 5 * 4, 50)
//    //          )
//    //
//    //      )
//    ctx.clearRect(0, 0, ctx.getCanvas.getWidth, ctx.getCanvas.getHeight)
//    CPUMemInfo.foreach { i =>
//      val (memPer, memByte, proName) = (i.memPer, i.memByte, i.proName)
//      ctx.fillText(f"内存占比：$memPer%.2f" + " % " + f"内存：$memByte", statisticsCanvas.getWidth - 210, 15)
//    }
//    ctx.fillText(f"丢包率：$loss%.3f" + " %  " + f"带宽：$band%.2f" + " bit/s", 0, 15)
//    //    info.values.headOption.foreach(i => ctx.fillText(f"丢包率：${i.lossScale2}%.2f" + " %", Constants.DefaultPlayer.width / 5 * 4, 20))
//    ctx.restore()
//  }
}
