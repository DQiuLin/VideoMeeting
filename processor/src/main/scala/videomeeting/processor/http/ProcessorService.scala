package videomeeting.processor.http

import java.io.{File, FileInputStream, FileOutputStream}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import videomeeting.processor.protocol.SharedProtocol._
import videomeeting.processor.protocol.CommonErrorCode.{fileNotExistError, parseJsonError, updateRoomError}
import videomeeting.processor.utils.ServiceUtils
import videomeeting.processor.Boot.{executor, roomManager, scheduler, showStreamLog, timeout}
import io.circe.Error
import io.circe.generic.auto._
import videomeeting.processor.core_new.RoomManager
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.headers.HttpOriginRange
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import videomeeting.processor.models.MpdInfoDao
import videomeeting.protocol.ptcl.processer2Manager.Processor.{CloseRoom, CloseRoomRsp, NewConnect, NewConnectRsp, UpdateRoomInfo, UpdateRsp}
import org.slf4j.LoggerFactory
import videomeeting.processor.core_new.RoomManager
import videomeeting.processor.utils.ServiceUtils

import scala.concurrent.Future

trait ProcessorService extends ServiceUtils {

  private val log = LoggerFactory.getLogger(this.getClass)

  private def newConnect = (path("newConnect") & post) {
    entity(as[Either[Error, NewConnect]]) {
      case Right(req) =>
        log.info(s"post method $NewConnect")
        roomManager ! RoomManager.NewConnection(req.roomId, req.host, req.client, req.pushLiveId, req.pushLiveCode,req.startTime)
        complete(NewConnectRsp())
      case Left(e) =>
        complete(parseJsonError)
    }
  }

  private def closeRoom = (path("closeRoom") & post) {
    entity(as[Either[Error, CloseRoom]]) {
      case Right(req) =>
        log.info(s"post method closeRoom ${req.roomId}.")
        roomManager ! RoomManager.CloseRoom(req.roomId)
        complete(CloseRoomRsp())

      case Left(e) =>
        complete(parseJsonError)
    }
  }

  private def updateRoomInfo = (path("updateRoomInfo") & post) {
    entity(as[Either[Error, UpdateRoomInfo]]) {
      case Right(req) =>
        log.info(s"post method updateRoomInfo.")
        roomManager ! RoomManager.UpdateRoomInfo(req.roomId, req.layout)
        complete(UpdateRsp())

      case Left(e) =>
        complete(parseJsonError)
    }
  }

  def tempDestination(fileInfo: FileInfo): File =
    File.createTempFile(fileInfo.fileName, ".tmp")

  def createNewFile(file:File, name:String): Boolean = {
    val fis =new FileInputStream(file)
    val picFile = new File("D:\\image\\"+ name)
    picFile.createNewFile()
    val fos = new FileOutputStream(picFile)
    var byteRead = 0
    val bytes = new Array[Byte](1024)
    byteRead = fis.read(bytes, 0, bytes.length)
    while(byteRead != -1){
      fos.write(bytes, 0, byteRead)
      byteRead = fis.read(bytes, 0, bytes.length)
    }
    fos.flush()
    fos.close()
    fis.close()
    file.delete()
  }

  private val upLoadImg = (path("upLoadImg") & post) {
    storeUploadedFile("imgFile", tempDestination) {
      case (metadata, file) =>
        createNewFile(file, metadata.fileName)
        complete(UploadSuccessRsp(metadata.fileName))
    }
  }

  private val streamLog  = (path("streamLog") & get){
    showStreamLog = !showStreamLog
    complete(showStreamLog)
  }
//
//  val processorRoute:Route = pathPrefix("processor") {
//    updateRoomInfo ~ closeRoom ~ getMpd ~ getRtmpUrl ~ getDash ~ getMpd4Record ~ getRecordList ~ upLoadImg ~ streamLog
//  }

  val processorRoute:Route = pathPrefix("processor") {
   newConnect  ~ closeRoom ~ updateRoomInfo  ~ upLoadImg ~ streamLog
  }
}
