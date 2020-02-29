package videomeeting.meetingManager.protocol

import akka.actor.typed.ActorRef
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol.WsMsgClient
import videomeeting.meetingManager.core.{MeetingManager, UserActor}
import videomeeting.meetingManager.core.{MeetingActor, MeetingManager, UserActor}

/**
  * created by benyafang on 2019.9.6 16:34
  * */
object ActorProtocol {

  trait RoomCommand extends MeetingManager.Command with MeetingActor.Command

  case class WebSocketMsgWithActor(userId: Int,roomId: Int,msg:WsMsgClient) extends RoomCommand

  case class UpdateSubscriber(join:Int,meetingId: Int,userId: Int,temporary:Boolean,userActorOpt:Option[ActorRef[UserActor.Command]]) extends RoomCommand

  case class StartMeeting4Host(userId: Int, roomId: Int, actor:ActorRef[UserActor.Command]) extends RoomCommand

  case class UserLeftRoom(userId: Int,temporary:Boolean,roomId: Int) extends RoomCommand

  final case class StartLiveAgain(roomId: Int) extends RoomCommand

  case class HostCloseRoom(roomId: Int) extends RoomCommand// 主播关闭房间

  case class AddUserActor4Test(userId: Int,roomId: Int,userActor: ActorRef[UserActor.Command])extends RoomCommand


  case class BanOnAnchor(roomId: Int) extends RoomCommand

  case class MeetingCreate(meetingId: Int) extends RoomCommand

  case class ModifyRoomDes(userId: Int, meetingId: Int, name: Option[String], des: Option[String]) extends RoomCommand

}
