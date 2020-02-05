package videomeeting.meetingManager.http

import videomeeting.meetingManager.Boot._

import scala.language.postfixOps
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import videomeeting.meetingManager.Boot.{executor, scheduler}
import videomeeting.protocol.ptcl.CommonRsp
import videomeeting.meetingManager.http.SessionBase._
import videomeeting.meetingManager.core.{MeetingManager, UserManager}
import videomeeting.meetingManager.utils.RtpClient

import scala.concurrent.Future

trait RtpService extends ServiceUtils{
  import io.circe._
  import io.circe.syntax._
  import io.circe.generic.auto._


  case class GetLiveInfoReq()
  private val getLiveInfo = (path("getLiveInfo") & post){
    dealPostReq[GetLiveInfoReq]{req =>
      RtpClient.getLiveInfoFunc().map{
        case Right(rsp) =>
          log.debug(s"获取liveInfo  ..${rsp}")
          complete(CommonRsp(1000023, s"获取live info失败："))
        case Left(error) =>
          complete(CommonRsp(1000023, s"获取live info失败：${error}"))

      }.recover{
        case e:Exception =>
          complete(CommonRsp(1000024, s"获取live info失败：${e}"))
      }
    }
  }

  val rtpRoutes: Route = pathPrefix("rtp") {
    getLiveInfo
  }
}

