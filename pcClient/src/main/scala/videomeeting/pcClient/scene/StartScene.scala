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
import videomeeting.pcClient.scene.StartScene.{StartSceneListener, WatchListInfo,SpeakListInfo}
import videomeeting.pcClient.utils.{NetUsage, TimeUtil}

import scala.collection.mutable
/**
  * Author: Administrator
  * Date: 2020/2/6/006
  * Time: 23:23
  */
object StartScene {

  case class WatchListInfo(
    userInfo: StringProperty,
    toBeHostBtn: ObjectProperty[Button],
    soundCtrBtn: ObjectProperty[Button],
    imageCtrBtn: ObjectProperty[Button],
  ) {
    def getUserInfo: String = userInfo.get()

    def setUserInfo(info: String): Unit = userInfo.set(info)

    def getHostBtn: Button = toBeHostBtn.get()

    def setHostBtn(btn: Button): Unit = toBeHostBtn.set(btn)

    def getSoundBtn: Button = soundCtrBtn.get()

    def setSoundBtn(btn: Button): Unit = soundCtrBtn.set(btn)

    def getImageBtn: Button = imageCtrBtn.get()

    def setImageBtn(btn: Button): Unit = imageCtrBtn.set(btn)
  }

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

  trait StartSceneListener {

    def startLive()

    def stopLive()

    def audienceAcceptance(userId: Long, accept: Boolean, newRequest: SpeakListInfo)

    def shutJoin()

    def gotoHomeScene()

    def ask4Loss()

  }

}

class StartScene(stage: Stage) {
  import HostScene._

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
  var roomInfoMap = Map.empty[Long, List[String]]
  val speakObservableList: ObservableList[SpeakListInfo] = FXCollections.observableArrayList()
  val watchObservableList: ObservableList[WatchListInfo] = FXCollections.observableArrayList()
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
    *左侧导航栏
    *
    **/

  val userIcon = new ImageView("img2/2user.png")
  userIcon.setFitWidth(20)
  userIcon.setFitHeight(20)
  val applyIcon = new ImageView("img2/2apply.png")
  applyIcon.setFitWidth(20)
  applyIcon.setFitHeight(20)
//这个要找一张那种来电的图片来
  val applyIcon1 = new ImageView("img2/2apply1.png")
  applyIcon.setFitWidth(20)
  applyIcon.setFitHeight(20)

  val tb1 = new ToggleButton("当前参会成员", userIcon)
  tb1.getStyleClass.add("hostScene-leftArea-toggleButton")
  val tb2 = new ToggleButton("申请发言人", applyIcon)
  tb2.getStyleClass.add("hostScene-leftArea-toggleButton")

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
  val watchingList = new WatchingList(width * 0.05, width * 0.04, height * 0.8, Some(tb1))
  val watchingState: Text = watchingList.watchingState
  val watchingTable: TableView[WatchingList.WatchingListInfo] = watchingList.watchingTable

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
  def updateAudienceList(audienceId: Long, audienceName: String): Unit = {
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

  /*
   *更新参会人员
   *
   */
  def updateWatcherList(audienceId: Long, audienceName: String): Unit = {

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

  def getScene: Scene = this.scene

  def setListener(listener: StartSceneListener): Unit = {
    this.listener = listener
  }

  def addLeftArea(): VBox = {
    tb1.setSelected(true)

    val group = new ToggleGroup
    tb1.setToggleGroup(group)
    tb2.setToggleGroup(group)

    val tbBox = new HBox()
    tbBox.getChildren.addAll(tb1, tb2)

    val left1Area = addLeftChild1Area()
    val left2Area = addLeftChild2Area()

    content.getChildren.add(left1Area)
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

    val leftArea = new VBox()
    leftArea.getChildren.addAll(tbBox, content)

    leftArea
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


  def addAllElement(): Unit = {
    group.getChildren.clear()
    fullScreenImage.getChildren.clear()
    rightArea = addRightArea()
    borderPane.setRight(rightArea)
    group.getChildren.add(borderPane)
  }

  def removeAllElement(): Unit = {
    group.getChildren.clear()
    fullScreenImage.getChildren.addAll(liveImage, statisticsCanvas, barrageCanvas)
    fullScreenImage.setLayoutX(0)
    fullScreenImage.setLayoutY(0)
    group.getChildren.add(fullScreenImage)
  }

  def drawPackageLoss(info: mutable.Map[String, PackageLossInfo], bandInfo: Map[String, BandWidthInfo]): Unit = {
    ctx.save()
    //    println(s"draw loss, ${ctx.getCanvas.getWidth}, ${ctx.getCanvas.getHeight}")
    ctx.setFont(new Font("Comic Sans Ms", 20))
    ctx.setFill(Color.WHITE)
    val loss: Double = if (info.values.headOption.nonEmpty) info.values.head.lossScale2 else 0
    val band: Double = if (bandInfo.values.headOption.nonEmpty) bandInfo.values.head.bandWidth2s else 0
    val  CPUMemInfo= NetUsage.getCPUMemInfo
    //    info.values.headOption.foreach(
    //      i =>
    //        bandInfo.values.headOption.foreach(
    //          j =>
    //            ctx.fillText(f"丢包率：${i.lossScale2}%.3f" + " %" + f"带宽：${j.bandWidth2s}%.2f" + " %", Constants.DefaultPlayer.width / 5 * 4, 50)
    //          )
    //
    //      )
    ctx.clearRect(0, 0, ctx.getCanvas.getWidth, ctx.getCanvas.getHeight)
    CPUMemInfo.foreach { i =>
      val (memPer, memByte, proName) = (i.memPer, i.memByte, i.proName)
      ctx.fillText(f"内存占比：$memPer%.2f" + " % " + f"内存：$memByte" , statisticsCanvas.getWidth - 210, 15)
    }
    ctx.fillText(f"丢包率：$loss%.3f" + " %  " + f"带宽：$band%.2f" + " bit/s", 0, 15)
    //    info.values.headOption.foreach(i => ctx.fillText(f"丢包率：${i.lossScale2}%.2f" + " %", Constants.DefaultPlayer.width / 5 * 4, 20))
    ctx.restore()
  }
}