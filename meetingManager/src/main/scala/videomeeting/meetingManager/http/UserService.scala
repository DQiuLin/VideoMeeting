package videomeeting.meetingManager.http

import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol._
import videomeeting.meetingManager.Boot._
import videomeeting.meetingManager.core.UserManager.{log => _, _}

import scala.language.postfixOps
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import videomeeting.meetingManager.Boot.{executor, meetingManager, scheduler, userManager}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import videomeeting.protocol.ptcl.{CommonRsp, Response}
import videomeeting.meetingManager.core.RegisterActor.RegisterReq
import videomeeting.meetingManager.core.MeetingManager
import videomeeting.meetingManager.models.dao.{MeetingDao, UserInfoDao}
import videomeeting.protocol.ptcl.CommonInfo.{MeetInfo, MeetingInfo, PeopleInfo, User, UserInfo}
import videomeeting.meetingManager.common.{AppSettings, Common}
import videomeeting.meetingManager.core.MeetingManager.{GetMeetingList, UserInfoChange}
import videomeeting.meetingManager.http.SessionBase.UserSession
import videomeeting.meetingManager.utils.SecureUtil
import videomeeting.meetingManager.common.{AppSettings, Common}
import videomeeting.meetingManager.core.MeetingManager
import videomeeting.meetingManager.models.dao.UserInfoDao
import videomeeting.meetingManager.utils.{HestiaClient, SecureUtil}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{FiniteDuration, _}

trait UserService extends ServiceUtils {

  import io.circe._
  import io.circe.syntax._
  import io.circe.generic.auto._

  private val tokenExistTime = AppSettings.tokenExistTime * 1000L // seconds

  private var id = Await.result(MeetingDao.getMeetingIdNow(), 1000 millis) + 1

  private val signUp = (path("signUp") & post) {

    entity(as[Either[Error, SignUp]]) {
      case Right(data) =>
        val code = SecureUtil.nonceStr(20)
        dealFutureResult {
          UserInfoDao.searchByName(data.userName).map {
            case Some(_) =>
              complete(CommonRsp(180010, "用户名已注册"))
            case None =>
              val signFutureRsp: Future[SignUpRsp] = registerActor ? (RegisterReq(data.userName, data.password, _))
              dealFutureResult {
                signFutureRsp.map {
                  rsp =>
                    complete(rsp)
                }
              }
          }
        }
      case Left(error) =>
        complete(CommonRsp(200001, s"error :${error}"))
    }
  }

  private val signIn = (path("signIn") & post) {
    entity(as[Either[Error, SignIn]]) {
      case Right(data) =>
        dealFutureResult {
          UserInfoDao.searchByName(data.userName).map {
            case Some(rst) =>
              if (rst.password != SecureUtil.getSecurePassword(data.password, rst.createTime)) {
                log.error(s"login error: wrong pw")
                complete(WrongPwError)
              }
              else {
                val userInfo = UserInfo(rst.id, rst.username, if (rst.headImg == "") Common.DefaultImg.headImg else rst.headImg, "", 0L)
                val session = UserSession(rst.id.toString, rst.username, System.currentTimeMillis().toString).toSessionMap
                val meetingInfo = MeetingInfo(id, s"${rst.username}的会议", s"${rst.username}的会议", rst.id, rst.username, if (rst.headImg == "") Common.DefaultImg.headImg else rst.headImg, Some(0))
                id += 1
                addSession(session) {
                  log.info(s"${rst.id} login success")
                  complete(SignInRsp(Some(userInfo), Some(meetingInfo)))
                }
              }
            case None =>
              log.error(s"login error: no user")
              complete(NoUserError)
          }
        }
      case Left(error) =>
        complete(SignInRsp(None, None, 200002, s"error :${error}"))
    }
  }

  private val setupWebSocket = (path("setupWebSocket") & get) {
    parameter(
      'userId.as[Int],
      'token.as[String],
      'roomId.as[Int]
    ) { (uid, token, roomId) =>
      val setWsFutureRsp: Future[Option[Flow[Message, Message, Any]]] = userManager ? (SetupWs(uid, roomId, _))
      dealFutureResult(
        setWsFutureRsp.map {
          case Some(rsp) => handleWebSocketMessages(rsp)
          case None =>
            log.debug(s"建立websocket失败，userId=$uid,meetingId=$roomId")
            complete("setup error")
        }
      )

    }
  }

  private val getMeetingList = (path("getRoomList") & get) {

    val roomListFutureRsp: Future[MeetingListRsp] = meetingManager ? (GetMeetingList(_))
    dealFutureResult(
      roomListFutureRsp.map(rsp => complete(rsp))
    )
  }

  private val searchMeeting = (path("searchMeeting") & post) {
    entity(as[Either[Error, SearchMeetingReq]]) {
      case Right(rsp) =>
        if (rsp.roomId < 0) {
          complete(SearchRoomError4RoomId)
        } else {
          val searchRoomFutureRsp: Future[SearchMeetingRsp] = meetingManager ? (MeetingManager.SearchMeeting(rsp.userId, rsp.roomId, _))
          dealFutureResult(
            searchRoomFutureRsp.map(rsp => complete(rsp))
          )
        }

      case Left(error) =>
        log.debug(s"search room 接口请求错误,error=$error")
        println(error)
        complete(SearchMeetingRsp(None, 100005, msg = s"接口请求错误，error:$error"))
    }
  }

  private val nickNameChange = (path("nickNameChange") & get) {
    //    authUser { _ =>
    parameter('userId.as[Int], 'newName.as[String]) {
      (userId, newName) =>
        dealFutureResult {
          UserInfoDao.searchById(userId).map {
            case Some(_) =>
              dealFutureResult {
                UserInfoDao.searchByName(newName).map {
                  case Some(_) =>
                    complete(CommonRsp(1000051, "用户名已被注册"))
                  case None =>
                    dealFutureResult {
                      UserInfoDao.updateName(userId, newName).map { rst =>
                        meetingManager ! UserInfoChange(userId, false)
                        complete(CommonRsp(0, "ok"))
                      }
                    }
                }
              }
            case None =>
              complete(CommonRsp(1000050, "user not exist"))
          }
        }
    }
    //    }
  }

  /** 临时用户申请userId和token接口 */
  private val temporaryUser = (path("temporaryUser") & get) {
    val rspFuture: Future[GetTemporaryUserRsp] = userManager ? (TemporaryUser(_))
    dealFutureResult(rspFuture.map(complete(_)))
  }

  case class DeleteUser(email: String)

  private val getMeetingInfo = (path("getMeetingInfo") & post) {
    entity(as[Either[Error, GetMeetingInfoReq]]) {
      case Right(req) =>

              complete(CommonRsp(100046, s"userId和token验证失败"))

      case Left(error) =>
        log.debug(s"获取房间信息失败，解码失败，error:$error")
        complete(CommonRsp(100045, s"decode error:$error"))
    }
  }

  private val getInvited = (path("getInvited") & post) {
    entity(as[Either[Error, GetInvited]]) {
      case Right(req) =>
        dealFutureResult {
          MeetingDao.searchInvitedByMid(req.meetingId).map { seq =>
            val ls = seq.map(l => PeopleInfo(l._1, l._2, 0)).toList
            complete(GetInvitedRsp(Some(ls)))
          }
        }

      case Left(error) =>
        log.debug(s"获取被邀请者列表失败，解码失败，error:$error")
        complete(GetInvitedRsp(None, 100045, s"decode error:$error"))
    }
  }

  val userRoutes: Route = pathPrefix("user") {
    signUp ~ signIn ~
      nickNameChange ~ getMeetingList ~ searchMeeting ~ setupWebSocket ~ temporaryUser ~ getMeetingInfo ~ getInvited
  }
}
