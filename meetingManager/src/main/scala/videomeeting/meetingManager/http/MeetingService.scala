package videomeeting.meetingManager.http

import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol._
import videomeeting.meetingManager.Boot._
import videomeeting.meetingManager.core.UserManager.{log => _, _}

import scala.language.postfixOps
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import videomeeting.meetingManager.Boot.{executor, meetingManager, scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import videomeeting.meetingManager.core.MeetingManager.GetMeetingList
import videomeeting.meetingManager.models.dao.MeetingDao
import videomeeting.protocol.ptcl.CommonInfo.{AttendMeetingInfo, CommentInfo, InitiateMeetingInfo, InviteMeetingInfo, MeetInfo, PeopleInfo}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{FiniteDuration, _}

/**
 * File: MeetingService.scala
 * Name: 张袁峰
 * Student ID: 16301170
 * date: 2020/1/23
 */
trait MeetingService extends ServiceUtils {

  import io.circe._
  import io.circe.syntax._
  import io.circe.generic.auto._

  private val getInitiateList = (path("initiate") & get) {
    parameter(
      'uid.as[Int]
    ) { uid =>
      dealFutureResult {
//        MeetingDao.searchByCreator(uid).flatMap(ls =>
//          ls.map { m =>
//            MeetingDao.searchAllRelatedByMid(m._1.id).flatMap(l =>
//              MeetingDao.searchCommentByMid(m._1.id).flatMap(lst =>
//                MeetingDao.searchRecordByMid(m._1.id).map { op =>
//                  val peopleList = l.map(r => PeopleInfo(r._1, r._2, r._3)).toList
//                  val commentList = lst.map(r => CommentInfo(r._1.id, r._2, r._1.commentTime, r._1.comment)).toList
//                  val meetInfo = MeetInfo(m._1.name, m._1.time, m._1.info, m._2, peopleList, commentList)
//                  val initiateInfo = InitiateMeetingInfo(m._1.id, None, Some(op.get.vPath), meetInfo)
//                  initiateInfo
//                }
//              )
//            )
//          }
//        )
        val meetingsFuture = MeetingDao.searchByCreator(uid)
        val seqFuture = for {
          meetings <- meetingsFuture
        } yield {
          for {
            meeting <- meetings
          } yield {
            val people = MeetingDao.searchAllRelatedByMid(meeting._1.id)
            val comments = MeetingDao.searchCommentByMid(meeting._1.id)
            val record = MeetingDao.searchRecordByMid(meeting._1.id)
            val ls = for {
              p <- people
              c <- comments
              r <- record
            } yield {
              val peopleList = p.map(r => PeopleInfo(r._1, r._2, r._3)).toList
              val commentList = c.map(r => CommentInfo(r._1.id, r._2, r._3, r._1.commentTime, r._1.comment)).toList
              val videoPath = r.map(r => r.vPath)
              (peopleList, commentList, videoPath)
            }
            val resFuture = for {
              l <- ls
            } yield {
              val meetInfo = MeetInfo(meeting._1.name, meeting._1.time, meeting._1.info, meeting._2, l._1, l._2)
              val initiateInfo = InitiateMeetingInfo(meeting._1.id, None, l._3, meetInfo)
              initiateInfo
            }
            val res = Await.result(resFuture, 5.second)
            res
          }
        }
        seqFuture.map(seq => complete(InitiateRsp(Some(seq.toList))))
      }
    }
  }

  private val getAttendList = (path("attend") & get) {
    parameter(
      'uid.as[Int]
    ) { uid =>
      dealFutureResult {
        val meetingsFuture = MeetingDao.searchByAudience(uid)
        val seqFuture = for {
          meetings <- meetingsFuture
        } yield {
          for {
            meeting <- meetings
          } yield {
            val people = MeetingDao.searchAllRelatedByMid(meeting._1.id)
            val comments = MeetingDao.searchCommentByMid(meeting._1.id)
            val record = MeetingDao.searchRecordByMid(meeting._1.id)
            val ls = for {
              p <- people
              c <- comments
              r <- record
            } yield {
              val peopleList = p.map(r => PeopleInfo(r._1, r._2, r._3)).toList
              val commentList = c.map(r => CommentInfo(r._1.id, r._2, r._3, r._1.commentTime, r._1.comment)).toList
              val videoPath = r.map(r => r.vPath)
              (peopleList, commentList, videoPath)
            }
            val resFuture = for {
              l <- ls
            } yield {
              val meetInfo = MeetInfo(meeting._1.name, meeting._1.time, meeting._1.info, meeting._2, l._1, l._2)
              val attendInfo = AttendMeetingInfo(meeting._1.id, None, l._3, meetInfo)
              attendInfo
            }
            val res = Await.result(resFuture, 5.second)
            res
          }
        }
        seqFuture.map(seq => complete(AttendRsp(Some(seq.toList))))
      }
    }
  }

  private val getInviteList = (path("invited") & get) {
    parameter(
      'uid.as[Int]
    ) { uid =>
      dealFutureResult {
        val meetingsFuture = MeetingDao.searchByInvited(uid)
        val seqFuture = for {
          meetings <- meetingsFuture
        } yield {
          for {
            meeting <- meetings
          } yield {
            val people = MeetingDao.searchAllRelatedByMid(meeting._1.id)
            val comments = MeetingDao.searchCommentByMid(meeting._1.id)
            val record = MeetingDao.searchRecordByMid(meeting._1.id)
            val ls = for {
              p <- people
              c <- comments
              r <- record
            } yield {
              val peopleList = p.map(r => PeopleInfo(r._1, r._2, r._3)).toList
              val commentList = c.map(r => CommentInfo(r._1.id, r._2, r._3, r._1.commentTime, r._1.comment)).toList
              val videoPath = r.map(r => r.vPath)
              (peopleList, commentList, videoPath)
            }
            val resFuture = for {
              l <- ls
            } yield {
              val meetInfo = MeetInfo(meeting._1.name, meeting._1.time, meeting._1.info, meeting._2, l._1, l._2)
              val attendInfo = InviteMeetingInfo(meeting._1.id, None, l._3, meetInfo)
              attendInfo
            }
            val res = Await.result(resFuture, 5.second)
            res
          }
        }
        seqFuture.map(seq => complete(InviteRsp(Some(seq.toList))))
      }
    }
  }

  private val getCommentList = (path("getComment") & get) {
    parameter(
      'mid.as[Int]
    ) { mid =>
      dealFutureResult {
        MeetingDao.searchCommentByMid(mid).map { r=>
          val l = r.map(rst => CommentInfo(rst._1.id, rst._2, rst._3, rst._1.commentTime, rst._1.comment)).toList
          complete(GetCommentRsp(Some(l)))
        }
      }
    }
  }

  private val invite = (path("invite") & post) {
    entity(as[Either[Error, AddInvite]]) {
      case Right(data) =>
        dealFutureResult {
          MeetingDao.searchById(data.meetingId).map {
            case Some(m) =>
              if (m._1.creator == data.invite) {
                dealFutureResult {
                  MeetingDao.searchByMidAndUid(data.meetingId, data.invited).map {
                    case Some(mt) =>
                      complete(AddInviteRsp(200001, s"邀请失败，该用户已被邀请"))
                    case None =>
                      dealFutureResult {
                        MeetingDao.addUserMeeting(data.meetingId, data.invited, 0).map { r =>
                        if (r > 0)
                          complete(AddInviteRsp())
                        else
                          complete(AddInviteRsp(200000, s"邀请失败"))
                        }
                      }
                  }
                }
              }
              else complete(AddInviteRsp(200002, s"邀请失败，您不是创建者"))
            case None =>
              complete(AddInviteRsp(200003, s"邀请失败，不存在此会议"))
          }
        }
      case Left(error) =>
        complete(AddInviteRsp(200004, s"error :${error}"))
    }
  }

  private val comment = (path("comment") & post) {
    entity(as[Either[Error, Comment]]) {
      case Right(data) =>
        dealFutureResult {
          MeetingDao.addComment(data.meetingId, data.userId, data.comment, data.time).map { r =>
            if (r > 0)
              complete(CommentRsp())
            else
              complete(CommentRsp(201001, s"评论失败"))
          }
        }
      case Left(error) =>
        complete(CommentRsp(201002, s"error :${error}"))
    }
  }

  private val deleteInvite = (path("remove") & post) {
    entity(as[Either[Error, Remove]]) {
      case Right(data) =>
      dealFutureResult {
        MeetingDao.deleteUserMeeting(data.meetingId, data.userId).map(_ => complete(RemoveRsp()))
      }
    }
  }

  private val deleteComment = (path("delete") & post) {
    entity(as[Either[Error, Delete]]) {
      case Right(data) =>
        dealFutureResult {
          MeetingDao.deleteCommentById(data.id).map(_ => complete(DeleteRsp()))
        }
    }
  }

  val meetingRoutes: Route = pathPrefix("meeting") {
    getInitiateList ~ getAttendList ~ getInviteList ~ invite ~ comment ~ deleteInvite ~ deleteComment ~ getCommentList
  }

}
