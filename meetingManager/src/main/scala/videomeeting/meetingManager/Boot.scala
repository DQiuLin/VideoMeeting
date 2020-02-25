package videomeeting.meetingManager

import java.io.FileInputStream
import java.security.{KeyStore, SecureRandom}

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout

import scala.util.{Failure, Success}
import scala.language.postfixOps
import videomeeting.meetingManager.common.AppSettings
import videomeeting.meetingManager.core.{EmailActor, MeetingManager, RegisterActor, UserManager}
import videomeeting.meetingManager.common.AppSettings
import videomeeting.meetingManager.core.{MeetingManager, RegisterActor, UserManager}
import videomeeting.meetingManager.http.HttpService

/**
  * Author: Tao Zhang
  * Date: 4/29/2019
  * Time: 11:28 PM
  */
object Boot extends HttpService {

  import concurrent.duration._

  override implicit val system: ActorSystem = ActorSystem("meetingManager", AppSettings.config)

  override implicit val materializer: Materializer = ActorMaterializer()

  override implicit val scheduler = system.scheduler

  override implicit val timeout: Timeout = Timeout(20 seconds)

  val log: LoggingAdapter = Logging(system, getClass)

  override implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")

  val userManager = system.spawn(UserManager.create(), "userManager")

  val meetingManager = system.spawn(MeetingManager.create(), "meetingManager")

  val registerActor = system.spawn(RegisterActor.create(), "registerActor")

  val emailActor = system.spawn(EmailActor.behavior, "emailActor")

  def main(args: Array[String]): Unit = {
    val httpsBinding = Http().bindAndHandle(Routes, AppSettings.httpInterface, AppSettings.httpPort)

    httpsBinding.onComplete {
      case Success(b) ⇒
        val localAddress = b.localAddress
        println(s"Server is listening on https://${localAddress.getHostName}:${localAddress.getPort}")
      case Failure(e) ⇒
        println(s"httpsBinding failed with ${e.getMessage}")
        system.terminate()
        System.exit(-1)
    }

  }
}
