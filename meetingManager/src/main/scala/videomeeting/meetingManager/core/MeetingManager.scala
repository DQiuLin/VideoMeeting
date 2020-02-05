package videomeeting.meetingManager.core

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import videomeeting.protocol.ptcl.CommonInfo.MeetInfo
import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol._
import videomeeting.meetingManager.Boot.{executor, scheduler, timeout}
import videomeeting.meetingManager.common.AppSettings._
import videomeeting.meetingManager.common.Common
import videomeeting.meetingManager.models.dao.UserInfoDao
import videomeeting.meetingManager.core.MeetingActor._
import videomeeting.meetingManager.protocol.ActorProtocol
import videomeeting.meetingManager.protocol.CommonInfoProtocol.WholeRoomInfo
import videomeeting.meetingManager.utils.{DistributorClient, ProcessorClient}
import org.slf4j.LoggerFactory
import videomeeting.meetingManager.common.Common
import videomeeting.meetingManager.common.Common.TestConfig
import videomeeting.meetingManager.models.dao.UserInfoDao
import videomeeting.meetingManager.protocol.ActorProtocol
import videomeeting.meetingManager.protocol.CommonInfoProtocol.WholeRoomInfo
import videomeeting.meetingManager.utils.DistributorClient
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol

import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * created by benyafang on 2019.7.16 am 10:32
  *
  * */
object MeetingManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  case class GetMeetingList(replyTo: ActorRef[MeetingListRsp]) extends Command

  case class SearchMeeting(userId:  Option[Int], roomId: Int, replyTo:ActorRef[SearchMeetingRsp]) extends Command

  case class UserInfoChange(userId:Int,temporary:Boolean) extends Command

  case class ExistMeeting(roomId:Int, replyTo:ActorRef[Boolean]) extends Command

  case class DelaySeekRecord(wholeRoomInfo:WholeRoomInfo, totalView:Int, roomId:Int, startTime:Long, liveId: String) extends Command
  case class OnSeekRecord(wholeRoomInfo:WholeRoomInfo, totalView:Int, roomId:Int, startTime:Long, liveId: String) extends Command

  //case class FinishPull(roomId: Int, startTime: Long, liveId: String) extends Command
  private final case object DelaySeekRecordKey

  private final case object FinishPullKey

  def create():Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      log.info(s"${ctx.self.path} setup")
      Behaviors.withTimers[Command]{implicit timer =>
        idle()
      }
    }
  }

  private def idle(
//                    roomInUse:mutable.HashMap[Long,RoomInfo]
                  ) //roomId -> (roomInfo, liveInfo)
                  (implicit stashBuffer: StashBuffer[Command],timer:TimerScheduler[Command]):Behavior[Command] = {

    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case GetMeetingList(replyTo) =>
          val roomInfoListFuture = ctx.children.map(_.unsafeUpcast[MeetingActor.Command]).map{ r =>
            val roomInfoFuture:Future[MeetInfo] = r ? (GetMeetingInfo(_))
            roomInfoFuture
          }.toList
          Future.sequence(roomInfoListFuture).map{seq =>
            replyTo ! MeetingListRsp(Some(seq))
          }
          Behaviors.same

        case r@ActorProtocol.AddUserActor4Test(userId,roomId,userActor) =>
          getMeetingActorOpt(roomId,ctx) match {
            case Some(actor) =>actor ! r
            case None =>
          }
          Behaviors.same

        case r@ActorProtocol.WebSocketMsgWithActor(userId,roomId,req) =>
          getMeetingActorOpt(roomId,ctx) match{
            case Some(actor) => actor ! r
            case None => log.debug(s"${ctx.self.path}请求错误，该房间还不存在，房间id=$roomId，用户id=$userId")
          }
          Behaviors.same

        case r@ActorProtocol.StartLiveAgain(roomId) =>
          getMeetingActorOpt(roomId,ctx) match{
            case Some(actor) => actor ! r
            case None => log.debug(s"${ctx.self.path}重新直播请求错误，该房间已经关闭，房间id=$roomId")
          }
          Behaviors.same


        case r@ActorProtocol.HostCloseRoom(roomId)=>
          //如果断开websocket的用户的id能够和已经开的房间里面的信息匹配上，就说明是主播
          getMeetingActorOpt(roomId, ctx) match{
            case Some(roomActor) => roomActor ! r
            case None =>log.debug(s"${ctx.self.path}关闭房间失败，房间不存在，id=$roomId")
          }
          Behaviors.same

        case r@ActorProtocol.UpdateSubscriber(join,roomId,userId,temporary,userActor) =>
          getMeetingActorOpt(roomId,ctx)match{
            case Some(actor) =>actor ! r
            case None =>log.debug(s"${ctx.self.path}更新用户信息失败，房间不存在，有可能该用户是主播等待房间开启，房间id=$roomId,用户id=$userId")
          }
          Behaviors.same

        case r@ActorProtocol.StartRoom4Anchor(userId,roomId,actor) =>
          getMeetingActor(roomId,ctx) ! r
          Behaviors.same


        case r@GetRtmpLiveInfo(roomId, replyTo)=>
          getMeetingActorOpt(roomId,ctx) match{
            case Some(actor) =>actor ! r
            case None =>
              log.debug(s"${ctx.self.path}房间未建立")
              replyTo ! GetLiveInfoRsp4RM(None,100041,s"获取live info 请求失败:房间不存在")
          }
          Behaviors.same

        case r@ActorProtocol.BanOnAnchor(roomId) =>
          getMeetingActorOpt(roomId,ctx) match{
            case Some(actor) =>actor ! r
            case None =>
              log.debug(s"${ctx.self.path}房间未建立")
          }
          Behaviors.same

        case SearchMeeting(userId, roomId, replyTo) =>
          if(roomId == Common.TestConfig.TEST_ROOM_ID){
            log.debug(s"${ctx.self.path} get test room mpd,roomId=${roomId}")
            getMeetingActorOpt(roomId,ctx) match{
              case Some(actor) =>
                val roomInfoFuture:Future[RoomInfo] = actor ? (GetRoomInfo(_))
                roomInfoFuture.map{r =>replyTo ! SearchRoomRsp(Some(r))}
              case None =>
                log.debug(s"${ctx.self.path} test room dead")
                replyTo ! SearchRoomError4RoomId
            }
          } else{
            getMeetingActorOpt(roomId,ctx) match{
              case Some(actor) =>
                val roomInfoFuture:Future[RoomInfo] = actor ? (GetRoomInfo(_))
                roomInfoFuture.map{r =>
                  r.rtmp match {
                    case Some(v) =>
                      log.debug(s"${ctx.self.path} search room,roomId=${roomId},rtmp=${r.rtmp}")
                      replyTo ! SearchRoomRsp(Some(r))//正常返回
                    case None =>
                      log.debug(s"${ctx.self.path} search room failed,roomId=${roomId},rtmp=None")
                      replyTo ! SearchRoomError(msg = s"${ctx.self.path} room rtmp is None")
                      /*ProcessorClient.getmpd(roomId).map{
                        case Right(rsp)=>
                          if(rsp.errCode == 0){
                            actor ! UpdateRTMP(rsp.rtmp)
                            val roomInfoFuture:Future[RoomInfo] = actor ? (GetRoomInfo(_))
                            roomInfoFuture.map{w =>
                              log.debug(s"${ctx.self.path} research room,roomId=${roomId},rtmp=${r.rtmp}")
                              replyTo ! SearchRoomRsp(Some(w))}//正常返回
                          }else if(rsp.errCode == 100024){
                            //replyTo ! SearchRoomRsp(Some(r))
                            replyTo ! SearchRoomRsp(Some(r), 100009, "stop push stream")//主播停播，房间还在
                          }
                          else{
                          log.info(s"getmpd code:${rsp.errCode}, msg${rsp.msg}")
                            replyTo ! SearchRoomError(errCode = rsp.errCode, msg = rsp.msg)//
                          }

                        case Left(error)=>
                          replyTo ! SearchRoomError4ProcessorDead//请求processor失败
                      }*/

                  }
                }
              case None =>
                log.debug(s"${ctx.self.path} test room dead")
                replyTo ! SearchRoomError4RoomId//主播关闭房间
            }
          }
          Behaviors.same

        case ExistMeeting(roomId,replyTo) =>
          getMeetingActorOpt(roomId,ctx) match {
            case Some(actor) =>
              replyTo ! true
            case None =>
              replyTo ! false
          }
          Behaviors.same

        case UserInfoChange(userId,temporary) =>
          UserInfoDao.searchById(userId).map{
              case Some(v) =>
                getMeetingActorOpt(v.roomid,ctx)match{
                  case Some(meetingActor) =>
                    meetingActor ! ActorProtocol.UpdateSubscriber(Common.Subscriber.change,v.roomid,userId,temporary,None)
                  case None =>
                    log.debug(s"${ctx.self.path}更新房间 ${v.roomid}的用户信息userId:$userId")
                }
              case None =>
                log.debug(s"${ctx.self.path}更新用户信息失败，用户信息不存在，userId:$userId")
          }
          Behaviors.same

        //延时请求获取录像（计时器）
        case DelaySeekRecord(wholeRoomInfo, totalView, roomId, startTime, liveId) =>
          log.info("---- wait seconds to seek record ----")
          timer.startSingleTimer(DelaySeekRecordKey + roomId.toString + startTime, OnSeekRecord(wholeRoomInfo, totalView, roomId, startTime, liveId), 5.seconds)
          Behaviors.same

        //延时请求获取录像
        case OnSeekRecord(wholeRoomInfo, totalView, roomId, startTime, liveId) =>
          timer.cancel(DelaySeekRecordKey + roomId.toString + startTime)
          DistributorClient.seekRecord(roomId,startTime).onComplete{
            case Success(v) =>
              v match{
                case Right(rsp) =>
                  log.debug(s"${ctx.self.path}获取录像id${roomId}时长为duration=${rsp.duration}")
                  RecordDao.addRecord(wholeRoomInfo.roomInfo.roomId,
                    wholeRoomInfo.roomInfo.roomName,wholeRoomInfo.roomInfo.roomDes,startTime,
                    UserInfoDao.getVideoImg(wholeRoomInfo.roomInfo.coverImgUrl),0,wholeRoomInfo.roomInfo.like,rsp.duration)
                  //timer.startSingleTimer(FinishPullKey + roomId.toString + startTime, FinishPull(roomId, startTime, liveId), 5.seconds)
                case Left(err) =>
                  log.debug(s"${ctx.self.path} 查询录像文件信息失败,error:$err")
              }

            case Failure(error) =>
              log.debug(s"${ctx.self.path} 查询录像文件失败,error:$error")
          }
          Behaviors.same



        case ChildDead(name,childRef) =>
          log.debug(s"${ctx.self.path} the child = ${ctx.children}")
          Behaviors.same

        case x =>
          log.debug(s"${ctx.self.path} recv an unknown msg")
          Behaviors.same
      }
    }
  }


  private def getMeetingActor(meetingId:Int, ctx: ActorContext[Command]) = {
    val childrenName = s"roomActor-${meetingId}"
    ctx.child(childrenName).getOrElse {
      val actor = ctx.spawn(MeetingActor.create(meetingId), childrenName)
      ctx.watchWith(actor,ChildDead(childrenName,actor))
      actor
    }.unsafeUpcast[MeetingActor.Command]
  }

  private def getMeetingActorOpt(meetingId:Int, ctx: ActorContext[Command]) = {
    val childrenName = s"roomActor-${meetingId}"
//    log.debug(s"${ctx.self.path} the child = ${ctx.children},get the roomActor opt = ${ctx.child(childrenName).map(_.unsafeUpcast[RoomActor.Command])}")
    ctx.child(childrenName).map(_.unsafeUpcast[MeetingActor.Command])

  }


}
