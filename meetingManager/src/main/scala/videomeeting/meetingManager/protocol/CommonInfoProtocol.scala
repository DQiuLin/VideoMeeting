package videomeeting.meetingManager.protocol

import videomeeting.protocol.ptcl.CommonInfo.RoomInfo
import videomeeting.protocol.ptcl.CommonInfo

object CommonInfoProtocol {

  //fixme isJoinOpen,liveInfoMap字段移到这里
  final case class WholeRoomInfo(
                                var roomInfo:RoomInfo,
                                //var recordStartTime: Option[Long] = None,
                                var layout:Int = CommonInfo.ScreenLayout.EQUAL,
                                var aiMode:Int = 0
                                )

}
