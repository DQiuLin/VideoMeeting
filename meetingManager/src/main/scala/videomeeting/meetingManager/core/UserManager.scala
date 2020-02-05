package videomeeting.meetingManager.core

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import akka.stream.scaladsl.Flow
import videomeeting.protocol.ptcl.CommonInfo.{User, UserInfo}
import videomeeting.protocol.ptcl.CommonRsp
import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol._
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol._
import videomeeting.meetingManager.Boot.{executor, meetingManager, scheduler, timeout}
import videomeeting.meetingManager.common.{AppSettings, Common}
import videomeeting.meetingManager.core.UserActor.ChildDead
import videomeeting.meetingManager.models.dao.UserInfoDao
import videomeeting.meetingManager.protocol.ActorProtocol
import videomeeting.meetingManager.utils.SecureUtil
import videomeeting.meetingManager.utils.SecureUtil.nonceStr
import videomeeting.meetingManager.common.{AppSettings, Common}
import videomeeting.meetingManager.common.Common.TestConfig
import videomeeting.meetingManager.models.dao.UserInfoDao
import videomeeting.meetingManager.protocol.ActorProtocol

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success}
/**
  * created by ltm on
  * 2019/7/16
  */
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorAttributes, Supervision}
import akka.util.ByteString
import org.slf4j.LoggerFactory

object UserManager {

  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  private final case object BehaviorChangeKey

  case class TimeOut(msg:String) extends Command

  final case class WebSocketFlowSetup(userId:Int,meetingId:Int,temporary:Boolean,replyTo:ActorRef[Option[Flow[Message,Message,Any]]]) extends Command

  final case class Register(code:String, email:String, userName:String, password:String, replyTo:ActorRef[SignUpRsp]) extends Command

  final case class SetupWs(uidOpt:Int,meetingId:Int,replyTo: ActorRef[Option[Flow[Message, Message, Any]]]) extends Command

  final case class TemporaryUser(replyTo:ActorRef[GetTemporaryUserRsp]) extends Command

  case class DeleteTemporaryUser(userId:Int) extends Command


  private val tokenExistTime = AppSettings.tokenExistTime * 1000L // seconds
  private val deleteTemporaryDelay = AppSettings.guestTokenExistTime.seconds

  private[this] def switchBehavior(ctx: ActorContext[Command], behaviorName: String,
                                   behavior:Behavior[Command], durationOpt: Option[FiniteDuration] = None,
                                   timeOut:TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) ={
    println(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }


  def create():Behavior[Command] = {
    log.debug(s"UserManager start...")
    Behaviors.setup[Command]{
      ctx =>
        implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command]{
          implicit timer =>
            val userIdGenerator = new AtomicInteger(1)
            val temporaryUserMap = mutable.HashMap[Long, (Long, UserInfo)]()
            meetingManager ! ActorProtocol.AddUserActor4Test(TestConfig.TEST_USER_ID,Common.TestConfig.TEST_MEET_ID,getUserActor(Common.TestConfig.TEST_USER_ID,false,ctx))
            idle(userIdGenerator,temporaryUserMap)
        }
    }
  }

  //todo 临时用户token过期处理
  private def idle(
                    userIdGenerator:AtomicInteger,
                    temporaryUserMap:mutable.HashMap[Long,(Long, UserInfo)],//临时用户,userId,createTime,userInfo
                  )
      (implicit stashBuffer: StashBuffer[Command],timer:TimerScheduler[Command]):Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {

        case TemporaryUser(replyTo) =>
          val userId = userIdGenerator.getAndIncrement()
          val userInfo = UserInfo(userId, s"Guest$userId", Common.DefaultImg.headImg)
          replyTo ! GetTemporaryUserRsp(Some(userInfo))
          temporaryUserMap.put(userId,(System.currentTimeMillis(),userInfo))
          timer.startSingleTimer(s"DeleteTemporaryUser_$userId",DeleteTemporaryUser(userId),deleteTemporaryDelay)
          Behaviors.same

        case DeleteTemporaryUser(userId) =>
          temporaryUserMap.remove(userId)
          Behaviors.same

        case SetupWs(uid, meetingId,replyTo) =>
          log.debug(s"${ctx.self.path} ws start")
          val flowFuture: Future[Option[Flow[Message, Message, Any]]] = ctx.self ? (WebSocketFlowSetup(uid,meetingId,false, _))
          flowFuture.map(replyTo ! _)
          Behaviors.same

        case WebSocketFlowSetup(userId,meetingId,temporary,replyTo) =>
          if(temporary){
            val existRoom:Future[Boolean] = meetingManager ? (MeetingManager.ExistMeeting(meetingId,_))
            existRoom.map{exist =>
              if(exist){
                log.info(s"${ctx.self.path} websocket will setup for user:$userId")
                getUserActorOpt(userId,temporary,ctx) match{
                  case Some(actor) =>
                    log.debug(s"${ctx.self.path} setup websocket error:该账户已经登录userId=$userId,temporary=$temporary")
                    //TODO 重复登录相关处理
//                    actor ! UserActor.UserLogin(roomId,userId)
//                    replyTo ! Some(setupWebSocketFlow(actor))
                    replyTo ! None
                  case None =>
                    val userActor = getUserActor(userId, temporary,ctx)
                    userActor ! UserActor.UserLogin(meetingId,userId)
                    replyTo ! Some(setupWebSocketFlow(userActor))
                }


              }else{
                log.debug(s"${ctx.self.path} setup websocket error:the room doesn't exist")
                replyTo ! None
              }
            }
          }else{
            log.info(s"${ctx.self.path} websocket will setup for user:$userId")
            getUserActorOpt(userId,temporary,ctx) match{
              case Some(actor) =>
                log.debug(s"${ctx.self.path} setup websocket error:该账户已经登录userId=$userId,temporary=$temporary")
                //TODO 重复登录相关处理
//                actor ! UserActor.UserLogin(roomId,userId)
//                replyTo ! Some(setupWebSocketFlow(actor))
                replyTo ! None
              case None =>
                val userActor = getUserActor(userId, temporary,ctx)
                userActor ! UserActor.UserLogin(meetingId,userId)
                replyTo ! Some(setupWebSocketFlow(userActor))
            }
          }
          Behaviors.same

        case ChildDead(userId,temporary,actor) =>
          if(temporary){
            //token过期的时候删除用户
//            temporaryUserMap.remove(userId)
          }
          log.debug(s"${ctx.self.path} the child = ${ctx.children}")
          Behaviors.same

        case x =>
          log.warn(s"unknown msg: $x")
          Behaviors.unhandled
      }
    }

  private def getUserActor(userId:Int,temporary:Boolean,ctx: ActorContext[Command]) = {
    val childrenName = s"userActor-$userId-temp-$temporary"
    ctx.child(childrenName).getOrElse {
      val actor = ctx.spawn(UserActor.create(userId,temporary), childrenName)
      ctx.watchWith(actor, ChildDead(userId, temporary,actor))
      actor
    }.unsafeUpcast[UserActor.Command]
  }

  private def getUserActorOpt(userId:Int,temporary:Boolean,ctx:ActorContext[Command]) = {
    val childrenName = s"userActor-$userId-temp-$temporary"
    ctx.child(childrenName).map(_.unsafeUpcast[UserActor.Command])
  }


  private def setupWebSocketFlow(userActor:ActorRef[UserActor.Command]):Flow[Message,Message,Any]  = {
    import org.seekloud.byteobject.ByteObject._
    import org.seekloud.byteobject.MiddleBufferInJvm

    import scala.language.implicitConversions

    implicit def parseJsonString2WsMsgClient(s: String): Option[WsMsgClient] = {
      import io.circe.generic.auto._
      import io.circe.parser._

      try {
        val wsMsg = decode[WsMsgClient](s).right.get
        Some(wsMsg)
      } catch {
        case e: Exception =>
          log.warn(s"parse front msg failed when json parse,s=${s},e=$e")
          None
      }
    }
    Flow[Message]
      .collect {
        case TextMessage.Strict(m) =>
          log.debug(s"接收到ws消息，类型TextMessage.Strict，msg-${m}")
          UserActor.WebSocketMsg(m)

        case BinaryMessage.Strict(m) =>
//          log.debug(s"接收到ws消息，类型Binary")
          val buffer = new MiddleBufferInJvm(m.asByteBuffer)
          bytesDecode[WsMsgClient](buffer) match {
            case Right(req) =>
              UserActor.WebSocketMsg(Some(req))
            case Left(e) =>
              log.debug(s"websocket decode error:$e")
              UserActor.WebSocketMsg(None)
          }

        case x =>
          log.debug(s"$userActor recv a unsupported msg from websocket:$x")
          UserActor.WebSocketMsg(None)

      }
      .via(UserActor.flow(userActor))
      .map{
        case t: Wrap =>
//          val buffer = new MiddleBufferInJvm(16384)
//          val message = bytesDecode[WsMsgRm](buffer) match {
//            case Right(rst) => rst
//            case Left(e) => DecodeError
//          }
//
//          message match {
//            case HeatBeat(ts) =>
//              log.debug(s"heartbeat: $ts")
//
//            case x =>
//              log.debug(s"unknown msg:$x")
//
//          }
          BinaryMessage.Strict(ByteString(t.ws))
        case x =>
          log.debug(s"websocket send an unknown msg:$x")
          TextMessage.apply("")

      }

      .withAttributes(ActorAttributes.supervisionStrategy(decider = decider))
  }


  private val decider:Supervision.Decider = {
    e:Throwable =>
      e.printStackTrace()
      Supervision.Resume
  }



}
