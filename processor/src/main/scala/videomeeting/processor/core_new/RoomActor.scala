package videomeeting.processor.core_new

import java.io.{File, InputStream, OutputStream}
import java.nio.channels.Channels
import java.nio.channels.Pipe.{SinkChannel, SourceChannel}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import videomeeting.processor.common.AppSettings.{debugPath, isDebug}
import videomeeting.processor.stream.PipeStream
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

import videomeeting.processor.Boot.{streamPullActor, streamPushActor}

import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/10/22
  * Time at 下午2:28
  *
  * actor由RoomManager创建
  * 连线房间
  * 管理多路grabber和一路recorder
  */
object RoomActor {

  private  val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class NewRoom(roomId: Long,client: List[String], pushLiveId: String, pushLiveCode: String, layout: Int) extends Command

  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case class ForceClientExit(roomId:Long,liveId:String) extends Command

  case class Recorder(roomId: Long, recorderRef: ActorRef[RecorderActor.Command]) extends Command

  case class CloseRoom(roomId: Long) extends Command

  case class ChildDead4Grabber(roomId: Long, childName: String, value: ActorRef[GrabberActor.Command]) extends Command// fixme liveID

  case class ChildDead4Recorder(roomId: Long, childName: String, value: ActorRef[RecorderActor.Command]) extends Command

  case class ChildDead4PushPipe(liveId: String, childName: String, value: ActorRef[StreamPushPipe.Command]) extends Command

  case class ChildDead4PullPipe(liveId: String, childName: String, value: ActorRef[StreamPullPipe.Command]) extends Command

  case class ClosePipe(liveId: String) extends Command

  case object Timer4Stop

  case object Stop extends Command

  case class Timer4PipeClose(liveId: String)

  val pipeMap = mutable.Map[String, PipeStream]()

  val pullPipeMap = mutable.Map[String, ActorRef[StreamPullPipe.Command]]()
  val pushPipeMap = mutable.Map[String, ActorRef[StreamPushPipe.Command]]()

  def create(roomId: Long, client: List[String], pushLiveId: String, pushLiveCode: String,  startTime:Long): Behavior[Command]= {
    Behaviors.setup[Command]{ ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"grabberManager start----")
          work(mutable.Map[Long, List[ActorRef[GrabberActor.Command]]](), mutable.Map[Long,ActorRef[RecorderActor.Command]](), mutable.Map[Long, List[String]]())
      }
    }
  }

  def work(
            grabberMap: mutable.Map[Long, List[ActorRef[GrabberActor.Command]]],
            recorderMap: mutable.Map[Long,ActorRef[RecorderActor.Command]],
            roomLiveMap: mutable.Map[Long, List[String]]
          )(implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {

        case msg:NewRoom =>
          log.info(s"${ctx.self} receive a msg $msg")
          if (isDebug) {
            val file = new File(debugPath + msg.roomId)
            if (!file.exists()) {
              file.mkdir()
            }
          }
          val pushPipe = new PipeStream
          val pushSink = pushPipe.getSink
          val pushSource= pushPipe.getSource
          val pushOut = Channels.newOutputStream(pushSink)

          var grabberList = List[ActorRef[GrabberActor.Command]]()

          msg.client.foreach { client =>
            val pullPipe4Client = new PipeStream
            val pullSink4Client = pullPipe4Client.getSink
            val pullSource4Client = pullPipe4Client.getSource
            val pullInput4Client = Channels.newInputStream(pullSource4Client)
            val pullOut4Client = Channels.newOutputStream(pullSink4Client)

            pipeMap.put(client, pullPipe4Client)

            val recorderActor = getRecorderActor(ctx, msg.roomId, msg.client ,msg.pushLiveId, msg.pushLiveCode, msg.layout, pushOut)
            val grabber4client = getGrabberActor(ctx, msg.roomId,client, pullInput4Client, recorderActor)

            val pullPipe4client = getPullPipe(ctx, msg.roomId, client, pullOut4Client)
            val pushPipe4recorder = getPushPipe(ctx, msg.roomId, msg.pushLiveId, msg.pushLiveCode, pushSource)

            pullPipeMap.put(client, pullPipe4client)
            pushPipeMap.put(msg.pushLiveId, pushPipe4recorder)

            grabberList = grabberList :+ grabber4client
            recorderMap.put(msg.roomId, recorderActor)
          }

          grabberMap.put(msg.roomId, grabberList)
          roomLiveMap.put(msg.roomId, msg.client :+ msg.pushLiveId)
          pipeMap.put(msg.pushLiveId, pushPipe)

          streamPushActor ! StreamPushActor.NewLive(msg.pushLiveId, msg.pushLiveCode)
          Behaviors.same

        case UpdateRoomInfo(roomId, layout) =>
          if(recorderMap.get(roomId).nonEmpty) {
            recorderMap.get(roomId).foreach(_ ! RecorderActor.UpdateRoomInfo(roomId,layout ))
          } else {
            log.info(s"${roomId} recorder not exist")
          }
          Behaviors.same

        case msg:Recorder =>
          log.info(s"${ctx.self} receive a msg $msg")
          val grabberActor = grabberMap.get(msg.roomId)
          if(grabberActor.isDefined){
            grabberActor.get.foreach(_ ! GrabberActor.Recorder(msg.recorderRef))
          } else {
            log.info(s"${msg.roomId} grabbers not exist")
          }
          Behaviors.same

        case msg:ForceClientExit =>
          //TODO

          Behaviors.same

        case CloseRoom(roomId) =>
          log.info(s"${ctx.self} receive a msg $msg")
          if(grabberMap.get(roomId).nonEmpty){
            grabberMap.get(roomId).foreach{g => g.foreach(_ ! GrabberActor.StopGrabber)}
            grabberMap.remove(roomId)
          } else {
            log.info(s"${roomId}  grabbers not exist when closeRoom")
          }
          if(recorderMap.get(roomId).nonEmpty) {
            recorderMap.get(roomId).foreach(_ ! RecorderActor.StopRecorder)
            recorderMap.remove(roomId)
          } else{
            log.info(s"${roomId}  recorder not exist when closeRoom")

          }
          if(roomLiveMap.get(roomId).nonEmpty){
            streamPullActor ! StreamPullActor.RoomClose(roomLiveMap(roomId))
            roomLiveMap.get(roomId).foreach{live =>
              live.foreach{l =>
                pullPipeMap.get(l).foreach( a => a ! StreamPullPipe.ClosePipe)
                timer.startSingleTimer(Timer4PipeClose(l), ClosePipe(l),1000.milli)
              }
            }
            roomLiveMap.remove(roomId)
          } else {
            log.info(s"${roomId}  pipe not exist when closeRoom")
          }
          timer.startSingleTimer(Timer4Stop, Stop, 1500.milli)
          Behaviors.same

        case ClosePipe(liveId) =>
          pushPipeMap.get(liveId).foreach( a => a ! StreamPushPipe.ClosePipe)
          pullPipeMap.remove(liveId)
          pushPipeMap.remove(liveId)
          pipeMap.remove(liveId)
          Behaviors.same

        case Stop =>
          log.info(s"${ctx.self} stopped ------")
          Behaviors.stopped

        case ChildDead4Grabber(roomId, childName, value) =>
          log.info(s"${childName} is dead ")
          grabberMap.remove(roomId)
          Behaviors.same

        case ChildDead4Recorder(roomId, childName, value) =>
          log.info(s"${childName} is dead ")
          recorderMap.remove(roomId)
          Behaviors.same

        case ChildDead4PullPipe(liveId, childName, value) =>
          log.info(s"${childName} is dead ")
          pullPipeMap.remove(liveId)
          Behaviors.same

        case ChildDead4PushPipe(liveId, childName, value) =>
          log.info(s"${childName} is dead ")
          pushPipeMap.remove(liveId)
          Behaviors.same
      }

    }
  }

  def getGrabberActor(ctx: ActorContext[Command], roomId: Long, liveId: String, source: InputStream, recorderRef: ActorRef[RecorderActor.Command]) = {
    val childName = s"grabberActor_$liveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(GrabberActor.create(roomId, liveId, source, recorderRef), childName)
      ctx.watchWith(actor,ChildDead4Grabber(roomId, childName, actor))
      actor
    }.unsafeUpcast[GrabberActor.Command]
  }

  def getRecorderActor(ctx: ActorContext[Command], roomId: Long, client:List[String], pushLiveId: String,  pushLiveCode: String,layout: Int,  out: OutputStream) = {
    val childName = s"recorderActor_$pushLiveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(RecorderActor.create(roomId, client, layout, out), childName)
      ctx.watchWith(actor,ChildDead4Recorder(roomId, childName, actor))
      actor
    }.unsafeUpcast[RecorderActor.Command]
  }

  def getPullPipe(ctx: ActorContext[Command], roomId: Long, liveId: String, out: OutputStream) = {
    val childName = s"pullPipeActor_$liveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(StreamPullPipe.create(roomId: Long, liveId: String, out), childName)
      ctx.watchWith(actor, ChildDead4PullPipe(liveId, childName, actor))
      actor
    }.unsafeUpcast[StreamPullPipe.Command]
  }

  def getPushPipe(ctx: ActorContext[Command], roomId: Long, pushLiveId: String, pushLiveCode: String, source: SourceChannel) = {
    val childName = s"pushPipeActor_$pushLiveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(StreamPushPipe.create(roomId, pushLiveId, pushLiveCode, source,0l), childName)
      ctx.watchWith(actor, ChildDead4PushPipe(pushLiveId, childName, actor) )
      actor
    }.unsafeUpcast[StreamPushPipe.Command]
  }





}
