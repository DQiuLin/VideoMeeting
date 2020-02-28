package videomeeting.meetingManager.models

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.PostgresProfile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = tMeetingComment.schema ++ tMeetingInfo.schema ++ tMeetingRecord.schema ++ tUserInfo.schema ++ tUserMeeting.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tMeetingComment
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param mid Database column mid SqlType(int4)
   *  @param author Database column author SqlType(int4)
   *  @param comment Database column comment SqlType(varchar), Length(1023,true)
   *  @param commentTime Database column comment_time SqlType(int8) */
  case class rMeetingComment(id: Int, mid: Int, author: Int, comment: String, commentTime: Long)
  /** GetResult implicit for fetching rMeetingComment objects using plain SQL queries */
  implicit def GetResultrMeetingComment(implicit e0: GR[Int], e1: GR[String], e2: GR[Long]): GR[rMeetingComment] = GR{
    prs => import prs._
    rMeetingComment.tupled((<<[Int], <<[Int], <<[Int], <<[String], <<[Long]))
  }
  /** Table description of table meeting_comment. Objects of this class serve as prototypes for rows in queries. */
  class tMeetingComment(_tableTag: Tag) extends profile.api.Table[rMeetingComment](_tableTag, "meeting_comment") {
    def * = (id, mid, author, comment, commentTime) <> (rMeetingComment.tupled, rMeetingComment.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(mid), Rep.Some(author), Rep.Some(comment), Rep.Some(commentTime))).shaped.<>({r=>import r._; _1.map(_=> rMeetingComment.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column mid SqlType(int4) */
    val mid: Rep[Int] = column[Int]("mid")
    /** Database column author SqlType(int4) */
    val author: Rep[Int] = column[Int]("author")
    /** Database column comment SqlType(varchar), Length(1023,true) */
    val comment: Rep[String] = column[String]("comment", O.Length(1023,varying=true))
    /** Database column comment_time SqlType(int8) */
    val commentTime: Rep[Long] = column[Long]("comment_time")

    /** Foreign key referencing tMeetingInfo (database name meeting_comment) */
    lazy val tMeetingInfoFk = foreignKey("meeting_comment", mid, tMeetingInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    /** Foreign key referencing tUserInfo (database name meeting_comment_author_fkey) */
    lazy val tUserInfoFk = foreignKey("meeting_comment_author_fkey", author, tUserInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table tMeetingComment */
  lazy val tMeetingComment = new TableQuery(tag => new tMeetingComment(tag))

  /** Entity class storing rows of table tMeetingInfo
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(varchar), Length(63,true)
   *  @param time Database column time SqlType(int8)
   *  @param info Database column info SqlType(varchar), Length(1023,true)
   *  @param creator Database column creator SqlType(int4) */
  case class rMeetingInfo(id: Int, name: String, time: Long, info: String, creator: Int)
  /** GetResult implicit for fetching rMeetingInfo objects using plain SQL queries */
  implicit def GetResultrMeetingInfo(implicit e0: GR[Int], e1: GR[String], e2: GR[Long]): GR[rMeetingInfo] = GR{
    prs => import prs._
    rMeetingInfo.tupled((<<[Int], <<[String], <<[Long], <<[String], <<[Int]))
  }
  /** Table description of table meeting_info. Objects of this class serve as prototypes for rows in queries. */
  class tMeetingInfo(_tableTag: Tag) extends profile.api.Table[rMeetingInfo](_tableTag, "meeting_info") {
    def * = (id, name, time, info, creator) <> (rMeetingInfo.tupled, rMeetingInfo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(name), Rep.Some(time), Rep.Some(info), Rep.Some(creator))).shaped.<>({r=>import r._; _1.map(_=> rMeetingInfo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar), Length(63,true) */
    val name: Rep[String] = column[String]("name", O.Length(63,varying=true))
    /** Database column time SqlType(int8) */
    val time: Rep[Long] = column[Long]("time")
    /** Database column info SqlType(varchar), Length(1023,true) */
    val info: Rep[String] = column[String]("info", O.Length(1023,varying=true))
    /** Database column creator SqlType(int4) */
    val creator: Rep[Int] = column[Int]("creator")

    /** Foreign key referencing tUserInfo (database name meeting_info_fkey) */
    lazy val tUserInfoFk = foreignKey("meeting_info_fkey", creator, tUserInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table tMeetingInfo */
  lazy val tMeetingInfo = new TableQuery(tag => new tMeetingInfo(tag))

  /** Entity class storing rows of table tMeetingRecord
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param mid Database column mid SqlType(int4)
   *  @param vPath Database column v_path SqlType(varchar), Length(1023,true) */
  case class rMeetingRecord(id: Int, mid: Int, vPath: String)
  /** GetResult implicit for fetching rMeetingRecord objects using plain SQL queries */
  implicit def GetResultrMeetingRecord(implicit e0: GR[Int], e1: GR[String]): GR[rMeetingRecord] = GR{
    prs => import prs._
    rMeetingRecord.tupled((<<[Int], <<[Int], <<[String]))
  }
  /** Table description of table meeting_record. Objects of this class serve as prototypes for rows in queries. */
  class tMeetingRecord(_tableTag: Tag) extends profile.api.Table[rMeetingRecord](_tableTag, "meeting_record") {
    def * = (id, mid, vPath) <> (rMeetingRecord.tupled, rMeetingRecord.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(mid), Rep.Some(vPath))).shaped.<>({r=>import r._; _1.map(_=> rMeetingRecord.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column mid SqlType(int4) */
    val mid: Rep[Int] = column[Int]("mid")
    /** Database column v_path SqlType(varchar), Length(1023,true) */
    val vPath: Rep[String] = column[String]("v_path", O.Length(1023,varying=true))

    /** Foreign key referencing tMeetingInfo (database name meeting_record) */
    lazy val tMeetingInfoFk = foreignKey("meeting_record", mid, tMeetingInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table tMeetingRecord */
  lazy val tMeetingRecord = new TableQuery(tag => new tMeetingRecord(tag))

  /** Entity class storing rows of table tUserInfo
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param username Database column username SqlType(varchar), Length(100,true)
   *  @param password Database column password SqlType(varchar), Length(100,true)
   *  @param token Database column token SqlType(varchar), Length(63,true), Default()
   *  @param tokenCreateTime Database column token_create_time SqlType(int8)
   *  @param headImg Database column head_img SqlType(varchar), Length(256,true), Default()
   *  @param createTime Database column create_time SqlType(int8)
   *  @param rtmpToken Database column rtmp_token SqlType(varchar), Length(256,true), Default() */
  case class rUserInfo(id: Int, username: String, password: String, token: String = "", tokenCreateTime: Long, headImg: String = "", createTime: Long, rtmpToken: String = "")
  /** GetResult implicit for fetching rUserInfo objects using plain SQL queries */
  implicit def GetResultrUserInfo(implicit e0: GR[Int], e1: GR[String], e2: GR[Long]): GR[rUserInfo] = GR{
    prs => import prs._
    rUserInfo.tupled((<<[Int], <<[String], <<[String], <<[String], <<[Long], <<[String], <<[Long], <<[String]))
  }
  /** Table description of table user_info. Objects of this class serve as prototypes for rows in queries. */
  class tUserInfo(_tableTag: Tag) extends profile.api.Table[rUserInfo](_tableTag, "user_info") {
    def * = (id, username, password, token, tokenCreateTime, headImg, createTime, rtmpToken) <> (rUserInfo.tupled, rUserInfo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(username), Rep.Some(password), Rep.Some(token), Rep.Some(tokenCreateTime), Rep.Some(headImg), Rep.Some(createTime), Rep.Some(rtmpToken))).shaped.<>({r=>import r._; _1.map(_=> rUserInfo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column username SqlType(varchar), Length(100,true) */
    val username: Rep[String] = column[String]("username", O.Length(100,varying=true))
    /** Database column password SqlType(varchar), Length(100,true) */
    val password: Rep[String] = column[String]("password", O.Length(100,varying=true))
    /** Database column token SqlType(varchar), Length(63,true), Default() */
    val token: Rep[String] = column[String]("token", O.Length(63,varying=true), O.Default(""))
    /** Database column token_create_time SqlType(int8) */
    val tokenCreateTime: Rep[Long] = column[Long]("token_create_time")
    /** Database column head_img SqlType(varchar), Length(256,true), Default() */
    val headImg: Rep[String] = column[String]("head_img", O.Length(256,varying=true), O.Default(""))
    /** Database column create_time SqlType(int8) */
    val createTime: Rep[Long] = column[Long]("create_time")
    /** Database column rtmp_token SqlType(varchar), Length(256,true), Default() */
    val rtmpToken: Rep[String] = column[String]("rtmp_token", O.Length(256,varying=true), O.Default(""))
  }
  /** Collection-like TableQuery object for table tUserInfo */
  lazy val tUserInfo = new TableQuery(tag => new tUserInfo(tag))

  /** Entity class storing rows of table tUserMeeting
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param mid Database column mid SqlType(int4)
   *  @param uid Database column uid SqlType(int4)
   *  @param audience Database column audience SqlType(int4) */
  case class rUserMeeting(id: Int, mid: Int, uid: Int, audience: Int)
  /** GetResult implicit for fetching rUserMeeting objects using plain SQL queries */
  implicit def GetResultrUserMeeting(implicit e0: GR[Int]): GR[rUserMeeting] = GR{
    prs => import prs._
    rUserMeeting.tupled((<<[Int], <<[Int], <<[Int], <<[Int]))
  }
  /** Table description of table user_meeting. Objects of this class serve as prototypes for rows in queries. */
  class tUserMeeting(_tableTag: Tag) extends profile.api.Table[rUserMeeting](_tableTag, "user_meeting") {
    def * = (id, mid, uid, audience) <> (rUserMeeting.tupled, rUserMeeting.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(mid), Rep.Some(uid), Rep.Some(audience))).shaped.<>({r=>import r._; _1.map(_=> rUserMeeting.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column mid SqlType(int4) */
    val mid: Rep[Int] = column[Int]("mid")
    /** Database column uid SqlType(int4) */
    val uid: Rep[Int] = column[Int]("uid")
    /** Database column audience SqlType(int4) */
    val audience: Rep[Int] = column[Int]("audience")

    /** Foreign key referencing tMeetingInfo (database name user_meeting_mid_fkey) */
    lazy val tMeetingInfoFk = foreignKey("user_meeting_mid_fkey", mid, tMeetingInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    /** Foreign key referencing tUserInfo (database name user_meeting) */
    lazy val tUserInfoFk = foreignKey("user_meeting", uid, tUserInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table tUserMeeting */
  lazy val tUserMeeting = new TableQuery(tag => new tUserMeeting(tag))
}
