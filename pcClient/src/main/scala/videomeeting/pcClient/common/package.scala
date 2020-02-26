package videomeeting.pcClient

import videomeeting.protocol.ptcl.CommonInfo.{RecordInfo, MeetingInfo}

/**
  * User: TangYaruo
  * Date: 2019/9/16
  * Time: 20:08
  */
package object common {

  /*room or record*/
  case class AlbumInfo(
                        roomId: Int,
                        roomName: String,
                        roomDes: String,
                        coverImgUrl: String,
                        userId: Int,
                        userName: String,
                        headImgUrl: String,
                        audienceNum: Option[Int] = None,
                        streamId: Option[String] = None,
                        recordId: Int = 0,
                        timestamp: Long = 0l,
                        duration: String = ""
                      ) {
    def toRoomInfo =
      MeetingInfo(roomId, roomName, roomDes, coverImgUrl,
        userId, userName, headImgUrl, audienceNum, streamId)

    def toRecordInfo =
      RecordInfo(recordId, roomId, roomName, roomDes, userId, userName,
        timestamp, headImgUrl, coverImgUrl, duration)
  }

  implicit class RichRoomInfo(r: MeetingInfo) {
    def toAlbum: AlbumInfo =
      AlbumInfo(
        r.meetingId,
        r.meetingName,
        r.roomDes,
        r.coverImgUrl,
        r.userId,
        r.username,
        r.headImgUrl,
        r.attendanceNum,
        streamId = r.rtmp
      )
  }

  implicit class RichRecordInfo(r: RecordInfo) {
    def toAlbum: AlbumInfo =
      AlbumInfo(
        r.roomId,
        r.recordName,
        r.recordDes,
        r.coverImg,
        r.userId,
        r.userName,
        r.headImg,
        recordId = r.recordId,
        timestamp = r.startTime,
        duration = r.duration
      )
  }

}
