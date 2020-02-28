package videomeeting.meetingManager.http

import java.io.File

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.ContentNegotiator.Alternative.ContentType

import scala.language.postfixOps
import videomeeting.meetingManager.utils.FileFilter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import videomeeting.protocol.ptcl.CommonRsp
import videomeeting.meetingManager.Boot.{executor, materializer, meetingManager, scheduler, timeout}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol._
import videomeeting.meetingManager.common.AppSettings
import videomeeting.meetingManager.core.MeetingManager.UserInfoChange
import videomeeting.meetingManager.utils.{MacFileFilter, WinFileFilter}
import videomeeting.meetingManager.utils.HestiaClient
import org.slf4j.LoggerFactory
import videomeeting.meetingManager.common.AppSettings
import videomeeting.meetingManager.models.dao.UserInfoDao
import videomeeting.meetingManager.utils.{HestiaClient, MacFileFilter, WinFileFilter}

import scala.util.{Failure, Success}
/**
  * created by benyafang on 2019.8.19 14:43
  * */
trait FileService extends ServiceUtils{
  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

  private def storeFile(source: Source[ByteString, Any]): Directive1[java.io.File] = {
    val dest = java.io.File.createTempFile("videomeeting", ".tmp")
    val file = source.runWith(FileIO.toPath(dest.toPath)).map(_ => dest)
    onComplete[java.io.File](file).flatMap {
      case Success(f) =>
        provide(f)
      case Failure(e) =>
        dest.deleteOnExit()
        failWith(e)
    }
  }

  val uploadImg = (path("uploadFile") & post){
//    authUser { _ =>
      parameter('userId.as[Int]) { userId =>
        fileUpload("fileUpload") {
          case (fileInfo, file1) =>
            storeFile(file1) { f =>
              //            dealFutureResult{//fixme上传之前先删除之前存的照片
              //              UserInfoDao.SearchById(userId).map{t =>
              //                  if(t.nonEmpty){
              //                    if(t.get.headImg != "" && imgType == CommonInfo.ImgType.headImg)HestiaClient.deleteImg(t.get.headImg)
              //                    else if(t.get.coverImg != "" && imgType == CommonInfo.ImgType.coverImg)HestiaClient.deleteImg(t.get.coverImg)
              dealFutureResult {
                HestiaClient.upload(f, fileInfo.fileName).map {
                  case Right(url) =>
                    f.deleteOnExit()
                    dealFutureResult {
                      UserInfoDao.modifyImg4User(userId, url).map { r =>
                        meetingManager ! UserInfoChange(userId, false)
                        complete(ImgChangeRsp(url))
                      }.recover {
                        case e: Exception =>
                          log.debug(s"modify img error:$e")
                          complete(ImgChangeRsp(url))
                      }
                    }
                  case Left(error) =>
                    f.deleteOnExit()
                    log.debug(s"upload img error:$error")
                    complete(ImgChangeRspDecodeError)
                }
              }

              //                  }else{
              //                    complete(NoUserError)
              //                  }

              //              }.recover{
              //                case e:Exception =>
              //                  log.debug(s"search user error")
              //                  complete(ImgChangeRspInternalError)
              //              }

              //            }
            }
        }
      }
//    }
  }

//  val listClientFileName = (path("listClientFileName") & get){
//    val files = new File(s"${AppSettings.clientPath}")
//    val macFilter = new MacFileFilter()
//    val a = files.listFiles(macFilter)
//    files.listFiles(new WinFileFilter())
//    complete(ListClientFiles(Some(ClientInfo(files.listFiles(new WinFileFilter()).toList.map(_.getName),files.listFiles(macFilter).toList.map(_.getName)))))
//  }

  val downloadZip = (path("download" / Remaining)){(filename) =>
    val file = new File(s"${AppSettings.clientPath}"+filename)
    log.debug(s"下载文件路径：${file.getAbsolutePath}路径")
    if(file.exists()){
      val responseEntity = HttpEntity(
        ContentTypes.`application/octet-stream`,
        file.length(),
        FileIO.fromPath(file.toPath,chunkSize = 262144)
      )
      complete(responseEntity)
    }else{
      complete(CommonRsp(100045,"下载失败，文件不存在"))
    }
  }

  val file = pathPrefix("file"){
    uploadImg ~ downloadZip //~ listClientFileName
  }

}
