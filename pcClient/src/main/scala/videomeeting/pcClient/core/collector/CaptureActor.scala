package videomeeting.pcClient.core.collector

import java.io.{File, OutputStream}
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import videomeeting.capture.core.CaptureManager
import videomeeting.capture.protocol.Messages
import videomeeting.capture.protocol.Messages._
import videomeeting.capture.sdk.{DeviceUtil, MediaCapture}
import videomeeting.pcClient.Boot
import videomeeting.pcClient.component.WarningDialog
import videomeeting.pcClient.core.stream.LiveManager
import videomeeting.pcClient.core.stream.LiveManager.{ChangeMediaOption, RecordOption}
import videomeeting.protocol.ptcl.client2Manager.websocket.AuthProtocol.HostStopPushStream2Client
import org.slf4j.LoggerFactory
//import org.bytedeco.javacv.FrameRecorder

import concurrent.duration._
import language.postfixOps

/**
  * User: TangYaruo
  * Date: 2019/9/4
  * Time: 14:04
  */
object   CaptureActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  type CaptureCommand = ReplyToCommand

  final case class StartEncode(output: Either[File, OutputStream], url: Option[String] = None) extends CaptureCommand

  final case class StopEncode(encoderType: EncoderType.Value) extends CaptureCommand

  final case object StopCapture extends CaptureCommand

  final case class GetMediaCapture(mediaCapture: MediaCapture) extends CaptureCommand

  final case class PushRtmpStream(url: String) extends CaptureCommand

  final case object StopPushRtmp extends CaptureCommand

  /*drawer*/
  sealed trait DrawCommand

  final case class DrawImage(image: Image) extends DrawCommand

  final case class SwitchMode(isJoin: Boolean, reset: () => Unit) extends DrawCommand with CaptureCommand

  final case class ReSet(reset: () => Unit, offOrOn: Boolean) extends DrawCommand

  final case object StopDraw extends DrawCommand

  private object ENCODE_RETRY_TIMER_KEY


  def create(frameRate: Int, gc: GraphicsContext, isJoin: Boolean, callBackFunc: Option[() => Unit] = None,parent: ActorRef[LiveManager.LiveCommand]): Behavior[CaptureCommand] =
    Behaviors.setup[CaptureCommand] { ctx =>
      log.info("CaptureActor is starting...")
      Behaviors.withTimers[CaptureCommand] { implicit timer =>
        idle(parent,frameRate, gc, isJoin, callBackFunc)
      }
    }

  private def idle(
    parent:ActorRef[LiveManager.LiveCommand],
    frameRate: Int,
    gc: GraphicsContext,
    isJoin: Boolean = false,
    callBackFunc: Option[() => Unit] = None,
    resetFunc: Option[() => Unit] = None,
    mediaCapture: Option[MediaCapture] = None,
    reqActor: Option[ActorRef[Messages.ReqCommand]] = None,
    loopExecutor: Option[ScheduledThreadPoolExecutor] = None,
    imageLoop: Option[ScheduledFuture[_]] = None,
    drawActor: Option[ActorRef[DrawCommand]] = None
  )(
    implicit timer: TimerScheduler[CaptureCommand]
  ): Behavior[CaptureCommand] =
    Behaviors.receive[CaptureCommand] { (ctx, msg) =>
      msg match {
        case msg: GetMediaCapture =>
          idle(parent,frameRate, gc, isJoin, callBackFunc, resetFunc, Some(msg.mediaCapture), reqActor, loopExecutor, imageLoop, drawActor)

        case msg: CaptureStartSuccess =>
          log.info(s"MediaCapture start success!")
          CaptureManager.setLatestFrame()
          val drawActor = ctx.spawn(drawer(gc, isJoin), s"CaptureDrawer-${System.currentTimeMillis()}")
          val executor = new ScheduledThreadPoolExecutor(1)
          val askImageLoop = executor.scheduleAtFixedRate(
            () => {
              msg.manager ! Messages.AskImage
            },
            0,
            ((1000.0 / frameRate) * 1000).toLong,
            TimeUnit.MICROSECONDS
          )
          callBackFunc.foreach(func => func())
          idle(parent,frameRate, gc, isJoin, callBackFunc, resetFunc, mediaCapture, Some(msg.manager), Some(executor), Some(askImageLoop), Some(drawActor))

        case msg: CannotAccessSound =>
          log.info(s"Sound unavailable.")
          Behaviors.same

        case msg: CannotAccessImage =>
          log.info(s"Image unavailable.")
          Behaviors.same

        case CaptureStartFailed =>
          log.info(s"Media capture start failed. Review your settings.")
          Behaviors.same

        case ManagerStopped =>
          log.info(s"Capture Manager stopped.")
          if (resetFunc.nonEmpty) {
            resetFunc.foreach(func => func())
            mediaCapture.foreach(_.start())
          }
          idle(parent,frameRate, gc, isJoin, callBackFunc, None, mediaCapture, reqActor, loopExecutor, imageLoop, drawActor)

        case StreamCannotBeEncoded =>
          log.info(s"Stream cannot be encoded to mpegts.")
          Behaviors.same

        case CannotSaveToFile =>
          log.info(s"Stream cannot be save to file.")
          Behaviors.same

        case msg: ImageRsp =>
          drawActor.foreach(_ ! DrawImage(msg.latestImage.image))
          Behaviors.same

        case msg: SoundRsp => //no need yet
          Behaviors.same

        case NoImage =>
//          log.info(s"No images yet, try later.")
          Behaviors.same

        case NoSamples =>
          log.info(s"No sound yet, try later.")
          Behaviors.same

        case msg: StartEncode =>
          msg.output match {
            case Right(outputStream) =>
              if (reqActor.nonEmpty) {
                reqActor.foreach(_ ! StartEncodeStream(outputStream))
              } else {
                timer.startSingleTimer(ENCODE_RETRY_TIMER_KEY, msg, 500.millis)
              }
            case Left(file) =>
              if (reqActor.nonEmpty) {
                reqActor.foreach(_ ! StartEncodeFile(file))
              } else {
                timer.startSingleTimer(ENCODE_RETRY_TIMER_KEY, msg, 500.millis)
              }
          }
          Behaviors.same

        case PushRtmpStream(url) =>
          log.info(s"send url to CaptureManager: ${url}")
          reqActor.foreach(_ ! RecordToBiliBili(url))
          Behaviors.same

        case StopPushRtmp =>
          reqActor.foreach(_ ! StopRecordToBiliBili)
          Behaviors.same

        case msg: StopEncode =>
          msg.encoderType match {
            case EncoderType.STREAM => reqActor.foreach(_ ! StopEncodeStream)
            case EncoderType.FILE => reqActor.foreach(_ ! StopEncodeFile)
          }
          Behaviors.same

        case msg: SwitchMode =>
          drawActor.foreach(_ ! msg)
          Behaviors.same

        case msg: ChangeMediaOption =>
          mediaCapture.foreach { m =>
            m.stop()
            val re = msg.re.map(DeviceUtil.parseImgResolution)
            m.setOptions(outputBitrate = msg.bit, frameRate = msg.frameRate, imageWidth = re.map(_._1), imageHeight = re.map(_._2),
              needImage = Some(msg.needImage), needSound = Some(msg.needSound))
            log.debug(s"change media settings: ${m.mediaSettings}")
            val offOrOn = msg.needImage
            drawActor.foreach(_ ! ReSet(msg.reset, offOrOn))
          }
          idle(parent,frameRate, gc, isJoin, callBackFunc, Some(msg.reset), mediaCapture, reqActor, loopExecutor, imageLoop, drawActor)

        case msg: RecordOption =>
          reqActor.foreach { req =>
            if (msg.recordOrNot) {
              req ! Messages.StartEncodeFile(new File(msg.path.get))
            } else {
              req ! Messages.StopEncodeFile
            }
          }
          idle(parent,frameRate, gc, isJoin, callBackFunc, Some(msg.reset), mediaCapture, reqActor, loopExecutor, imageLoop, drawActor)

        case StopCapture =>
          log.info(s"Media capture is stopping...")
          imageLoop.foreach(_.cancel(false))
          loopExecutor.foreach(_.shutdown())
          reqActor.foreach(_ ! StopMediaCapture)
          drawActor.foreach(_ ! StopDraw)
          Behaviors.stopped

        case EncodeException(ex) =>
          if(ex.getMessage.contains("av_interleaved_write_frame() error"))
          {
            Boot.addToPlatform{
              WarningDialog.initWarningDialog("b站端直播已停止")
              //dispatch(HostStopPushStream2Client)
            }
            parent!LiveManager.StopBili
            log.error(s"${ex.getMessage}")
          }
          Behaviors.same

        case CannotRecordToBiliBili =>
          Boot.addToPlatform{
            WarningDialog.initWarningDialog("连接到b站失败，请确认b站端是否开始直播，或者重开b站端直播")
            parent!LiveManager.StopBili
          }
          Behaviors.same

        case x =>
          log.warn(s"unknown msg in idle: $x")
          Behaviors.unhandled
      }
    }


  private def drawer(
    gc: GraphicsContext,
    isJoin: Boolean,
    needImage: Boolean = true
  ): Behavior[DrawCommand] =
    Behaviors.receive[DrawCommand] { (ctx, msg) =>
      msg match {
        case msg: DrawImage =>
          val sWidth = gc.getCanvas.getWidth
          val sHeight = gc.getCanvas.getHeight
          if (needImage) {
            if (!isJoin) {
              Boot.addToPlatform {
                gc.drawImage(msg.image, 0.0, 0.0, sWidth, sHeight)
              }
            } else {
//              Boot.addToPlatform {
//                gc.drawImage(msg.image, 0.0, sHeight / 4, sWidth / 2, sHeight / 2)
//              }
            }
          }
          Behaviors.same

        case msg: SwitchMode =>
          log.debug(s"Capture Drawer switch mode.")
          CaptureManager.setLatestFrame()
          Boot.addToPlatform (msg.reset())
          drawer(gc, msg.isJoin, needImage)

        case msg: ReSet =>
          log.info("drawer reset")
          Boot.addToPlatform(msg.reset())
          drawer(gc, isJoin, !msg.offOrOn)

        case StopDraw =>
          log.info(s"Capture Drawer stopped.")
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in drawer: $x")
          Behaviors.unhandled
      }
    }


}
