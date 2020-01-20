package org.seekloud.theia.roomManager.models.dao

import org.seekloud.theia.roomManager.models.SlickTables
import slick.jdbc.PostgresProfile.api._
import org.seekloud.theia.roomManager.Boot.executor
import org.seekloud.theia.roomManager.utils.DBUtil._

import scala.concurrent.Future
/**
  * created by benyafang on 2019/9/23 16:20
  * */
object RecordCommentDAO {
  def addRecordComment(r:SlickTables.rRecordComment):Future[Long] = {
    db.run(SlickTables.tRecordComment.returning(SlickTables.tRecordComment.map(_.commentId)) += r)
  }

  def getRecordComment(roomId:Long,recordTime:Long): Future[scala.Seq[SlickTables.tRecordComment#TableElementType]] = {
    db.run(SlickTables.tRecordComment.filter(r => r.roomId === roomId && r.recordTime === recordTime).result)
  }

}
