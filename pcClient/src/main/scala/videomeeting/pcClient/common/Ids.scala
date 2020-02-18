package videomeeting.pcClient.common

import videomeeting.common.Constants.AudienceStatus
import videomeeting.pcClient.core.stream.LiveManager.{JoinInfo, WatchInfo}
import videomeeting.protocol.ptcl.CommonInfo.RecordInfo
import org.slf4j.LoggerFactory

/**
  * Author: zwq
  * Date: 2019/9/23
  * Time: 10:59
  */
object Ids {

  private[this] val log = LoggerFactory.getLogger(this.getClass)


  def getPlayId(audienceStatus: Int, roomId: Option[Long] = None, audienceId: Option[Long] = None, startTime: Option[Long] = None): String = {

    val playId = audienceStatus match {
      case AudienceStatus.LIVE => s"room${roomId.get}"
      case AudienceStatus.CONNECT => s"room${roomId.get}-audience${audienceId.get}"
    }

    playId

  }

}
