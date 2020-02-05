package videomeeting.protocol.ptcl.client2Manager.http

import java.io.File

import videomeeting.protocol.ptcl.{Response, _}
import videomeeting.protocol.ptcl.CommonInfo._

/**
 * User: Arrow
 * Date: 2019/7/15
 * Time: 18:00
 */
object CommonProtocol {

  //TODO 具体错误情况和错误码由room manager细化

  /**
   * 注册 & 登录 & 查询房间
   *
   * POST
   *
   **/
  case class SignUp(
                     userName: String,
                     password: String
                   ) extends Request

  case class SignIn(
                     userName: String,
                     password: String
                   ) extends Request

  case class SignInByMail(
                           email: String,
                           password: String
                         ) extends Request

  case class SearchRoom(roomId: Int) extends Request

  case class SignUpRsp(
                        errCode: Int = 0,
                        msg: String = "ok"
                      ) extends Response

  case class SignInRsp(
                        userInfo: Option[User] = None,
                        errCode: Int = 0,
                        msg: String = "ok"
                      ) extends Response //fixme url，userName

  case class RegisterInfo(Id: Int, email: String, name: String, password: String)

  case class UserInfoRsp(info: RegisterInfo,
                         errCode: Int = 0,
                         msg: String = "ok"
                        ) extends Response

  case class SearchMeetingReq(
                               userId: Option[Int],
                               roomId: Int
                             ) extends Request

  case class SearchMeetingRsp(
                               roomInfo: Option[RoomInfo],
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends Response


  /**
   * 录像
   */


  /**
   *
   * 头像 昵称
   */

  case class ImgChangeRsp(
                           url: String,
                           errCode: Int = 0,
                           msg: String = "ok"
                         ) extends Response

  val ImgChangeRspDecodeError = ImgChangeRsp("", 100101, "error:decode error")
  val ImgChangeRspInternalError = ImgChangeRsp("", 1001012, "error:internal error")

  val SignInternalError = SignInRsp(errCode = 100001, msg = "error : internal error.")
  val NoUserError = SignInRsp(errCode = 100002, msg = "error : no user.")
  val WrongPwError = SignInRsp(errCode = 100003, msg = "error : wrong password.")
  val TokenError = SignInRsp(errCode = 100007, msg = "error : token error.")

  val SignUpInternalError = SignUpRsp(errCode = 100001, msg = "error : internal error.")
  val UserExistError = SignUpRsp(100004, msg = "error: user already exist")

  def SearchRoomError(errCode: Int = 100005, msg: String = "error: search room error，processor left") = SearchMeetingRsp(None, errCode, msg)

  val SearchRoomError4RoomId = SearchMeetingRsp(None, 100008, msg = "error: roomId error")
  val SearchRoomError4ProcessorDead = SearchMeetingRsp(None, 100006, msg = "error: processor failed")

  /**
   *
   * 建立websocket
   *
   * post
   */
  case class SetupWsReq(
                         userId: Int,
                         token: String
                       ) extends Request

  case class SetupWsRsp(
                         errCode: Int = 0,
                         msg: String = "ok"
                       ) extends Response

  val SetupWsError = SetupWsRsp(100007, msg = "error: setupWs error")

  /**
   *
   * 获取房间列表
   *
   * GET
   *
   **/
  case class MeetingListRsp(
                             meetingList: Option[List[MeetInfo]] = None,
                             errCode: Int = 0,
                             msg: String = "ok"
                           ) extends Response //fixme 添加userName,url,观众数量

  val GetRoomListError = MeetingListRsp(errCode = 100011, msg = "get room list error.")

  /** 临时用户申请userId和token接口 */
  final case class GetTemporaryUserRsp(
                                        userInfoOpt: Option[User],
                                        errCode: Int = 0,
                                        msg: String = "ok"
                                      ) extends Response

  /**
   * 根据userId,token查询roomInfo接口
   **/
  final case class GetMeetingInfoReq(
                                      userId: Int,
                                      token: String
                                    )

  final case class MeetingInfoRsp(
                                   roomInfoOpt: Option[RoomInfo],
                                   errCode: Int = 0,
                                   msg: String = "ok"
                                 )

  /**
   * WebClient
   */
  final case class InitiateRsp(
                                meetingList: Option[List[InitiateMeetingInfo]] = None,
                                errCode: Int = 0,
                                msg: String = "ok"
                              ) extends Response

  final case class AttendRsp(
                              meetingList: Option[List[AttendMeetingInfo]] = None,
                              errCode: Int = 0,
                              msg: String = "ok"
                            ) extends Response

  final case class InviteRsp(
                              meetingList: Option[List[InviteMeetingInfo]] = None,
                              errCode: Int = 0,
                              msg: String = "ok"
                            ) extends Response

  final case class AddInvite(
                              invite: Int,
                              meetingId: Int,
                              invited: Int
                            ) extends Request

  final case class AddInviteRsp(
                                 errCode: Int = 0,
                                 msg: String = "ok"
                               ) extends Response

  final case class Comment(
                            meetingId: Int,
                            userId: Int,
                            comment: String
                          ) extends Request

  final case class CommentRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends Response

  final case class Remove(
                         meetingId: Int,
                         userId: Int
                         ) extends Request

  final case class RemoveRsp(
                              errCode: Int = 0,
                              msg: String = "ok"
                            ) extends Response

  final case class Delete(
                         id: Int
                         ) extends Request

  final case class DeleteRsp(
                              errCode: Int = 0,
                              msg: String = "ok"
                            ) extends Response

  final case class GetInvited(
                             meetingId: Int
                             ) extends Request

  final case class GetInvitedRsp(
                                invited: Option[List[PeopleInfo]] = None,
                                errCode: Int = 0,
                                msg: String = "ok"
                                ) extends Response

}
