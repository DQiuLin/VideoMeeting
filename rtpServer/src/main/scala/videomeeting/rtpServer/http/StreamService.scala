package videomeeting.rtpServer.http

import akka.http.scaladsl.server.Directives.{path, pathPrefix}
import io.circe.Error
import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.headers.{CacheDirectives, Expires, `Cache-Control`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import videomeeting.rtpServer.Boot._
import videomeeting.rtpServer.core.StreamManager.GetAllStream
import videomeeting.rtpServer.protocol.StreamServiceProtocol.{GetAllStreamRsp, GetSubscribers, StreamInfo}
import akka.actor.typed.scaladsl.AskPattern._
import videomeeting.rtpServer.core.{PublishManager, StreamManager}
import videomeeting.rtpServer.protocol.{ErrorRsp, SuccessRsp}
import videomeeting.rtpServer.ptcl.protocol.Address
import scala.concurrent.duration._
import scala.concurrent.Future

/**
  * Author: wqf
  * Date: 2019/8/25
  * Time: 12:38
  */
trait StreamService extends BaseService{

  private val getAllStream = (path("getAllStream") & get & pathEndOrSingleSlash){
    val msg: Future[List[StreamInfo]] = streamManager ? GetAllStream
    dealFutureResult{
      msg.map{streamInfo =>
        if(streamInfo.nonEmpty){
          complete(GetAllStreamRsp(Some(streamInfo)))
        }else{
          complete(GetAllStreamRsp(None))
        }
      }
    }
  }


  private val getSubscribers = (path("getSubscribers") & get & pathEndOrSingleSlash){
    parameters('liveId.as[String])
    { liveId =>
      val msg: Future[List[Address]] = publishManager ? (PublishManager.GetSubscribers(liveId, _))
      //val msg =  publishManager.ask[List[Address]](PublishManager.GetSubscribers(liveId, _))(2000)
      dealFutureResult{
        msg.map{address =>
          if(address.nonEmpty){
            complete(GetSubscribers(Some(address)))
          }else{
            complete(GetSubscribers(None))
          }
        }
      }
    }
  }

  private val StopStream = (path("stopStream") & get & pathEndOrSingleSlash){
    parameters('liveId.as[String]){liveId =>
      val msg: Future[Boolean] = streamManager ? (StreamManager.StopStream(liveId, _))
      dealFutureResult{
        msg.map{m =>
          if(m){
            complete(SuccessRsp())
          }else{
            complete(ErrorRsp(10000, "该流不存在"))
          }
        }
      }

    }
  }

  val streamRoutes =
    pathPrefix("stream"){
      getAllStream ~getSubscribers ~StopStream
  }
}
