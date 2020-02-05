package videomeeting.meetingManager.common

import videomeeting.protocol.ptcl.CommonInfo.User
import AppSettings._

object Common {
  object Role{
    val host = 0
    val attendance = 1
  }

  object Source{
    val pc = "PC"
    val web = "WEB"
  }

  object DefaultImg{
    val coverImg = "http://pic.neoap.com/hestia/files/image/roomManager/1c6af4509f95701ffeae9999059d66d9.png"//默认封面图
    val headImg =  "http://pic.neoap.com/hestia/files/image/roomManager/b2eab30365a2a81cf1a13d1de6332c8f.png"//默认头像
    val videoImg = "http://pic.neoap.com/hestia/files/image/roomManager/973c741b77c9607243ada13d4c40b4af.jpg"//默認的視頻封面
  }

  object Subscriber{
    val host = 0
    val attendance = 1
    val left = 2
  }

  object Like{
    val up = 1
    val down = 0
  }

  def getMpdPath(roomId:Long) = {
      s"/theia/distributor/getFile/${
        if(roomId == TestConfig.TEST_MEET_ID)"test" else roomId
      }/index.mpd"
  }


  object TestConfig{
    val TEST_USER_ID = 123
    val TEST_MEET_ID = 123
  }

//  val testUser = UserInfo

}
