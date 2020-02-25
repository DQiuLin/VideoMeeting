package videomeeting.meetingManager.protocol

import videomeeting.protocol.ptcl.CommonInfo
import videomeeting.protocol.ptcl.CommonInfo._

object CommonInfoProtocol {

  final case class WholeRoomInfo(
                                  var roomInfo: MeetingInfo,
                                  var liveInfo: LiveInfo, //房间主持人的liveInfo
                                  var isJoinOpen: Boolean = false,
                                  var layout: Int = CommonInfo.ScreenLayout.EQUAL,
                                  var aiMode: Int = 0
                                )

}
