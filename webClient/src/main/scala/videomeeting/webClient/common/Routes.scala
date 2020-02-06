package videomeeting.webClient.common

/**
  * created by dql on 2020/1/20
  * webClient路径
  */

object Routes {
  private val base = "/videomeeting/meetingManager"

  object UserRoutes {

    private val baseUrl = base + "/user"

    val userRegister: String = baseUrl + "/signUp"
    val userLogin: String = baseUrl + "/signIn"
    val getMeetingList: String = baseUrl + "/getMeetingList"
    val searchMeeting: String = baseUrl + "/searchMeeting"
    val temporaryUser: String = baseUrl + "/temporaryUser"
    val getMeetingInfo: String = baseUrl + "/getMeetingInfo"

    def uploadImg(imgType: Int, userId: String): String = base + s"/file/uploadFile?imgType=$imgType&userId=$userId"

    def nickNameChange(userId: Long, userName: String): String = baseUrl + s"/nickNameChange?userId=$userId&newName=$userName"

    def setupWebSocket(userId: Long, token: String, roomId: Long): String = baseUrl + s"/setupWebSocket?userId=$userId&token=$token&roomId=$roomId"
  }

  object MeetingRoutes {

    private val baseUrl = base + "/meeting"

    val invite: String = baseUrl + "/invite"
    val comment: String = baseUrl + "/comment"
    val deleteInvite: String = baseUrl + "/remove"
    val deleteComment: String = baseUrl + "/delete"

    def getInitiateList(uid: Int): String = baseUrl + s"/initiate?uid=$uid"

    def getAttendList(uid: Int): String = baseUrl + s"/attend?uid=$uid"

    def getInviteList(uid: Int): String = baseUrl + s"/invited?uid=$uid"

  }

}
