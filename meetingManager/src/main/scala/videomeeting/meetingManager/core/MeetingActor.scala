package videomeeting.meetingManager.core

import akka.actor
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import videomeeting.protocol.ptcl.CommonInfo._
import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol.{GetLiveInfoRsp, GetMeetInfoRsp4RM}
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol.{HostCloseRoom, _}
import videomeeting.meetingManager.Boot.{executor, meetingManager}
import videomeeting.meetingManager.common.AppSettings.{distributorIp, distributorPort}
import videomeeting.meetingManager.common.Common
import videomeeting.meetingManager.common.Common.{Like, Role}
import videomeeting.meetingManager.core.MeetingManager.GetRtmpLiveInfo
import videomeeting.meetingManager.models.dao.UserInfoDao
import videomeeting.meetingManager.protocol.ActorProtocol
import videomeeting.meetingManager.protocol.ActorProtocol.{BanOnAnchor, ModifyRoomDes}
import videomeeting.meetingManager.utils.{DistributorClient, ProcessorClient, RtpClient}

import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success}
import videomeeting.meetingManager.core.UserActor
import videomeeting.meetingManager.common.Common
import videomeeting.meetingManager.common.Common.{Like, Role, Subscriber}
import videomeeting.meetingManager.models.dao.UserInfoDao
import videomeeting.meetingManager.protocol.ActorProtocol
import videomeeting.meetingManager.protocol.CommonInfoProtocol.WholeRoomInfo
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol

object MeetingActor {

  import org.seekloud.byteobject.ByteObject._

  import scala.language.implicitConversions

  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command with MeetingManager.Command

  //private final case object DelayUpdateRtmpKey

  private final case class SwitchBehavior(
                                           name: String,
                                           behavior: Behavior[Command],
                                           durationOpt: Option[FiniteDuration] = None,
                                           timeOut: TimeOut = TimeOut("busy time error")
                                         ) extends Command

  private case class TimeOut(msg: String) extends Command

  private final case object BehaviorChangeKey

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer: TimerScheduler[Command]) = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  final case class TestRoom(meetingInfo: MeetingInfo) extends Command

  final case class GetMeetingInfo(replyTo: ActorRef[MeetingInfo]) extends Command //考虑后续房间的建立不依赖ws

  final case class UpdateRTMP(rtmp: String) extends Command

  private final val InitTime = Some(5.minutes)

  def create(meetingId: Int): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      log.debug(s"${ctx.self.path} setup")
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer: MiddleBufferInJvm = new MiddleBufferInJvm(8192)
        val subscribers = mutable.HashMap.empty[(Int, Boolean), ActorRef[UserActor.Command]]
        init(meetingId, subscribers)
      }
    }
  }

  private def init(
                    meetingId: Int,
                    subscribers: mutable.HashMap[(Int, Boolean), ActorRef[UserActor.Command]],
                    meetingInfoOpt: Option[MeetingInfo] = None
                  )
                  (
                    implicit stashBuffer: StashBuffer[Command],
                    timer: TimerScheduler[Command],
                    sendBuffer: MiddleBufferInJvm
                  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case ActorProtocol.MeetingCreate(`meetingId`) =>
          log.info("meetingActor get MeetingCreate")
          Behaviors.same

//        case ActorProtocol.StartMeeting4Host(userId, `meetingId`, actor) =>
//          log.debug(s"${ctx.self.path} 用户id=$userId 开启了新的会议id=$meetingId")
//          subscribers.put((userId, false), actor)
//          for {
//            data <- RtpClient.getLiveInfoFunc()
//            userTableOpt <- UserInfoDao.searchById(userId)
//          } yield {
//            data match {
//              case Right(rsp) =>
//                if (userTableOpt.nonEmpty) {
//                  log.info(s"start meeting succeed")
//                  val meetingInfo = MeetingInfo(meetingId, s"${userTableOpt.get.username}的会议", s"${userTableOpt.get.username}的会议", userTableOpt.get.id, userTableOpt.get.username, userTableOpt.get.headImg, Some(0))
//                  DistributorClient.startPull(meetingId, rsp.liveInfo.liveId).map {
//                    case Right(r) =>
//                      log.info(s"distributor startPull succeed, get live address: ${r.liveAdd}")
//                      dispatchTo(subscribers)(List((userId, false)), StartLiveRsp(Some(rsp.liveInfo)))
//                      meetingInfo.mpd = Some(r.liveAdd)
//                      val startTime = r.startTime
//                      ctx.self ! SwitchBehavior("idle", idle(meetingInfo, mutable.HashMap(Role.host -> mutable.HashMap(userId -> rsp.liveInfo)), subscribers, 0, startTime))
//
//                    case Left(e) =>
//                      log.error(s"distributor startPull error: $e")
//                      dispatchTo(subscribers)(List((userId, false)), StartLiveRefused4LiveInfoError)
//                      ctx.self ! SwitchBehavior("init", init(meetingId, subscribers))
//                  }
//                } else {
//                  log.debug(s"${ctx.self.path} 开始会议被拒绝，数据库中没有该用户的数据，userId=$userId")
//                  dispatchTo(subscribers)(List((userId, false)), StartLiveRefused)
//                  ctx.self ! SwitchBehavior("init", init(meetingId, subscribers))
//                }
//              case Left(error) =>
//                log.debug(s"${ctx.self.path} 开始会议被拒绝，请求rtp server解析失败，error:$error")
//                dispatchTo(subscribers)(List((userId, false)), StartLiveRefused)
//                ctx.self ! SwitchBehavior("init", init(meetingId, subscribers))
//            }
//          }
//          switchBehavior(ctx, "busy", busy(), InitTime, TimeOut("busy"))

        case GetMeetingInfo(replyTo) =>
          if (meetingInfoOpt.nonEmpty) {
            replyTo ! meetingInfoOpt.get
          } else {
            log.debug("会议信息未更新")
            replyTo ! MeetingInfo(-1, "", "", -1, "", "")
          }
          Behaviors.same

        case TestRoom(meetingInfo) =>
          //仅用户测试使用空房间
          idle(meetingInfo, mutable.HashMap[Int, mutable.HashMap[Int, LiveInfo]](), subscribers, 0, System.currentTimeMillis())

        case ActorProtocol.AddUserActor4Test(userId, roomId, userActor) =>
          subscribers.put((userId, false), userActor)
          Behaviors.same

        case ActorProtocol.UpdateSubscriber(join, meetingId, userId, temporary, userActorOpt) =>
          log.info(s"${ctx.self.path}新用户加入会议meetingId=$meetingId,userId=$userId in init")
          for {
            data <- RtpClient.getLiveInfoFunc()
            userTableOpt <- UserInfoDao.searchById(userId)
          } yield {
            data match {
              case Right(rsp) =>
                if (userTableOpt.nonEmpty) {
                  log.info(s"start meeting succeed")
                  subscribers.put((userId, temporary), userActorOpt.get)
                  val meetingInfo = MeetingInfo(meetingId, s"${userTableOpt.get.username}的会议", s"${userTableOpt.get.username}的会议", userTableOpt.get.id, userTableOpt.get.username, userTableOpt.get.headImg, Some(0))
                  ctx.self ! SwitchBehavior("idle", idle(meetingInfo, mutable.HashMap(Role.host -> mutable.HashMap(userId -> rsp.liveInfo)), subscribers, 0, 0L))
                } else {
                  log.debug(s"${ctx.self.path} 开始会议被拒绝，数据库中没有该用户的数据，userId=$userId")
                  dispatchTo(subscribers)(List((userId, false)), StartLiveRefused)
                  ctx.self ! SwitchBehavior("init", init(meetingId, subscribers))
                }

              case Left(error) =>
                log.debug(s"${ctx.self.path} 开始会议被拒绝，请求rtp server解析失败，error:$error")
                dispatchTo(subscribers)(List((userId, false)), StartLiveRefused)
                ctx.self ! SwitchBehavior("init", init(meetingId, subscribers))
            }
          }
          switchBehavior(ctx, "busy", busy(), InitTime, TimeOut("busy"))

        case x =>
          log.debug(s"${ctx.self.path} recv an unknown msg:$x in init state...")
          Behaviors.same
      }
    }
  }

  private def idle(
    meetingInfo: MeetingInfo,
    liveInfoMap: mutable.HashMap[Int, mutable.HashMap[Int, LiveInfo]],
    subscribe: mutable.HashMap[(Int, Boolean), ActorRef[UserActor.Command]], //需要区分订阅的用户的身份，注册用户还是临时用户(uid,是否是临时用户true:是)
    viewNum: Int,
    startTime: Long
  )
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command],
      sendBuffer: MiddleBufferInJvm
    ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case ActorProtocol.AddUserActor4Test(userId, roomId, userActor) =>
          subscribe.put((userId, false), userActor)
          Behaviors.same

        case GetMeetingInfo(replyTo) =>
          replyTo ! meetingInfo
          Behaviors.same

        case ModifyRoomDes(uid, mid, name, des) =>
          val roomInfo = if (name.nonEmpty && des.nonEmpty) {
            meetingInfo.copy(meetingName = name.get, roomDes = des.get)
          } else if (name.nonEmpty) {
            meetingInfo.copy(meetingName = name.get)
          } else if (des.nonEmpty) {
            meetingInfo.copy(roomDes = des.get)
          } else {
            meetingInfo
          }
          idle(roomInfo, liveInfoMap, subscribe, viewNum, startTime)

        case UpdateRTMP(rtmp) =>
          //timer.cancel(DelayUpdateRtmpKey + wholeRoomInfo.roomInfo.roomId.toString)
          val newMeetingInfo = meetingInfo.copy(rtmp = Some(rtmp))
          log.debug(s"${ctx.self.path} 更新liveId=$rtmp,更新后的liveId=${meetingInfo.rtmp}")
          idle(newMeetingInfo, liveInfoMap, subscribe, viewNum, startTime)

        case ActorProtocol.WebSocketMsgWithActor(userId, roomId, wsMsg) =>
          handleWebSocketMsg(meetingInfo, subscribe, liveInfoMap, startTime, dispatch(subscribe), dispatchTo(subscribe))(ctx, userId, roomId, wsMsg)

        case GetRtmpLiveInfo(_, replyTo) =>
          log.debug(s"meeting${meetingInfo.meetingId}获取liveId成功")
          liveInfoMap.get(Role.host) match {
            case Some(value) =>
              replyTo ! GetMeetInfoRsp4RM(Some(value.values.head))
            case None =>
              log.debug(s"${ctx.self.path} no host live info,meetingId=${meetingInfo.meetingId}")
              replyTo ! GetMeetInfoRsp4RM(None)

          }
          Behaviors.same

        case ActorProtocol.UpdateSubscriber(join, meetingId, userId, temporary, userActorOpt) =>
          var view = viewNum
          //虽然房间存在，但其实主播已经关闭房间，这时的startTime=-1
          //向所有人发送主播已经关闭房间的消息
          log.info(s"-----roomActor get UpdateSubscriber id: $meetingId")
          if (startTime == -1) {
            dispatchTo(subscribe)(List((userId, temporary)), NoAuthor)
          }
          else {
            if (join == Subscriber.attendance) {
              view += 1
              log.debug(s"${ctx.self.path}新用户加入会议meetingId=$meetingId,userId=$userId")
              subscribe.put((userId, temporary), userActorOpt.get)
            } else if (join == Common.Subscriber.left) {
              log.debug(s"${ctx.self.path}用户离开会议meetingId=$meetingId,userId=$userId")
              subscribe.remove((userId, temporary))
              if (liveInfoMap.contains(Role.attendance)) {
                if (liveInfoMap(Role.attendance).contains(userId)) {
                  meetingInfo.rtmp match {
                    case Some(v) =>
                      if (v != liveInfoMap(Role.host)(meetingInfo.userId).liveId) {
                        liveInfoMap.remove(Role.attendance)
                        ProcessorClient.closeRoom(meetingId)
                        ctx.self ! UpdateRTMP(liveInfoMap(Role.host)(meetingInfo.userId).liveId)
                        dispatch(subscribe)(AuthProtocol.AudienceDisconnect(liveInfoMap(Role.host)(meetingInfo.userId).liveId))
                        dispatch(subscribe)(RcvComment(-1l, "", s"the attendance has shut the join in meeting $meetingId"))
                      }
                    case None =>
                      log.debug("no host liveId when audience left room")
                  }
                }
              }
            }
          }
          //所有的注册用户
          val attendanceList = subscribe.filterNot(_._1 == (meetingInfo.userId, false)).keys.toList.filter(r => !r._2).map(_._1)
          val temporaryList = subscribe.filterNot(_._1 == (meetingInfo.userId, false)).keys.toList.filter(r => r._2).map(_._1)
          UserInfoDao.getUserInfo(attendanceList).onComplete {
            case Success(rst) =>
              val temporaryUserDesList = temporaryList.map(r => UserInfo(r, s"guest_$r", Common.DefaultImg.headImg, "", -1L))
              dispatch(subscribe)(UpdateAudienceInfo(rst ++ temporaryUserDesList))
            case Failure(_) =>

          }
          meetingInfo.attendanceNum = Some(subscribe.size - 1)
          idle(meetingInfo, liveInfoMap, subscribe, view, startTime)

        case ActorProtocol.HostCloseRoom(roomId) =>
          log.debug(s"${ctx.self.path} host close the room")
          meetingInfo.rtmp match {
            case Some(v) =>
              if(v != liveInfoMap(Role.host)(meetingInfo.userId).liveId){
                ProcessorClient.closeRoom(meetingInfo.meetingId)
              }
            case None =>
          }
          //          if (wholeRoomInfo.roomInfo.rtmp.get != liveInfoMap(Role.host)(wholeRoomInfo.roomInfo.userId).liveId)
          //            ProcessorClient.closeRoom(wholeRoomInfo.roomInfo.roomId)
          if (startTime != -1l) {
            log.debug(s"${ctx.self.path} 主播向distributor发送finishPull请求")
            DistributorClient.finishPull(liveInfoMap(Role.host)(meetingInfo.userId).liveId)
            meetingManager ! MeetingManager.DelaySeekRecord(meetingInfo, viewNum, roomId, startTime, liveInfoMap(Role.host)(meetingInfo.userId).liveId)
          }
          dispatchTo(subscribe)(subscribe.filter(r => r._1 != (meetingInfo.userId, false)).keys.toList, HostCloseRoom())
          Behaviors.stopped

        case ActorProtocol.StartLiveAgain(roomId) =>
          log.debug(s"${ctx.self.path} the room actor has been exist,the host restart the room")
          for {
            data <- RtpClient.getLiveInfoFunc()
          } yield {
            data match {
              case Right(rsp) =>
                liveInfoMap.put(Role.host, mutable.HashMap(meetingInfo.userId -> rsp.liveInfo))
                val liveList = liveInfoMap.toList.sortBy(_._1).flatMap(r => r._2).map(_._2.liveId)
                //timer.startSingleTimer(DelayUpdateRtmpKey + roomId.toString, UpdateRTMP(rsp.liveInfo.liveId), 4.seconds)
                DistributorClient.startPull(roomId, rsp.liveInfo.liveId).map {
                  case Right(r) =>
                    log.info("distributor startPull succeed")
                    val startTime = r.startTime
                    val newMeetingInfo = meetingInfo.copy(attendanceNum = Some(0), mpd = Some(r.liveAdd), rtmp = Some(rsp.liveInfo.liveId))
                    dispatchTo(subscribe)(List((meetingInfo.userId, false)), StartLiveRsp(Some(rsp.liveInfo)))
                    ctx.self ! SwitchBehavior("idle", idle(newMeetingInfo, liveInfoMap, subscribe, 0, startTime))
                  case Left(e) =>
                    log.error(s"distributor startPull error: $e")
                    val newMeetingInfo = meetingInfo.copy(attendanceNum = Some(0))
                    dispatchTo(subscribe)(List((meetingInfo.userId, false)), StartLiveRsp(Some(rsp.liveInfo)))
                    ctx.self ! SwitchBehavior("idle", idle(newMeetingInfo, liveInfoMap, subscribe, 0, startTime))
                }


              case Left(str) =>
                log.debug(s"${ctx.self.path} 重新开始直播失败=$str")
                dispatchTo(subscribe)(List((meetingInfo.userId, false)), StartLiveRefused4LiveInfoError)
                ctx.self ! ActorProtocol.HostCloseRoom(meetingInfo.meetingId)
                ctx.self ! SwitchBehavior("idle", idle(meetingInfo, liveInfoMap, subscribe, 0, startTime))
            }
          }
          switchBehavior(ctx, "busy", busy(), InitTime, TimeOut("busy"))

        case x =>
          log.debug(s"${ctx.self.path} recv an unknown msg $x")
          Behaviors.same
      }
    }
  }

  private def busy()
                  (
                    implicit stashBuffer: StashBuffer[Command],
                    timer: TimerScheduler[Command],
                    sendBuffer: MiddleBufferInJvm
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, b, durationOpt, timeOut) =>
          switchBehavior(ctx, name, b, durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy, msg=$m")
          Behaviors.stopped

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }

  //websocket处理消息的函数
  /**
   * userActor --> roomManager --> roomActor --> userActor
   * roomActor
   * subscribers:map(userId,userActor)
   *
   *
   *
   **/
  private def handleWebSocketMsg(
    meetingInfo: MeetingInfo,
    subscribers: mutable.HashMap[(Int, Boolean), ActorRef[UserActor.Command]], //包括主持人在内的所有用户
    liveInfoMap: mutable.HashMap[Int, mutable.HashMap[Int, LiveInfo]], //除主持人外的所有用户的liveInfo
    startTime: Long,
    dispatch: WsMsgRm => Unit,
    dispatchTo: (List[(Int, Boolean)], WsMsgRm) => Unit
  )
    (ctx: ActorContext[Command], userId: Int, meetingId: Int, msg: WsMsgClient)
    (
      implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command],
      sendBuffer: MiddleBufferInJvm
    ): Behavior[Command] = {
    msg match {
      case msg: CloseUserImageAndAudio =>
        dispatchTo(List((msg.userId, false)), HostCloseUser(msg.image, msg.audio))
        Behaviors.same

      case msg: SetSpeaker =>
        dispatch(HostSetSpeaker(msg.userId))
        Behaviors.same

      case ApplyReq(userId4Audience, `meetingId`, clientType) =>
        UserInfoDao.searchById(userId4Audience).map { r =>
          if (r.nonEmpty) {
            dispatchTo(List((meetingInfo.userId, false)), AudienceApply(userId4Audience, r.get.username, clientType))
          } else {
            log.debug(s"${ctx.self.path} 发言请求失败，用户id错误id=$userId4Audience in meetingId=$meetingId")
            dispatchTo(List((userId4Audience, false)), ApplyAccountError)
          }
        }.recover {
          case e: Exception =>
            log.debug(s"${ctx.self.path} 发言请求失败，内部错误error=$e")
            dispatchTo(List((userId4Audience, false)), ApplyInternalError)
        }
        Behaviors.same


      case JoinReq(userId4Audience, `meetingId`, clientType) =>
        log.info(s"get JoinReq")
          UserInfoDao.searchById(userId4Audience).map { r =>
            if (r.nonEmpty) {
              dispatchTo(List((meetingInfo.userId, false)), AudienceJoin(userId4Audience, r.get.username, clientType))
            } else {
              log.debug(s"${ctx.self.path} 连线请求失败，用户id错误id=$userId4Audience in roomId=$meetingId")
              dispatchTo(List((userId4Audience, false)), JoinAccountError)
            }
          }.recover {
            case e: Exception =>
              log.debug(s"${ctx.self.path} 连线请求失败，内部错误error=$e")
              dispatchTo(List((userId4Audience, false)), JoinInternalError)
          }
        Behaviors.same

      case AudienceShutJoin(`meetingId`, `userId`) =>
        log.debug(s"${ctx.self.path} the audience connection has been shut")
        //TODO 目前是某个观众退出则关闭会议，应该修改为不关闭整个会议
        liveInfoMap.clear()
        ctx.self ! UpdateRTMP(meetingInfo.rtmp.get)
        //            val liveList = liveInfoMap.toList.sortBy(_._1).flatMap(r => r._2).map(_._2.liveId)
        //            ProcessorClient.updateRoomInfo(wholeRoomInfo.roomInfo.roomId, liveList, wholeRoomInfo.layout, wholeRoomInfo.aiMode, 0l)
        dispatch(AuthProtocol.AudienceDisconnect(meetingInfo.rtmp.get))
        dispatch(RcvComment(-1l, "", s"the audience has shut the join in room $meetingId"))
        Behaviors.same

      case JoinAccept(`meetingId`, userId4Audience, clientType, accept) =>
        log.debug(s"${ctx.self.path} 接受加入请求，meetingId=$meetingId")
        if (accept) {
          for {
            userInfoOpt <- UserInfoDao.searchById(userId4Audience)
            clientLiveInfo <- RtpClient.getLiveInfoFunc()
            mixLiveInfo <- RtpClient.getLiveInfoFunc()
          } yield {
            (clientLiveInfo, mixLiveInfo) match {
              case (Right(GetLiveInfoRsp(liveInfo4Client, 0, _)), Right(GetLiveInfoRsp(liveInfo4Mix, 0, _))) =>
                log.info("client" + liveInfo4Client + "; mix" + liveInfo4Mix)
                if (userInfoOpt.nonEmpty) {
                  liveInfoMap.get(Role.host) match {
                    case Some(value) =>
                      val liveIdHost = value.get(meetingInfo.userId)
                      if (liveIdHost.nonEmpty) {
                        liveInfoMap.get(Role.attendance) match {
                          case Some(value4Audience) =>
                            value4Audience.put(userId4Audience, liveInfo4Client)
                            liveInfoMap.put(Role.attendance, value4Audience)
                          case None =>
                            liveInfoMap.put(Role.attendance, mutable.HashMap(userId4Audience -> liveInfo4Client))
                        }
                        liveIdHost.foreach { HostLiveInfo =>
                          DistributorClient.startPull(meetingId, liveInfo4Mix.liveId)
                          val clientList = liveInfoMap(Role.attendance).flatMap(m => List(m._2.liveId)).toList
                          ProcessorClient.newConnect(meetingId, HostLiveInfo.liveId, clientList, liveInfo4Mix.liveId, liveInfo4Mix.liveCode, 0L)
                          ctx.self ! UpdateRTMP(liveInfo4Mix.liveId)
                        }
                        //                        val liveList = liveInfoMap.toList.sortBy(_._1).flatMap(r => r._2).map(_._2.liveId)
                        //                        log.debug(s"${ctx.self.path} 更新房间信息，连线者liveIds=${liveList}")
                        //                        ProcessorClient.updateRoomInfo(wholeRoomInfo.roomInfo.roomId, liveList, wholeRoomInfo.layout, wholeRoomInfo.aiMode, 0l)

                        val audienceInfo = AttendenceInfo(userId4Audience, userInfoOpt.get.username, liveInfo4Client.liveId)
//                        dispatch(RcvComment(-1l, "", s"user:$userId join in room:$roomId")) //群发评论
//                        dispatchTo(subscribers.keys.toList.filter(t => t._1 != meetingInfo.userId && t._1 != userId4Audience), Join4AllRsp(Some(liveInfo4Mix.liveId))) //除了host和连线者发送混流的liveId
                        dispatchTo(List((meetingInfo.userId, false)), AudienceJoinRsp(Some(audienceInfo)))
                        dispatchTo(List((userId4Audience, false)), JoinRsp(Some(liveIdHost.get.liveId), Some(liveInfo4Client)))
                      } else {
                        log.debug(s"${ctx.self.path} 没有主播的liveId,roomId=$meetingId")
                        dispatchTo(List((meetingInfo.userId, false)), AudienceJoinError)

                      }
                    case None =>
                      log.debug(s"${ctx.self.path} 没有主播的liveId,roomId=$meetingId")
                      dispatchTo(List((meetingInfo.userId, false)), AudienceJoinError)
                  }
                } else {
                  log.debug(s"${ctx.self.path} 错误的主播userId,可能是数据库里没有用户,userId=$userId4Audience")
                  dispatchTo(List((meetingInfo.userId, false)), AudienceJoinError)
                }
              case (Right(GetLiveInfoRsp(_, errCode, msg)), Right(GetLiveInfoRsp(_, errCode2, msg2))) =>
                log.debug(s"${ctx.self.path.name} join accept get liveInfo4Client left error:$errCode msg: $msg")
                log.debug(s"${ctx.self.path.name} join accept get liveInfo4Mix left error:$errCode2 msg: $msg2")
                dispatchTo(List((meetingInfo.userId, false)), AudienceJoinError)
              case (Left(error1), Left(error2)) =>
                log.debug(s"${ctx.self.path.name} join accept get liveInfo4Client left error:$error1")
                log.debug(s"${ctx.self.path.name} join accept get liveInfo4Mix left error:$error2")
                dispatchTo(List((meetingInfo.userId, false)), AudienceJoinError)
              case (_, Left(error2)) =>
                log.debug(s"${ctx.self.path.name} join accept get liveInfo4Client left ok")
                log.debug(s"${ctx.self.path.name} join accept get liveInfo4Mix left error:$error2")
                dispatchTo(List((meetingInfo.userId, false)), AudienceJoinError)
              case (Left(error1), _) =>
                log.debug(s"${ctx.self.path.name} join accept get liveInfo4Client left error:$error1")
                log.debug(s"${ctx.self.path.name} join accept get liveInfo4Mix left ok")
                dispatchTo(List((meetingInfo.userId, false)), AudienceJoinError)
            }
          }
        } else {
          dispatchTo(List((meetingInfo.userId, false)), AudienceJoinRsp(None))
          dispatchTo(List((userId4Audience, false)), JoinRefused)
        }

        Behaviors.same

      case ForceExit(userId4Audience,userNa4Audience)=>
        if(liveInfoMap.contains(userId4Audience)){
          log.debug(s"host force user-$userId4Audience to exit")
          ProcessorClient.forceExit(meetingId, liveInfoMap(Role.attendance)(userId4Audience).liveId, System.currentTimeMillis())
          liveInfoMap.remove(userId4Audience)
          dispatchTo(subscribers.filter(_._1._1 != userId).keys.toList,ForceExitRsp(userId4Audience, userNa4Audience))
        } else{
          log.debug(s"host force user-$userId4Audience to leave, but there is no user!")
        }
        Behaviors.same

      case ModifyRoomInfo(roomName, roomDes) =>
        val roomInfo = if (roomName.nonEmpty && roomDes.nonEmpty) {
          meetingInfo.copy(meetingName = roomName.get, roomDes = roomDes.get)
        } else if (roomName.nonEmpty) {
          meetingInfo.copy(meetingName = roomName.get)
        } else if (roomDes.nonEmpty) {
          meetingInfo.copy(roomDes = roomDes.get)
        } else {
          meetingInfo
        }
        log.debug(s"${ctx.self.path} modify the room info$roomInfo")
        dispatch(UpdateRoomInfo2Client(roomInfo.meetingName, roomInfo.roomDes))
        dispatchTo(List((roomInfo.userId, false)), ModifyRoomRsp())
        idle(roomInfo, liveInfoMap, subscribers, subscribers.size - 1, startTime)

      case x =>
        Behaviors.same
    }
  }

  private def dispatch(subscribers: mutable.HashMap[(Int, Boolean), ActorRef[UserActor.Command]])(msg: WsMsgRm)(implicit sendBuffer: MiddleBufferInJvm): Unit = {
    log.debug(s"${subscribers}分发消息：$msg")
    subscribers.values.foreach(_ ! UserActor.DispatchMsg(Wrap(msg.asInstanceOf[WsMsgRm].fillMiddleBuffer(sendBuffer).result()), msg.isInstanceOf[AuthProtocol.HostCloseRoom]))
  }

  /**
   * subscribers:所有的订阅者
   * targetUserIdList：要发送的目标用户
   * msg：发送的消息
   **/
  private def dispatchTo(subscribers: mutable.HashMap[(Int, Boolean), ActorRef[UserActor.Command]])(targetUserIdList: List[(Int, Boolean)], msg: WsMsgRm)(implicit sendBuffer: MiddleBufferInJvm): Unit = {
    log.debug(s"${subscribers}定向分发消息：$msg")
    targetUserIdList.foreach { k =>
      subscribers.get(k).foreach(r => r ! UserActor.DispatchMsg(Wrap(msg.asInstanceOf[WsMsgRm].fillMiddleBuffer(sendBuffer).result()), msg.isInstanceOf[AuthProtocol.HostCloseRoom]))
    }
  }


}
