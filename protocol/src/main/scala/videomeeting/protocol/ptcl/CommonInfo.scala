package videomeeting.protocol.ptcl

/**
  * User: TangYaruo
  * Date: 2019/7/15
  * Time: 22:05
  */
object CommonInfo {

  object ScreenLayout {
    val EQUAL = 0 //对等窗口
    val HOST_MAIN_LEFT = 1 //大小窗口（主播大，观众再左边）
    val HOST_MAIN_RIGHT = 1 //大小窗口（主播大，观众再右边）
    val AUDIENCE_MAIN_LEFT = 2 //大小窗口（观众大，主播在左边）
    val AUDIENCE_MAIN_RIGHT = 2 //大小窗口（观众大，主播在右边）
  }

  object ClientType {
    val PC = 0
    val WEB = 1

  }

  object CameraPosition{
    val left_top = 0
    val right_top = 1
    val right_bottom = 2
    val left_bottom = 3
  }

  case class User(
                   id: Long,
                   name: String,
                   password: String,
                   avatar: String
                 )

  case class UserInfo(
                       userId: Long,
                       userName: String,
                       headImgUrl:String,
                     )

  case class MeetingInfo(
                        meetingId: Int,
                        meetingName: String,
                        userId: Int,
                        username: String,
                        coverImgUrl:String,
                        var attendanceNum: Int = 0,
                        var rtmp: Option[String] = None
                        )

  case class RecordInfo(
                         recordId:Long,//数据库中的录像id，用于删除录像
                         roomId:Long,
                         recordName:String,
                         recordDes:String,
                         userId:Long,
                         userName:String,
                         startTime:Long,
                         headImg:String,
                         coverImg:String,
                         observeNum:Int, //浏览量
                         likeNum:Int,
                         duration:String = ""
                       )

  case class InviteMeetingInfo(
                                id: Int,
                                picture: Option[String] = None, //会议视频封面
                                video: Option[String], //会议视频url
                                meetInfo: MeetInfo
                              )

  case class MeetInfo(
                       name: String, //会议名称
                       time: Long, //会议时间
                       intro: String, //会议简介
                       creator: String,
                       people: List[PeopleInfo], //此会议相关用户
                       comment: List[CommentInfo] //评论
                     )

  case class PeopleInfo(
                         id: Int, //若用户名是唯一索引，id可省
                         name: String, //用户名
                         pType: Int //类型：参会/邀请 即：参会人员还是之后被邀请查看会议视频的人员
                       )

  case class CommentInfo(
                          id: Int,
                          usrName: String,
                          time: Long,
                          content: String
                        )

  case class InitiateMeetingInfo(
                                  id: Int,
                                  picture: Option[String], //会议视频封面
                                  video: Option[String], //会议视频url
                                  meetInfo: MeetInfo
                                )

  case class AttendMeetingInfo(
                                id: Int,
                                picture: Option[String], //会议视频封面
                                video: Option[String], //会议视频url
                                meetInfo: MeetInfo
                              )

  case class LiveInfo(
                       liveId: String,
                       liveCode: String
                     )

  /*参会者信息*/
  case class AttendenceInfo(
                           userId: Int,
                           userName: String,
                           liveId: String
                         )

//  case class MeetingInfo(
//    roomId: Long,
//    roomName: String,
//    roomDes: String,
//    userId: Long,  //房主id
//    userName:String,
//    headImgUrl:String,
//    coverImgUrl:String,
//    var observerNum:Int,
//    var like:Int,
//    var mpd: Option[String] = None,
//    var rtmp: Option[String] = None
//    //var liveAdd: Option[String] = None
//  )

  case class UserDes(
    userId: Long,
    userName: String,
    headImgUrl:String
  )
}
