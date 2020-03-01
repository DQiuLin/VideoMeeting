package videomeeting.processor.core_new

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{File, FileOutputStream, OutputStream}
import java.nio.{ByteBuffer, ShortBuffer}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.ffmpeg.global.{avcodec, avutil}
import org.bytedeco.javacv.{FFmpegFrameFilter, FFmpegFrameRecorder, Frame, Java2DFrameConverter}
import videomeeting.processor.Boot.roomManager
import videomeeting.processor.common.AppSettings.{addTs, bitRate, debugPath, isDebug}
import org.slf4j.LoggerFactory
import videomeeting.processor.utils.TimeUtil

import scala.collection.mutable
import scala.concurrent.duration._


/**
  * Created by sky
  * Date on 2019/10/22
  * Time at 下午2:30
  *
  * actor由RoomActor创建
  * 编码线程 stream数据传入pipe
  * 合并连线线程
  */
object RecorderActor {

  var audioChannels = 0 //todo 待议
  val sampleFormat = 1 //todo 待议
  var frameRate = 30

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case object Init extends Command

  case object RestartRecord extends Command

  case object StopRecorder extends Command

  case object CloseRecorder extends Command

  case class ForceExit4Client(liveId: String) extends Command

  case class NewFrame(liveId: String, frame: Frame) extends Command

  case class UpdateRecorder(channel: Int, sampleRate: Int, frameRate: Double, width: Int, height: Int, liveId: String) extends Command

  case object TimerKey4Close

  sealed trait VideoCommand

  case class TimeOut(msg: String) extends Command

  case class Image4Host(frame: Frame) extends VideoCommand

  case class Image4Client(frame: Frame, liveId: String) extends VideoCommand

  case object StartDrawing extends VideoCommand

  case class ReStartDrawing(clientInfo: List[String], exitLiveId: String) extends VideoCommand

  case class SetLayout(layout: Int) extends VideoCommand

  case class NewRecord4Ts(recorder4ts: FFmpegFrameRecorder) extends VideoCommand

  case object Close extends VideoCommand

  case class Ts4Host(var time: Long = 0)

  case class Ts4Client(var time: Long = 0)

  case class Image(var frame: Frame = null)

  case class Ts4LastImage(var time: Long = -1)

  case class Ts4LastSample(var time: Long = 0)

  private val emptyAudio = ShortBuffer.allocate(1024 * 2)
  private val emptyAudio4one = ShortBuffer.allocate(1152)

  def create(roomId: Long, client: List[String], layout: Int, output: OutputStream): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"recorderActor start----")
          audioChannels=client.size+1
          avutil.av_log_set_level(-8)
          val recorder4ts = new FFmpegFrameRecorder(output, 640, 480, audioChannels)
          recorder4ts.setFrameRate(frameRate)
          recorder4ts.setVideoBitrate(bitRate)
          recorder4ts.setVideoCodec(avcodec.AV_CODEC_ID_MPEG2VIDEO)
          recorder4ts.setAudioCodec(avcodec.AV_CODEC_ID_MP2)
          recorder4ts.setMaxBFrames(0)
          recorder4ts.setFormat("mpegts")
          try {
            recorder4ts.startUnsafe()
          } catch {
            case e: Exception =>
              log.error(s" recorder meet error when start:$e")
          }
          roomManager ! RoomManager.RecorderRef(roomId, ctx.self)
          ctx.self ! Init
          single(roomId,client, layout, recorder4ts, null, null, null,  null,null, mutable.Map[String, BufferedImage](), output, 30000, (0, 0))
      }
    }
  }

  def single(roomId: Long, client: List[String], layout: Int,
             recorder4ts: FFmpegFrameRecorder,
             ffFilter: FFmpegFrameFilter,
             drawer: ActorRef[VideoCommand],
             ts4Host: Ts4Host,
             ts4Client: mutable.Map[String,Ts4Client],
             hostImage: BufferedImage,
             clientImage: mutable.Map[String, BufferedImage],
             out: OutputStream,
             tsDiffer: Int = 30000, canvasSize: (Int, Int))(implicit timer: TimerScheduler[Command],
                                                            stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case Init =>
          if (ffFilter != null) {
            ffFilter.close()
          }
          val ffFilterN = if(client.size == 1){
            new FFmpegFrameFilter(s"[0:a][1:a] amix=inputs=2:duration=longest:dropout_transition=3:weights=1 1[a]", audioChannels)
          } else if(client.size == 2){
            new FFmpegFrameFilter(s"[0:a][1:a][2:a] amix=inputs=3:duration=longest:dropout_transition=3:weights=1 1[a]", audioChannels)
          } else if( client.size == 3){
            new FFmpegFrameFilter(s"[0:a][1:a][2:a][3:a] amix=inputs=4:duration=longest:dropout_transition=3:weights=1 1[a]", audioChannels)
          } else {
            new FFmpegFrameFilter(s"[0:a][1:a] amix=inputs=2:duration=longest:dropout_transition=3:weights=1 1[a]", audioChannels)
          }
          //val ffFilterN = new FFmpegFrameFilter("[0:a][1:a] amix=inputs=2:duration=longest:dropout_transition=3:weights=1 1[a]", audioChannels)
          ffFilterN.setAudioChannels(audioChannels)
          ffFilterN.setSampleFormat(sampleFormat)
          ffFilterN.setAudioInputs(client.size + 1)
          ffFilterN.start()
          single(roomId,client,layout, recorder4ts, ffFilterN, drawer, ts4Host, ts4Client, hostImage, clientImage, out, tsDiffer, canvasSize)

        case UpdateRecorder(channel, sampleRate, f, width, height, liveId) =>

            log.info(s"$roomId updateRecorder channel:$channel, sampleRate:$sampleRate, frameRate:$f, width:$width, height:$height")
            recorder4ts.setFrameRate(f)
            recorder4ts.setAudioChannels(channel)
            recorder4ts.setSampleRate(sampleRate)
            ffFilter.setAudioChannels(channel)
            ffFilter.setSampleRate(sampleRate)
            recorder4ts.setImageWidth(width)
            recorder4ts.setImageHeight(height)
            single(roomId, client, layout, recorder4ts, ffFilter, drawer, ts4Host, ts4Client,hostImage, clientImage, out, tsDiffer,  (640,  480))


        case NewFrame(liveId, frame) =>
            val canvas = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR)
            val drawer = ctx.spawn(draw(canvas, canvas.getGraphics, Ts4LastImage(), hostImage, clientImage, client, recorder4ts,
              new Java2DFrameConverter(), new Java2DFrameConverter, layout, "defaultImg.jpg", roomId, (640, 480)), s"drawer_$roomId")
            ctx.self ! NewFrame(liveId, frame)
            work(roomId,client,0,None,recorder4ts,ffFilter, drawer,ts4Host,ts4Client,out,tsDiffer,canvasSize)


        case CloseRecorder =>
          try {
            if (out != null)
              out.close()
          } catch {
            case e: Exception =>
              log.info(s"pipeStream has already been closed.")
          }
          try {
            ffFilter.close()
            drawer ! Close
          } catch {
            case e: Exception =>
              log.error(s"$roomId recorder close error ---")
          }
          Behaviors.stopped

        case StopRecorder =>
          timer.startSingleTimer(TimerKey4Close, CloseRecorder, 1.seconds)
          Behaviors.same
      }
    }
  }

  def work(roomId: Long, client: List[String], num: Int = 0,
           audioMap:Option[mutable.Map[String, Int]],
           recorder4ts: FFmpegFrameRecorder,
           ffFilter: FFmpegFrameFilter,
           drawer: ActorRef[VideoCommand],
           ts4Host: Ts4Host,
           ts4Client: mutable.Map[String,Ts4Client],
           out: OutputStream,
           tsDiffer: Int = 30000, canvasSize: (Int, Int))
          (implicit timer: TimerScheduler[Command],
           stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    log.info(s"$roomId recorder to couple behavior")
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case NewFrame(liveId, frame) =>
          var newNum = num
          val newMap = audioMap.getOrElse(mutable.Map[String, Int]())
          if (frame.image != null) {
             drawer ! Image4Client(frame,liveId)
          }
          if (frame.samples != null) {
            try {
              if(audioMap.isDefined){
                if(audioMap.get.contains(liveId)){
                  ffFilter.pushSamples(audioMap.get(liveId), frame.audioChannels, frame.sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
                }else{
                  ffFilter.pushSamples(num, frame.audioChannels, frame.sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
                  newMap.put(liveId,num)
                  newNum = num+1
                }
              }
              val f = ffFilter.pullSamples().clone()
              if (f != null) {
                recorder4ts.recordSamples(f.sampleRate, f.audioChannels, f.samples: _*)
              }
            } catch {
              case ex: Exception =>
                log.debug(s"$liveId record sample error system: $ex")
            }
          }
          log.info(s"output stream${out}")
          work(roomId, client, newNum,Some(newMap), recorder4ts, ffFilter, drawer, ts4Host, ts4Client, out, tsDiffer, canvasSize)

//        case msg: UpdateRoomInfo =>
//          log.info(s"$roomId got msg: $msg in work.")
//          if (msg.layout != num) {
//            drawer ! SetLayout(msg.layout)
//          }
//          ctx.self ! RestartRecord
//          work(roomId, client, msg.layout, recorder4ts, ffFilter, drawer, ts4Host, ts4Client, out, tsDiffer, canvasSize)

        case m@RestartRecord =>
          log.info(s"couple state get $m")
          Behaviors.same

        case CloseRecorder =>
          try {
            if (out != null)
              out.close()
          } catch {
            case e: Exception =>
              log.info(s"pipeStream has already been closed.")
          }
          try {
            ffFilter.close()
            drawer ! Close
          } catch {
            case e: Exception =>
              log.error(s"$roomId recorder close error ---")
          }
          Behaviors.stopped

        case StopRecorder =>
          timer.startSingleTimer(TimerKey4Close, CloseRecorder, 1.seconds)
          Behaviors.same

        case msg: ForceExit4Client =>
          log.info(s"${ctx.self} receive a msg $msg")
          val newClient = client.filter(c => c != msg.liveId)
          audioMap.get.remove(msg.liveId)
          drawer ! ReStartDrawing(newClient, msg.liveId)
          work(roomId, newClient, num,null, recorder4ts, ffFilter, drawer, ts4Host, ts4Client, out, tsDiffer, canvasSize)

        case x =>
          Behaviors.same
      }
    }
  }

  def draw(canvas: BufferedImage, graph: Graphics, lastTime: Ts4LastImage, hostFrame: BufferedImage, clientFrame: mutable.Map[String, BufferedImage], clientInfo: List[String],
           recorder4ts: FFmpegFrameRecorder, convert4Host: Java2DFrameConverter, convert: Java2DFrameConverter,
    num: Int = 0, bgImg: String, roomId: Long, canvasSize: (Int, Int)): Behavior[VideoCommand] = {
    Behaviors.setup[VideoCommand] { ctx =>
      Behaviors.receiveMessage[VideoCommand] {
        case t: Image4Host =>
          val img = convert4Host.convert(t.frame)
          ctx.self ! StartDrawing
          draw(canvas, graph, lastTime, img, clientFrame, clientInfo, recorder4ts, convert4Host, convert, num, bgImg, roomId, canvasSize)

        case t: Image4Client =>
          clientInfo.foreach { client =>
            if (client == t.liveId) {
              val clientConvert = new Java2DFrameConverter()
              val clientImg = clientConvert.convert(t.frame)
              clientFrame.put(client, clientImg)
              log.info(s"=========putImage ${client}")
            }
          }
          ctx.self ! StartDrawing
//          if (clientInfo.size == clientFrame.values.toList.size) {
//            clientInfo.size match {
//              case 0 =>
//                graph.drawImage(clientFrame.values.toList.head, 0, 0, canvasSize._1, canvasSize._2, null)
//              //                graph.drawString("主持人", 24, 25)
//              case 1 =>
//                log.info(s"======== Listsize ${clientFrame.values.toList.size}")
//                graph.drawImage(clientFrame.values.toList.head, 0, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                //                graph.drawString("主持人", 24, 25)
//                graph.drawImage(clientFrame.values.toList(1), canvasSize._1 / 2, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                //                graph.drawString("参会人1", 344, 25)
//                log.info(s"two people =============================${canvasSize._1}    ${canvasSize._2}")
//              case 2 =>
//                graph.drawImage(clientFrame.values.head, 0,canvasSize._2 / 4, canvasSize._1 / 3, canvasSize._2 / 2, null)
//                //                graph.drawString("主持人", 310, 0)
//                graph.drawImage(clientFrame.values.toList(1), canvasSize._1 / 3, canvasSize._2 / 2, canvasSize._1 / 3, canvasSize._2 / 2, null)
//                //                graph.drawString("参会人1", 150, 250)
//                graph.drawImage(clientFrame.values.toList(2),canvasSize._1 / 3, canvasSize._2 / 2, canvasSize._1 / 3, canvasSize._2 / 2, null)
//              //                graph.drawString("参会人2", 470, 250)
//              case 3 =>
//                graph.drawImage(clientFrame.values.head, 0, 0, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                //                graph.drawString("主持人", 150, 0)
//                graph.drawImage(clientFrame.values.toList(1), canvasSize._1 / 2,0, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                //                graph.drawString("参会人1", 470, 0)
//                graph.drawImage(clientFrame.values.toList(2), 0, canvasSize._2 / 2, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                //                graph.drawString("参会人2", 150, 250)
//                graph.drawImage(clientFrame.values.toList(3), canvasSize._1 / 2, canvasSize._2 / 2, canvasSize._1 / 2, canvasSize._2 / 2, null)
//              //                graph.drawString("参会人3", 470, 25)
//
//            }
//          } else {
//            log.info(s"${ctx.self} is waiting to drawing")
//          }
//
//          val frame = convert.convert(canvas)
//          recorder4ts.record(frame.clone())
          Behaviors.same

        case StartDrawing =>
          //根据不同的参会人数设置不同的排列方式
          if (clientInfo.size == clientFrame.values.toList.size) {
            clientInfo.size match {
              case 0 =>
                graph.drawImage(clientFrame.values.toList.head, 0, 0, canvasSize._1, canvasSize._2, null)
//                graph.drawString("主持人", 24, 25)
              case 1 =>
                graph.drawImage(clientFrame.values.toList.head, 0, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                graph.drawString("主持人", 24, 25)
                graph.drawImage(clientFrame.values.toList(1), canvasSize._1 / 2, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                graph.drawString("参会人1", 344, 25)
                log.info(s"two people =============================${canvasSize._1}    ${canvasSize._2}")
              case 2 =>
                graph.drawImage(clientFrame.values.head, 0,canvasSize._2 / 4, canvasSize._1 / 3, canvasSize._2 / 2, null)
//                graph.drawString("主持人", 310, 0)
                graph.drawImage(clientFrame.values.toList(1), canvasSize._1 / 3, canvasSize._2 / 2, canvasSize._1 / 3, canvasSize._2 / 2, null)
//                graph.drawString("参会人1", 150, 250)
                graph.drawImage(clientFrame.values.toList(2),canvasSize._1 / 3, canvasSize._2 / 2, canvasSize._1 / 3, canvasSize._2 / 2, null)
//                graph.drawString("参会人2", 470, 250)
              case 3 =>
                graph.drawImage(clientFrame.values.head, 0, 0, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                graph.drawString("主持人", 150, 0)
                graph.drawImage(clientFrame.values.toList(1), canvasSize._1 / 2,0, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                graph.drawString("参会人1", 470, 0)
                graph.drawImage(clientFrame.values.toList(2), 0, canvasSize._2 / 2, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                graph.drawString("参会人2", 150, 250)
                graph.drawImage(clientFrame.values.toList(3), canvasSize._1 / 2, canvasSize._2 / 2, canvasSize._1 / 2, canvasSize._2 / 2, null)
//                graph.drawString("参会人3", 470, 25)

            }
          } else {
            log.info(s"${ctx.self} is waiting to drawing")
          }

          val frame = convert.convert(canvas)
          recorder4ts.record(frame.clone())
          Behaviors.same

        case t: ReStartDrawing =>
          ctx.self ! StartDrawing
          clientFrame.remove(t.exitLiveId)
          graph.clearRect(0, 0, canvasSize._1, canvasSize._2)
          draw(canvas, graph, lastTime, hostFrame, clientFrame, t.clientInfo, recorder4ts, convert4Host, convert, num, bgImg, roomId, canvasSize)


        case Close =>
          log.info(s"drawer stopped")
          recorder4ts.releaseUnsafe()
          Behaviors.stopped

        //        case m@NewRecord4Ts(recorder4ts) =>
        //          log.info(s"got msg: $m")
        //          draw(canvas, graph, lastTime, clientFrame, recorder4ts, convert1, convert2,convert, layout, bgImg, roomId, canvasSize)
        //
        //        case t: SetLayout =>
        //          log.info(s"got msg: $t")
        //          draw(canvas, graph, lastTime, clientFrame, recorder4ts, convert1, convert2,convert, t.layout, bgImg, roomId, canvasSize)
      }
    }
  }

}
