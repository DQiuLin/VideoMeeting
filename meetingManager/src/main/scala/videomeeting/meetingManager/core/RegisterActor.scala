package videomeeting.meetingManager.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.byteobject.MiddleBufferInJvm
import videomeeting.protocol.ptcl.CommonRsp
import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol.SignUpRsp
import videomeeting.meetingManager.Boot.executor
import videomeeting.meetingManager.common.AppSettings._
import videomeeting.meetingManager.utils.SecureUtil
import org.slf4j.LoggerFactory
import videomeeting.meetingManager.models.dao.UserInfoDao
import videomeeting.protocol.ptcl

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by ltm on 2019/8/26.
  */
object RegisterActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class RegisterReq(userName:String, password: String, replyTo: ActorRef[SignUpRsp]) extends Command

  case class TimeOut(msg: String) extends Command

  private case object TimeoutKey

  private case object BehaviorChangeKey

  private val timeOutDuration = 24 * 60 * 60

  private final case class SwitchBehavior(
                                           name: String,
                                           behavior: Behavior[Command],
                                           durationOpt: Option[FiniteDuration] = None,
                                           timeOut: TimeOut = TimeOut("busy time error")
                                         ) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer: TimerScheduler[Command]) = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create() = {
    log.debug(s"RegisterActor start...")
    Behaviors.setup[Command] {ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer: MiddleBufferInJvm = new MiddleBufferInJvm(8192)
        idle()
      }
    }
  }

  def idle()
          (implicit stashBuffer:StashBuffer[Command],
           sendBuffer: MiddleBufferInJvm,
           timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case m: RegisterReq =>
          val timestamp = System.currentTimeMillis()
          UserInfoDao.addUser(m.userName, m.password, timestamp).onComplete {
            case Success(id) =>
              println("add user success")
              m.replyTo ! SignUpRsp()
            case Failure(e) =>
              log.debug(s"add register user failed, error: $e")
              m.replyTo ! SignUpRsp(180004, "add register user failed")
          }
          Behaviors.same

        //未知消息
        case x =>
          log.warn(s"unknown msg: $x")
          Behaviors.unhandled
      }
    }
  }

}

