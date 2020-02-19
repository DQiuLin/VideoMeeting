package videomeeting.pcClient.core.rtp

import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

import videomeeting.pcClient.utils.NetUtil
import videomeeting.pcClient.utils.RtpUtil.{clientHost, clientPushPort, rtpServerHost, rtpServerPushPort}
import org.slf4j.LoggerFactory

/**
  * User: TangYaruo
  * Date: 2019/8/20
  * Time: 21:55
  */
class PushChannel {

  private val log = LoggerFactory.getLogger(this.getClass)

  /*PUSH*/
  val serverPushAddr = new InetSocketAddress(rtpServerHost, rtpServerPushPort)

}
