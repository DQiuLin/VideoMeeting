package videomeeting.meetingManager.models.dao

import videomeeting.meetingManager.common.{AppSettings, Common}
import videomeeting.meetingManager.models.SlickTables._
import videomeeting.meetingManager.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import videomeeting.meetingManager.Boot.executor
import videomeeting.meetingManager.utils.SecureUtil
import videomeeting.meetingManager.common.Common
import videomeeting.protocol.ptcl.CommonInfo

import scala.concurrent.Future

/**
 * File: MeetingDao.scala
 * Name: 张袁峰
 * Student ID: 16301170
 * date: 2020/1/23
 */
object MeetingDao {

  def addMeeting(name: String, time: Long, info: String, creator: Int) = {
    db.run(tMeetingInfo += rMeetingInfo(1, name, time, info, creator))
  }

  def searchByCreator(creator: Int) = {
    db.run(tMeetingInfo.filter(m => m.creator === creator).result)
  }

  def searchByAudience(userId: Int) = {
    db.run(tUserMeeting.filter(u => u.uid === userId && u.audience === 1).flatMap(m =>
      tMeetingInfo.filter(mt => mt.id === m.mid)).result)
  }

  def searchByInvited(userId: Int) = {
    db.run(tUserMeeting.filter(u => u.uid === userId && u.audience === 0).flatMap(m =>
      tMeetingInfo.filter(mt => mt.id === m.mid)).result)
  }

  def addUserMeeting(mid: Int, uid: Int, audience: Int) = {
    db.run(tUserMeeting += rUserMeeting(1, mid, uid, audience))
  }

  def addRecord(mid: Int, videoPath: String, recordPath: Option[String]) = {
    db.run(tMeetingRecord += rMeetingRecord(1, mid, videoPath, recordPath))
  }

  def addComment(mid: Int, author: Int, comment: String, time: Long) = {
    db.run(tMeetingComment += rMeetingComment(1, mid, author, comment, time))
  }

  def searchInvitedByMid(mid: Int) = {
    db.run(tUserMeeting.filter(m => m.mid === mid && m.audience === 0).flatMap(r =>
      tUserInfo.filter(u => u.id === r.uid).map(rst => rst.username)).result)
  }

  def searchAttendanceByMid(mid: Int) = {
    db.run(tUserMeeting.filter(m => m.mid === mid && m.audience === 1).flatMap(r =>
      tUserInfo.filter(u => u.id === r.uid).map(rst => rst.username)).result)
  }

  def searchCommentByMid(mid: Int) = {
    db.run(tMeetingComment.filter(c => c.mid === mid).flatMap(r =>
      tUserInfo.filter(u => u.id === r.author).map(rst => (r, rst.username))).result)
  }

  def searchRecordByMid(mid: Int) = {
    db.run(tMeetingRecord.filter(r => r.mid === mid).result)
  }
}
