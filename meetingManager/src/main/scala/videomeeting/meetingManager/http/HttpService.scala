package videomeeting.meetingManager.http

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.Timeout

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:27 PM
  */
trait HttpService extends ServiceUtils
  with UserService
  with MeetingService
  with RtpService
  with FileService
  with ResourceService{

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler


  private val home:Route = pathPrefix("webClient"){
    pathEndOrSingleSlash{
      getFromResource("html/webClient.html")
    } ~ pc ~ statistics
  }

  private val pc =(pathPrefix("pc") & get){
    pathEndOrSingleSlash{
      getFromResource("html/webClient.html")
    }
  }

  private val statistics =(pathPrefix("statistics") & get){
    pathEndOrSingleSlash{
      getFromResource("html/statistics.html")
    }
  }

  val Routes: Route =
    ignoreTrailingSlash {
      pathPrefix("videomeeting") {
        home ~ statistics ~
        pathPrefix("meetingManager"){
          resourceRoutes ~ userRoutes ~ meetingRoutes  ~ rtpRoutes ~ file
        }
      }
    }


}
