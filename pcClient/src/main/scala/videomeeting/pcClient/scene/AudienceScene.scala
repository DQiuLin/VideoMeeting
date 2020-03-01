package videomeeting.pcClient.scene

import com.sun.glass.ui.View.EventHandler
import javafx.animation.{Animation, KeyFrame, Timeline}
import javafx.beans.property.StringProperty
import javafx.collections.{FXCollections, ObservableList}
import javafx.geometry.{Insets, Pos}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control._
import javafx.scene.effect.{DropShadow, Glow}
import javafx.scene.image.{Image, ImageView}
import javafx.scene.input.MouseEvent
import javafx.scene.layout._
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.{Media, MediaPlayer, MediaView}
import javafx.scene.{Group, Scene}
import videomeeting.pcClient.common._
import javafx.scene.paint.Color
import javafx.scene.text.{Font, Text}
import javafx.util
import javafx.util.Duration
import videomeeting.pcClient.common.Constants.AudienceStatus
import videomeeting.pcClient.component._
import videomeeting.pcClient.core.RmManager
import videomeeting.pcClient.core.stream.StreamPuller.{BandWidthInfo, PackageLossInfo}
import videomeeting.protocol.ptcl.CommonInfo.{MeetingInfo, RecordInfo, UserDes}
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol.Comment
import org.slf4j.LoggerFactory

import scala.collection.mutable


/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 12:12
  */


object AudienceScene {

//  case class AudienceList(
//                     audienceInfo: StringProperty
//                       ) {
//    def getUserInfo: String = audienceInfo.get()
//
//    def setUserInfo(info: String): Unit = audienceInfo.set(info)
//  }

  trait AudienceSceneListener {

    def joinReq(roomId: Int)

    def quitJoin(roomId: Int, userId:Int)

    def gotoHomeScene()

    def changeOption(needImage: Boolean = true, needSound: Boolean = true)

    def ask4Loss()

    def applySpeak(meetingId: Int)

  }


}

class AudienceScene(room: MeetingInfo, isRecord: Boolean = false, recordUrl: String = "") {
  import AudienceScene._

  private val width = Constants.AppWindow.width * 0.9
  private val height = Constants.AppWindow.height * 0.75

  private val group = new Group()

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

  override def finalize(): Unit = {
    //    println("release")
    super.finalize()
  }

  def stopPackageLoss(): Unit = {
    timeline.stop()
  }

  private[this] val log = LoggerFactory.getLogger(this.getClass)
  var watchUrl: Option[String] = None
  var liveId: Option[String] = None
  //  var recordUrl: Option[String] = None
  var commentPrefix = "effectType0"

  var hasReqJoin = false

  var audienceStatus: Int = AudienceStatus.LIVE

  var watchingLs = List.empty[UserDes]
  val fullScreenImage = new StackPane()
  var leftArea: VBox = _
  var rightArea: VBox = _
  val gift4Img = new Image("img/rose.png")
  val gift5Img = new Image("img/gift5.png")
  val gift6Img = new Image("img/gift6.png")
  val waitPulling = new Image("img/waitPulling.gif")


  /*屏幕下方功能条*/
  val liveBar: LiveBar = new LiveBar(Constants.WindowStatus.AUDIENCE_REC, width = Constants.DefaultPlayer.width, height = Constants.DefaultPlayer.height * 0.1)

  val imageToggleBtn: ToggleButton = liveBar.imageToggleButton
  val soundToggleBtn: ToggleButton = liveBar.soundToggleButton

  imageToggleBtn.setOnAction {
    _ =>
      listener.changeOption(needImage = imageToggleBtn.isSelected, needSound = soundToggleBtn.isSelected)
  }

  soundToggleBtn.setOnAction {
    _ =>
      listener.changeOption(needImage = imageToggleBtn.isSelected, needSound = soundToggleBtn.isSelected)
  }

//  liveBar.startTimer()


  val liveBarBox: VBox = liveBar.barVBox

  /*liveImage view*/

  val imgView = new Canvas(Constants.DefaultPlayer.width, Constants.DefaultPlayer.height)
  val gc: GraphicsContext = imgView.getGraphicsContext2D

  val statisticsCanvas = new Canvas(Constants.DefaultPlayer.width, Constants.DefaultPlayer.height)
  val ctx: GraphicsContext = statisticsCanvas.getGraphicsContext2D

  val backImg = new Image("img/loading.jpg")
  gc.drawImage(backImg, 0, 0, gc.getCanvas.getWidth, gc.getCanvas.getHeight)
  val connectionBg = new Image("img/connectionBg.jpg")

  /*host or audience*/
  var isHost = false

//  def resetBack(): Unit = {
//    gc.clearRect(0,0,gc.getCanvas.getWidth, gc.getCanvas.getHeight)
//    gc.drawImage(connectionBg, 0, 0, gc.getCanvas.getWidth, gc.getCanvas.getHeight)
//    val sWidth = gc.getCanvas.getWidth
//    val sHeight = gc.getCanvas.getHeight
//    gc.drawImage(waitPulling, sWidth / 2, sHeight / 4, sWidth / 2, sHeight / 2)
//    gc.drawImage(waitPulling, 0, sHeight / 4, sWidth / 2, sHeight / 2)
//    gc.setFont(Font.font(emojiFont, 25))
//    gc.setFill(Color.BLACK)
//    gc.fillText(s"连线中", imgView.getWidth / 2 - 40, imgView.getHeight / 8)
//  }
//
//
//  def resetBack2():Unit={
//    gc.clearRect(0,0,gc.getCanvas.getWidth, gc.getCanvas.getHeight)
//    gc.drawImage(connectionBg, 0, 0, gc.getCanvas.getWidth, gc.getCanvas.getHeight)
//    gc.setFont(Font.font(emojiFont, 25))
//    gc.setFill(Color.BLACK)
//    gc.fillText(s"连线中", imgView.getWidth / 2 - 40, imgView.getHeight / 8)
//  }

  def loadingBack(): Unit = {
    gc.drawImage(waitPulling, 0, 0, gc.getCanvas.getWidth, gc.getCanvas.getHeight)
  }


  def autoReset(): Unit = {

    audienceStatus match {
      case AudienceStatus.LIVE =>
        loadingBack()
      case AudienceStatus.CONNECT =>
        //resetBack()
    }

  }

  def autoReset2():Unit={
    //resetBack2()
  }

  /*观看列表*/

  private val scene = new Scene(group, width, height)
  scene.getStylesheets.add(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
  )

  def getScene: Scene = this.scene

  def getRoomInfo: MeetingInfo = this.room

  def getIsRecord: Boolean = this.isRecord

  var listener: AudienceSceneListener = _

  def setListener(listener: AudienceSceneListener): Unit = {
    this.listener = listener
  }

  val viewIcon: ImageView = Common.getImageView("img/view.png", 30, 30)
  val viewLabel = new Label(room.attendanceNum.getOrElse(0).toString, viewIcon)
  def createIDcard: HBox = {

//    val header = Pictures.getPic(room.headImgUrl)
//    header.setFitHeight(40)
//    header.setFitWidth(40)

    val userName = new Label(s"${room.username}")
    userName.getStyleClass.add("hostScene-rightArea-label")

    val userId = new Label(s"${room.userId}")
    userName.getStyleClass.add("hostScene-rightArea-label")

    val userInfo = new VBox()
    userInfo.getChildren.addAll(userName, userId)
    userInfo.setSpacing(5)
    userInfo.setPadding(new Insets(0, 5, 0, 5))
    userInfo.setAlignment(Pos.CENTER_LEFT)


//    val viewIcon = Common.getImageView("img/view.png", 30, 30)
//    val viewLabel = new Label(room.attendanceNum.getOrElse(0).toString, viewIcon)
    viewLabel.setPadding(new Insets(0,0,0,6))

    val IDcard = new HBox(userInfo, viewLabel)

    IDcard.setAlignment(Pos.CENTER_LEFT)
    IDcard.setPadding(new Insets(6, 5, 6, 3))
    IDcard.getStyleClass.add("hostScene-rightArea-IDcard")

    IDcard
  }

  def updateViewLabel(ls: List[UserDes]): Unit = {
    viewLabel.setText(s"${ls.length}")
  }

  val borderPane: BorderPane = addBorderPane()
  group.getChildren.addAll(borderPane)


  def addBorderPane(): BorderPane = {
    leftArea = addLeftArea()
    rightArea = addRightArea()
    val borderPane = new BorderPane
    borderPane.setLeft(leftArea)
    borderPane.setRight(rightArea)
    borderPane
  }

  def addLeftArea(): VBox = {

    def createRoomInfoBox: VBox = {
      //roomName
      val roomNameIcon = Common.getImageView("img/roomName.png", 30, 30)
      val roomNameText = new Text(room.meetingName)
      roomNameText.setWrappingWidth(width * 0.2)
      roomNameText.getStyleClass.add("audienceScene-leftArea-roomNameText")

      val roomName = new HBox()
      roomName.getChildren.addAll(roomNameIcon, roomNameText)
      roomName.setPadding(new Insets(20, 0, 0, 0))
      roomName.setAlignment(Pos.CENTER_LEFT)
      roomName.setSpacing(8)

      //roomDes
//      val roomDesIcon = Common.getImageView("img/roomDes.png", 30, 30)
//      val roomDesText = if(room.roomDes.nonEmpty){
//        new Text(room.roomDes)
//      } else {
//        new Text("TA还没有描述哦~")
//      }
//      roomDesText.setWrappingWidth(width * 0.2)
//      roomDesText.getStyleClass.add("audienceScene-leftArea-roomDesText")
//
//      val roomDes = new HBox()
//      roomDes.getChildren.addAll(roomDesIcon, roomDesText)
//      roomDes.setAlignment(Pos.CENTER_LEFT)
//      roomDes.setSpacing(8)

      val infoBox = new VBox(roomName)
      infoBox.setSpacing(20)
      infoBox.setPadding(new Insets(0, 0, 40, 0))

      infoBox
    }

    def createButtonBox: HBox = {
      val linkBtn = new Button("申请发言", new ImageView("img/link.png"))
      linkBtn.getStyleClass.add("audienceScene-leftArea-linkBtn")
      linkBtn.setOnAction{ _ =>
        listener.applySpeak(room.meetingId)
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
          listener.joinReq(room.meetingId)
          hasReqJoin = true
        }
        else WarningDialog.initWarningDialog("已经发送过申请啦~")
      }else{
        liveToggleButton.setText("加入会议")
        //退出会议
        listener.quitJoin(room.meetingId, room.userId)
        hasReqJoin = false
      }
    }
    )

    val leftAreaBox = new VBox(createRoomInfoBox, createButtonBox, liveToggleButton)


    leftAreaBox.setSpacing(5)
    leftAreaBox.setPadding(new Insets(25, 10, 10, 10))

    leftAreaBox.setPrefHeight(height)
    leftAreaBox.getStyleClass.add("hostScene-leftArea-wholeBox")
    leftAreaBox

  }

  def addRightArea(): VBox = {
    def createTopBox() = {
      val backBtn = new Button("", new ImageView("img/audienceBack.png"))
      backBtn.getStyleClass.add("audienceScene-leftArea-backBtn")
      backBtn.setOnAction(_ => listener.gotoHomeScene())
      Common.addButtonEffect(backBtn)

      val IDcard: HBox = createIDcard

      val leftBox = new HBox(IDcard)
      leftBox.setPrefWidth(imgView.getWidth * 0.6)
      leftBox.setAlignment(Pos.CENTER_LEFT)

      val rightBox = new HBox(backBtn)
      rightBox.setPrefWidth(imgView.getWidth * 0.4)
      rightBox.setAlignment(Pos.CENTER_RIGHT)

      val topBox = new HBox(leftBox, rightBox)
      topBox
    }

    val livePane = new StackPane()

    livePane.getChildren.addAll(imgView, statisticsCanvas)

    livePane.setAlignment(Pos.BOTTOM_RIGHT)

    livePane.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
      livePane.setAlignment(Pos.BOTTOM_RIGHT)
      livePane.getChildren.add(liveBarBox)

      //      if (isRecord) {
      //        livePane.setAlignment(Pos.BOTTOM_RIGHT)
      //        livePane.getChildren.add(playerPane.mediaTopBar)
      //      }
      //        playerPane.addTopAndBottom()
    })

    livePane.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
      livePane.setAlignment(Pos.BASELINE_RIGHT)
      livePane.getChildren.remove(liveBarBox)
      //      if (isRecord) {
      //        livePane.setAlignment(Pos.BOTTOM_RIGHT)
      //        livePane.getChildren.remove(playerPane.mediaTopBar)
      //      }
      //        playerPane.removeTopAndBottom()
    })

    //    val gift = new GiftBar(group)


    val hBox = new HBox()

    hBox.getChildren.add(livePane)

    val vBox = new VBox(createTopBox(), hBox)

    vBox.getStyleClass.add("hostScene-rightArea-wholeBox")
    if(!isRecord){
      vBox.setSpacing(10)
      vBox.setPadding(new Insets(15, 35, 0, 35))
    } else{
      vBox.setSpacing(30)
      vBox.setPadding(new Insets(50, 44, 0, 44))
    }

    vBox.setAlignment(Pos.TOP_CENTER)
    vBox


  }


  def addAllElement(): Unit = {
    group.getChildren.clear()
    fullScreenImage.getChildren.clear()
    rightArea = addRightArea()
    borderPane.setRight(rightArea)
    group.getChildren.addAll(borderPane)

  }

  def removeAllElement(): Unit = {
    group.getChildren.clear()
    fullScreenImage.getChildren.addAll(imgView,statisticsCanvas)
    fullScreenImage.setLayoutX(0)
    fullScreenImage.setLayoutY(0)
//    if (isRecord) fullScreenImage.getChildren.add(liveBarBox)
    group.getChildren.add(fullScreenImage)
  }

//  def drawPackageLoss(info: mutable.Map[String, PackageLossInfo], bandInfo: Map[String, BandWidthInfo]): Unit = {
//    ctx.save()
//    //    println(s"draw loss, ${ctx.getCanvas.getWidth}, ${ctx.getCanvas.getHeight}")
//    ctx.setFont(new Font("Comic Sans Ms", 20))
//    ctx.setFill(Color.WHITE)
//    val loss: Double = if (info.values.headOption.nonEmpty) info.values.head.lossScale2 else 0
//    val band: Double = if (bandInfo.values.headOption.nonEmpty) bandInfo.values.head.bandWidth2s else 0
//    val  CPUMemInfo= NetUsage.getCPUMemInfo
//    ctx.clearRect(0, 0, ctx.getCanvas.getWidth, ctx.getCanvas.getHeight)
//    CPUMemInfo.foreach { i =>
//      val (memPer, memByte, proName) = (i.memPer, i.memByte, i.proName)
//      ctx.fillText(f"内存占比：$memPer%.2f" + " % " + f"内存：$memByte" , statisticsCanvas.getWidth - 210, 15)
//    }
//    ctx.fillText(f"丢包率：$loss%.3f" + " %  " + f"带宽：$band%.2f" + " bit/s", 0, 15)
//    //    info.values.headOption.foreach(i => ctx.fillText(f"丢包率：${i.lossScale2}%.2f" + " %", Constants.DefaultPlayer.width / 5 * 4, 20))
//    ctx.restore()
//  }




}
