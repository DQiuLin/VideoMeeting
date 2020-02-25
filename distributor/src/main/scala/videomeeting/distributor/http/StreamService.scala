package videomeeting.distributor.http

import akka.http.scaladsl.server.Directives.{as, complete, entity, path, pathPrefix, _}
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import io.circe.Error
import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import videomeeting.distributor.core.LiveManager
import videomeeting.distributor.protocol.CommonErrorCode._
import akka.actor.typed.scaladsl.AskPattern._
import videomeeting.distributor.Boot.{executor, liveManager, scheduler, timeout}
import videomeeting.distributor.core.LiveManager.{liveStop, updateRoom}
import videomeeting.distributor.protocol.SharedProtocol._
import videomeeting.distributor.utils.ServiceUtils
import videomeeting.protocol.ptcl.distributor2Manager.DistributorProtocol.GetAllLiveInfoReq


import scala.concurrent.Future

trait StreamService extends ServiceUtils {

  private val log = LoggerFactory.getLogger(this.getClass)


  private val settings = CorsSettings.defaultSettings.withAllowedOrigins(
    HttpOriginMatcher.*
  )

  val newLive: Route = (path("startPull") & post) {
    entity(as[Either[Error, StartPullReq]]) {
      case Right(req) =>
        log.info(s"post method newLiveInfo.")
        val startTime = System.currentTimeMillis()
        liveManager ! updateRoom(req.roomId, req.liveId, startTime)
        val addr = s"/theia/distributor/getFile/${req.roomId}/index.mpd"
        complete(StartPullRsp(0,s"got liveId${req.liveId}",liveAdd = addr, startTime))

      case Left(e) =>
        complete(parseJsonError)
    }
  }

  val stopLive: Route = (path("finishPull") & post) {
    entity(as[Either[Error, FinishPullReq]]) {
      case Right(req) =>
        log.info(s"post method stopLiveInfo.")
        liveManager ! liveStop(req.liveId)
        complete(FinishPullRsp())

      case Left(e) =>
        complete(parseJsonError)
    }
  }

  val checkLive: Route = (path("checkStream") & post) {
    entity(as[Either[Error, CheckStreamReq]]) {
      case Right(req) =>
        log.info(s"post method checkLiveInfo.")
        val rst : Future[CheckStreamRsp] = liveManager ? (LiveManager.CheckLive(req.liveId, _))
        dealFutureResult(rst.map
          (rsp=>
            complete(rsp)
          )
        )

      case Left(e) =>
        complete(parseJsonError)
    }
  }

  val getAllLiveInfo: Route = (path("getAllLiveInfo") & post) {
    entity(as[Either[Error, GetAllLiveInfoReq]]) {
      case Right(_) =>
//        log.info(s"post method getAllLiveInfo.")
        dealFutureResult {
          val rst = liveManager ? LiveManager.GetAllLiveInfo
          rst.map {
            rsp =>
              complete(rsp)
          }
        }

      case Left(_) =>
        complete(parseJsonError)
    }
  }





  val streamRoute: Route = pathPrefix("admin") {
    getAllLiveInfo
  } ~ newLive ~ stopLive ~ checkLive
}
