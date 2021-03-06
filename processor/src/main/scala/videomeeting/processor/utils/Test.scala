package videomeeting.processor.utils

import java.io.File

import videomeeting.processor.Boot.executor
import videomeeting.processor.protocol.SharedProtocol.CloseRoom
import SecureUtil.genPostEnvelope
import org.slf4j.LoggerFactory

import scala.concurrent.Future
/**
  * created by byf on 2019.7.17 13:09
  * */
object ProcessorClient extends HttpUtil{

  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._
  case class UpdateRoom(
                         roomId:Long,
                         liveIdList:List[String], //主播放在第一个
                         startTime:Long,//视频开始时间，
                         layout:Int,
                         aiMode:Int //为了之后拓展多种模式，目前0为不开ai，1为人脸目标检测
                       )

  case class RoomInfo(roomId:Long, roles:List[String], layout:Int, aiMode:Int=0)

  case class CloseRoom(
                        roomId:Long
                      )

  case class GetMpd(
                     roomId:Long
                   )

  case class GetRtmpUrl(
                         roomId:Long
                       )

  case class GetMpd4Record(
                            roomId:Long,
                            startTime:Long
                          )

  trait CommonRsp {
    val errCode: Int
    val msg: String
  }

  case class MpdRsp(
                     mpd: String,
                     rtmp:String,
                     errCode: Int = 0,
                     msg: String = "ok"
                   ) extends CommonRsp

  case class RtmpRsp(
                      mpd: String,
                      errCode: Int = 0,
                      msg: String = "ok"
                    ) extends CommonRsp

  final case class ErrorRsp(
                             errCode: Int,
                             msg: String
                           ) extends CommonRsp

  final case class SuccessRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends CommonRsp

  private val log = LoggerFactory.getLogger(this.getClass)

  val processorBaseUrl = "http://10.1.29.246:41660/theia/processor"


  def updateRoomInfo(roomId:Long,liveIdList:List[String],layout:Int,aiMode:Int):Future[Either[String,SuccessRsp]] = {
    val url = processorBaseUrl + "/updateRoomInfo"
    val jsonString = UpdateRoom(roomId,liveIdList,0l,layout,aiMode).asJson.noSpaces
    val jsonData = genPostEnvelope("",System.nanoTime().toString,jsonString,"").asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonString,timeOut = 60 * 1000,needLogRsp = false).map{
      case Right(v) =>
        decode[SuccessRsp](v) match{
          case Right(data) =>
            Right(data)
          case Left(e) =>
            log.error(s"updateRoomInfo decode error : $e")
            Left("Error")
        }
      case Left(error) =>
        log.error(s"updateRoomInfo postJsonRequestSend error : $error")
        Left("Error")
    }
  }

  def closeRoom(roomId:Long):Future[Either[String,SuccessRsp]] = {
    val url = processorBaseUrl + "/updateRoomInfo"
    val jsonString = CloseRoom(roomId).asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonString,timeOut = 60 * 1000).map{
      case Right(v) =>
        decode[SuccessRsp](v) match{
          case Right(data) =>
            Right(data)
          case Left(e) =>
            log.error(s"updateRoomInfo decode error : $e")
            Left("Error")
        }
      case Left(error) =>
        log.error(s"updateRoomInfo postJsonRequestSend error : $error")
        Left("Error")
    }
  }

  def getmpd(roomId:Long):Future[Either[String,MpdRsp]] = {
    val url = processorBaseUrl + "/getMpd"
    val jsonString = GetMpd(roomId).asJson.noSpaces
    //    val jsonData = genPostEnvelope("",System.nanoTime().toString,jsonString,"").asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonString,timeOut = 60 * 1000, needLogRsp = false).map{
      case Right(v) =>
        decode[MpdRsp](v) match{
          case Right(data) =>
            Right(data)
          case Left(e) =>
            log.error(s"getmpd decode error : $e")
            Left("Error")
        }
      case Left(error) =>
        log.error(s"getmpd postJsonRequestSend error : $error")
        Left("Error")
    }
  }

  def getRtmp(roomId:Long):Future[Either[String,RtmpRsp]] = {
    val url = processorBaseUrl + "/getRtmpUrl"
    val jsonString = GetRtmpUrl(roomId).asJson.noSpaces
    //    val jsonData = genPostEnvelope("",System.nanoTime().toString,jsonString,"").asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonString,timeOut = 60 * 1000, needLogRsp = false).map{
      case Right(v) =>
        decode[RtmpRsp](v) match{
          case Right(data) =>
            Right(data)
          case Left(e) =>
            log.error(s"getmpd decode error : $e")
            Left("Error")
        }
      case Left(error) =>
        log.error(s"getmpd postJsonRequestSend error : $error")
        Left("Error")
    }
  }

  def deleteFile():Unit = {
    val f = new File("/Users/litianyu/Downloads/test")
    if(f.exists()){
      f.listFiles().map{
        e =>
          e.delete()
      }
      f.delete()
    }
  }

  def main(args: Array[String]): Unit = {

    deleteFile()
    updateRoomInfo(8888,List("liveIdTest-1111"),1,0).map{
      a=>
        println(a)
    }
//
//    Thread.sleep(30000)
//
//    closeRoom(8888).map{
//      a =>
//        println(a)
//    }
//    updateRoomInfo(8888,List("1000","2000"),2,0).map{
//      a=>
//        println(a)
//    }
  }


}
