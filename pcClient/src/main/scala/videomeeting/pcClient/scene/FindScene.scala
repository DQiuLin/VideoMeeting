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
import videomeeting.protocol.ptcl.CommonInfo.{MeetingInfo, RecordInfo, UserInfo}
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

    def enter(roomId: Int, timestamp: Long = 0L)

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

  /*live*/
  var roomList: List[MeetingInfo] = Nil

  /*buttons*/

  val backBtn = new Button("", new ImageView("img/backBtn1.png"))
  backBtn.getStyleClass.add("roomScene-backBtn")

  val shadow = new DropShadow()

  backBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
    backBtn.setEffect(shadow)
  })
  backBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
    backBtn.setEffect(null)
  })

  backBtn.setOnAction(_ => listener.gotoHomeScene())

  /*layout*/
  val backBtnBox = new HBox()
  backBtnBox.getChildren.add(backBtn)
  backBtnBox.setPadding(new Insets(15, 0, 0, 20))
  backBtnBox.setAlignment(Pos.TOP_LEFT)


  val topBox = new HBox()
  topBox.getChildren.add(backBtnBox)
  //topBox.getStyleClass.add("hostScene-leftArea-wholeBox")
  topBox.setPrefSize(width, height * 0.15)
  topBox.setSpacing(width * 0.33)
  topBox.setAlignment(Pos.CENTER)

  /*find space*/
  val roomIcon = new ImageView("img/userName.png")
  roomIcon.setFitHeight(30)
  roomIcon.setFitWidth(30)
  val roomLabel = new Label("会议号:")
  roomLabel.setFont(Font.font(18))
  val roomIdField = new TextField("")
  val goBtn = new Button("前往")
  //这里要加一个验证，是否为纯粹的数字
  goBtn.setOnAction(_ => listener.enter(roomIdField.getText.toInt))

  val findRoomGrid = new GridPane //格子布局
  findRoomGrid.setHgap(20)
  findRoomGrid.setVgap(30)
  findRoomGrid.add(roomIcon, 0, 0)
  findRoomGrid.add(roomLabel, 1, 0)
  findRoomGrid.add(roomIdField, 2, 0)
  findRoomGrid.add(goBtn, 1, 1)
  findRoomGrid.setStyle("-fx-background-color:#d4dbe3;")
  findRoomGrid.setPadding(new Insets(60, 20, 60, 20))

  val borderPane = new BorderPane()
  borderPane.setTop(topBox)
  borderPane.setCenter(findRoomGrid)
  group.getChildren.addAll(backIcon,borderPane)


  var listener: FindSceneListener = _

  def setListener(listener: FindSceneListener): Unit = {
    this.listener = listener
  }
}
