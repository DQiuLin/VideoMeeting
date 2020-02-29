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

  def searchById(id: Int) = {
    db.run(tMeetingInfo.filter(m => m.id === id).flatMap(m =>
      tUserInfo.filter(u => u.id === m.creator).map(r => (m, r.username))).result.headOption)
  }

  def searchByCreator(creator: Int) = {
    db.run(tMeetingInfo.filter(m => m.creator === creator).flatMap(m =>
      tUserInfo.filter(u => u.id === m.creator).map(r => (m, r.username))).result)
  }

  def searchByAudience(userId: Int) = {
    db.run(tUserMeeting.filter(u => u.uid === userId && u.audience === 1).flatMap(m =>
      tMeetingInfo.filter(mt => mt.id === m.mid)).flatMap(mt =>
        tUserInfo.filter(u => u.id === mt.creator).map(r => (mt, r.username))).result)
  }

  def searchByInvited(userId: Int) = {
    db.run(tUserMeeting.filter(u => u.uid === userId && u.audience === 0).flatMap(m =>
      tMeetingInfo.filter(mt => mt.id === m.mid)).flatMap(mt =>
        tUserInfo.filter(u => u.id === mt.creator).map(r => (mt, r.username))).result)
  }

  def addUserMeeting(mid: Int, uid: Int, audience: Int) = {
    db.run(tUserMeeting += rUserMeeting(1, mid, uid, audience))
  }

  def addRecord(mid: Int, videoPath: String) = {
    db.run(tMeetingRecord += rMeetingRecord(1, mid, videoPath))
  }

  def addComment(mid: Int, author: Int, comment: String, time: Long) = {
    db.run(tMeetingComment += rMeetingComment(1, mid, author, comment, time))
  }

  def searchInvitedByMid(mid: Int) = {
    db.run(tUserMeeting.filter(m => m.mid === mid && m.audience === 0).flatMap(r =>
      tUserInfo.filter(u => u.id === r.uid).map(rst => (rst.id, rst.username))).result)
  }

  def searchAttendanceByMid(mid: Int) = {
    db.run(tUserMeeting.filter(m => m.mid === mid && m.audience === 1).flatMap(r =>
      tUserInfo.filter(u => u.id === r.uid).map(rst => (rst.id, rst.username))).result)
  }

  def searchCommentByMid(mid: Int) = {
    db.run(tMeetingComment.filter(c => c.mid === mid).flatMap(r =>
      tUserInfo.filter(u => u.id === r.author).map(rst => (r, rst.username, rst.headImg))).result)
  }

  def searchRecordByMid(mid: Int) = {
    db.run(tMeetingRecord.filter(r => r.mid === mid).result.headOption)
  }

  def searchAllRelatedByMid(mid: Int) = {
    db.run(tUserMeeting.filter(m => m.mid === mid).flatMap(r =>
      tUserInfo.filter(u => u.id === r.uid).map(rst => (rst.id, rst.username, r.audience))).result)
  }

  def deleteUserMeeting(mid: Int, uid: Int) = {
    db.run(tUserMeeting.filter(m => m.mid === mid && m.uid === uid).delete)
  }

  def deleteCommentById(id: Int) = {
    db.run(tMeetingComment.filter(c => c.id === id).delete)
  }

  def searchByMidAndUid(mid: Int, uid: Int) = {
    db.run(tUserMeeting.filter(m => m.mid === mid && m.uid === uid).flatMap(m =>
      tMeetingInfo.filter(mt => mt.id === m.mid)).flatMap(mt =>
        tUserInfo.filter(u => u.id === mt.creator).map(r => (mt, r.username))).result.headOption)
  }

  def getMeetingIdNow() = {
    db.run(tMeetingInfo.length.result)
  }
}
