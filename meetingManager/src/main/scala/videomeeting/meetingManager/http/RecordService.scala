package videomeeting.meetingManager.http

import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol._
import videomeeting.meetingManager.Boot._
import videomeeting.meetingManager.core.UserManager.{log => _, _}

import scala.language.postfixOps
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import videomeeting.meetingManager.Boot.{executor, meetingManager, scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import videomeeting.meetingManager.core.MeetingManager.GetMeetingList

import scala.concurrent.Future

/**
 * File: RecordService.scala
 * Name: 张袁峰
 * Student ID: 16301170
 * date: 2020/1/23
 */
trait RecordService extends ServiceUtils {

  import io.circe._
  import io.circe.syntax._
  import io.circe.generic.auto._

  private val getMeetingInfo = (path("info") & get) {
    parameter(
      'mid.as[Int]
    ) { mid =>
      val roomListFutureRsp: Future[MeetingListRsp] = meetingManager ? (GetMeetingList(_))
      dealFutureResult(
        roomListFutureRsp.map(rsp => complete(rsp))
      )
    }
  }

  val recordRoutes: Route = pathPrefix("record") {
    getMeetingInfo
  }

}
