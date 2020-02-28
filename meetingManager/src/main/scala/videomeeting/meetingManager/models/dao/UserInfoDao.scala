package videomeeting.meetingManager.models.dao

import videomeeting.meetingManager.common.{AppSettings, Common}
import videomeeting.meetingManager.models.SlickTables._
import videomeeting.meetingManager.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import videomeeting.meetingManager.Boot.executor
import videomeeting.meetingManager.utils.SecureUtil
import videomeeting.meetingManager.common.Common
import videomeeting.meetingManager.common.Common.DefaultImg
import videomeeting.protocol.ptcl.CommonInfo
import videomeeting.protocol.ptcl.CommonInfo.UserInfo

import scala.concurrent.Future


object UserInfoDao {

  def getHeadImg(headImg:String):String = {
    if(headImg == "")DefaultImg.headImg else headImg
  }

  def addUser(name:String, pw:String, token:String, timeStamp:Long, rtmpToken:String) = {
    val password = SecureUtil.getSecurePassword(pw, timeStamp)
    db.run(tUserInfo += rUserInfo(1, name, password, token, timeStamp, Common.DefaultImg.headImg, timeStamp, rtmpToken))
  }

  def modifyImg4User(userId: Int, fileName: String) = {
      db.run(tUserInfo.filter(_.id === userId).map(_.headImg).update(fileName))
  }


  def searchByName(name:String) = {
    db.run(tUserInfo.filter(i => i.username === name).result.headOption)
  }

  def searchById(uid: Int) = {
    db.run(tUserInfo.filter(i => i.id === uid).result.headOption)
  }

  def getUserInfo(users: List[Int]) = {
    Future.sequence(users.map{uid =>
      db.run(tUserInfo.filter(t => t.id === uid).result)}).map(_.flatten).map{user =>
        user.map(r => UserInfo(r.id, r.username,if(r.headImg == "") Common.DefaultImg.headImg else r.headImg, "", 0L)).toList
    }
  }

  def updateHeadImg(uid:Int,headImg:String) = {
    db.run(tUserInfo.filter(i => i.id === uid).map(l=> (l.headImg)
    ).update(headImg))
  }

  def updateName(uid:Int,name:String) = {
    db.run(tUserInfo.filter(i => i.id === uid).map(l=> (l.username)
    ).update(name))
  }

  def updatePsw(uid:Int,psw:String) = {
    db.run(tUserInfo.filter(i => i.id === uid).map(l=> (l.password)
    ).update(psw))
  }

  def getUserLen = {
    db.run(tUserInfo.length.result)
  }

  def main(args: Array[String]): Unit = {
    val a = searchById(100001)
    Thread.sleep(3000)
    println(a)
  }
}
