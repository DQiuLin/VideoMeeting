package videomeeting.meetingManager.models

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.H2Profile
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
   *  @param id Database column ID SqlType(INTEGER), AutoInc, PrimaryKey
   *  @param mid Database column MID SqlType(INTEGER)
   *  @param author Database column AUTHOR SqlType(INTEGER)
   *  @param comment Database column COMMENT SqlType(VARCHAR), Length(1023,true)
   *  @param commentTime Database column COMMENT_TIME SqlType(BIGINT) */
  case class rMeetingComment(id: Int, mid: Int, author: Int, comment: String, commentTime: Long)
  /** GetResult implicit for fetching rMeetingComment objects using plain SQL queries */
  implicit def GetResultrMeetingComment(implicit e0: GR[Int], e1: GR[String], e2: GR[Long]): GR[rMeetingComment] = GR{
    prs => import prs._
    rMeetingComment.tupled((<<[Int], <<[Int], <<[Int], <<[String], <<[Long]))
  }
  /** Table description of table MEETING_COMMENT. Objects of this class serve as prototypes for rows in queries. */
  class tMeetingComment(_tableTag: Tag) extends profile.api.Table[rMeetingComment](_tableTag, Some("PUBLIC"), "MEETING_COMMENT") {
    def * = (id, mid, author, comment, commentTime) <> (rMeetingComment.tupled, rMeetingComment.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(mid), Rep.Some(author), Rep.Some(comment), Rep.Some(commentTime))).shaped.<>({r=>import r._; _1.map(_=> rMeetingComment.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(INTEGER), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column MID SqlType(INTEGER) */
    val mid: Rep[Int] = column[Int]("MID")
    /** Database column AUTHOR SqlType(INTEGER) */
    val author: Rep[Int] = column[Int]("AUTHOR")
    /** Database column COMMENT SqlType(VARCHAR), Length(1023,true) */
    val comment: Rep[String] = column[String]("COMMENT", O.Length(1023,varying=true))
    /** Database column COMMENT_TIME SqlType(BIGINT) */
    val commentTime: Rep[Long] = column[Long]("COMMENT_TIME")

    /** Foreign key referencing tMeetingInfo (database name CONSTRAINT_A) */
    lazy val tMeetingInfoFk = foreignKey("CONSTRAINT_A", mid, tMeetingInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    /** Foreign key referencing tUserInfo (database name CONSTRAINT_A9) */
    lazy val tUserInfoFk = foreignKey("CONSTRAINT_A9", author, tUserInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table tMeetingComment */
  lazy val tMeetingComment = new TableQuery(tag => new tMeetingComment(tag))

  /** Entity class storing rows of table tMeetingInfo
   *  @param id Database column ID SqlType(INTEGER), AutoInc, PrimaryKey
   *  @param name Database column NAME SqlType(VARCHAR), Length(63,true)
   *  @param time Database column TIME SqlType(BIGINT)
   *  @param info Database column INFO SqlType(VARCHAR), Length(1023,true)
   *  @param creator Database column CREATOR SqlType(INTEGER) */
  case class rMeetingInfo(id: Int, name: String, time: Long, info: String, creator: Int)
  /** GetResult implicit for fetching rMeetingInfo objects using plain SQL queries */
  implicit def GetResultrMeetingInfo(implicit e0: GR[Int], e1: GR[String], e2: GR[Long]): GR[rMeetingInfo] = GR{
    prs => import prs._
    rMeetingInfo.tupled((<<[Int], <<[String], <<[Long], <<[String], <<[Int]))
  }
  /** Table description of table MEETING_INFO. Objects of this class serve as prototypes for rows in queries. */
  class tMeetingInfo(_tableTag: Tag) extends profile.api.Table[rMeetingInfo](_tableTag, Some("PUBLIC"), "MEETING_INFO") {
    def * = (id, name, time, info, creator) <> (rMeetingInfo.tupled, rMeetingInfo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(name), Rep.Some(time), Rep.Some(info), Rep.Some(creator))).shaped.<>({r=>import r._; _1.map(_=> rMeetingInfo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(INTEGER), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column NAME SqlType(VARCHAR), Length(63,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(63,varying=true))
    /** Database column TIME SqlType(BIGINT) */
    val time: Rep[Long] = column[Long]("TIME")
    /** Database column INFO SqlType(VARCHAR), Length(1023,true) */
    val info: Rep[String] = column[String]("INFO", O.Length(1023,varying=true))
    /** Database column CREATOR SqlType(INTEGER) */
    val creator: Rep[Int] = column[Int]("CREATOR")

    /** Foreign key referencing tUserInfo (database name CONSTRAINT_2) */
    lazy val tUserInfoFk = foreignKey("CONSTRAINT_2", creator, tUserInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table tMeetingInfo */
  lazy val tMeetingInfo = new TableQuery(tag => new tMeetingInfo(tag))

  /** Entity class storing rows of table tMeetingRecord
   *  @param id Database column ID SqlType(INTEGER), AutoInc, PrimaryKey
   *  @param mid Database column MID SqlType(INTEGER)
   *  @param vPath Database column V_PATH SqlType(VARCHAR), Length(1023,true)
   *  @param rPath Database column R_PATH SqlType(VARCHAR), Length(1023,true), Default(None) */
  case class rMeetingRecord(id: Int, mid: Int, vPath: String, rPath: Option[String] = None)
  /** GetResult implicit for fetching rMeetingRecord objects using plain SQL queries */
  implicit def GetResultrMeetingRecord(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]]): GR[rMeetingRecord] = GR{
    prs => import prs._
    rMeetingRecord.tupled((<<[Int], <<[Int], <<[String], <<?[String]))
  }
  /** Table description of table MEETING_RECORD. Objects of this class serve as prototypes for rows in queries. */
  class tMeetingRecord(_tableTag: Tag) extends profile.api.Table[rMeetingRecord](_tableTag, Some("PUBLIC"), "MEETING_RECORD") {
    def * = (id, mid, vPath, rPath) <> (rMeetingRecord.tupled, rMeetingRecord.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(mid), Rep.Some(vPath), rPath)).shaped.<>({r=>import r._; _1.map(_=> rMeetingRecord.tupled((_1.get, _2.get, _3.get, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(INTEGER), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column MID SqlType(INTEGER) */
    val mid: Rep[Int] = column[Int]("MID")
    /** Database column V_PATH SqlType(VARCHAR), Length(1023,true) */
    val vPath: Rep[String] = column[String]("V_PATH", O.Length(1023,varying=true))
    /** Database column R_PATH SqlType(VARCHAR), Length(1023,true), Default(None) */
    val rPath: Rep[Option[String]] = column[Option[String]]("R_PATH", O.Length(1023,varying=true), O.Default(None))

    /** Foreign key referencing tMeetingInfo (database name CONSTRAINT_F) */
    lazy val tMeetingInfoFk = foreignKey("CONSTRAINT_F", mid, tMeetingInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table tMeetingRecord */
  lazy val tMeetingRecord = new TableQuery(tag => new tMeetingRecord(tag))

  /** Entity class storing rows of table tUserInfo
   *  @param id Database column ID SqlType(INTEGER), AutoInc, PrimaryKey
   *  @param username Database column USERNAME SqlType(VARCHAR), Length(63,true)
   *  @param password Database column PASSWORD SqlType(VARCHAR), Length(63,true)
   *  @param created Database column CREATED SqlType(BIGINT)
   *  @param headImg Database column HEAD_IMG SqlType(VARCHAR), Length(256,true), Default() */
  case class rUserInfo(id: Int, username: String, password: String, created: Long, headImg: String = "")
  /** GetResult implicit for fetching rUserInfo objects using plain SQL queries */
  implicit def GetResultrUserInfo(implicit e0: GR[Int], e1: GR[String], e2: GR[Long]): GR[rUserInfo] = GR{
    prs => import prs._
    rUserInfo.tupled((<<[Int], <<[String], <<[String], <<[Long], <<[String]))
  }
  /** Table description of table USER_INFO. Objects of this class serve as prototypes for rows in queries. */
  class tUserInfo(_tableTag: Tag) extends profile.api.Table[rUserInfo](_tableTag, Some("PUBLIC"), "USER_INFO") {
    def * = (id, username, password, created, headImg) <> (rUserInfo.tupled, rUserInfo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(username), Rep.Some(password), Rep.Some(created), Rep.Some(headImg))).shaped.<>({r=>import r._; _1.map(_=> rUserInfo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(INTEGER), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column USERNAME SqlType(VARCHAR), Length(63,true) */
    val username: Rep[String] = column[String]("USERNAME", O.Length(63,varying=true))
    /** Database column PASSWORD SqlType(VARCHAR), Length(63,true) */
    val password: Rep[String] = column[String]("PASSWORD", O.Length(63,varying=true))
    /** Database column CREATED SqlType(BIGINT) */
    val created: Rep[Long] = column[Long]("CREATED")
    /** Database column HEAD_IMG SqlType(VARCHAR), Length(256,true), Default() */
    val headImg: Rep[String] = column[String]("HEAD_IMG", O.Length(256,varying=true), O.Default(""))
  }
  /** Collection-like TableQuery object for table tUserInfo */
  lazy val tUserInfo = new TableQuery(tag => new tUserInfo(tag))

  /** Entity class storing rows of table tUserMeeting
   *  @param id Database column ID SqlType(INTEGER), AutoInc, PrimaryKey
   *  @param mid Database column MID SqlType(INTEGER)
   *  @param uid Database column UID SqlType(INTEGER)
   *  @param audience Database column AUDIENCE SqlType(INTEGER) */
  case class rUserMeeting(id: Int, mid: Int, uid: Int, audience: Int)
  /** GetResult implicit for fetching rUserMeeting objects using plain SQL queries */
  implicit def GetResultrUserMeeting(implicit e0: GR[Int]): GR[rUserMeeting] = GR{
    prs => import prs._
    rUserMeeting.tupled((<<[Int], <<[Int], <<[Int], <<[Int]))
  }
  /** Table description of table USER_MEETING. Objects of this class serve as prototypes for rows in queries. */
  class tUserMeeting(_tableTag: Tag) extends profile.api.Table[rUserMeeting](_tableTag, Some("PUBLIC"), "USER_MEETING") {
    def * = (id, mid, uid, audience) <> (rUserMeeting.tupled, rUserMeeting.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(mid), Rep.Some(uid), Rep.Some(audience))).shaped.<>({r=>import r._; _1.map(_=> rUserMeeting.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(INTEGER), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column MID SqlType(INTEGER) */
    val mid: Rep[Int] = column[Int]("MID")
    /** Database column UID SqlType(INTEGER) */
    val uid: Rep[Int] = column[Int]("UID")
    /** Database column AUDIENCE SqlType(INTEGER) */
    val audience: Rep[Int] = column[Int]("AUDIENCE")

    /** Foreign key referencing tMeetingInfo (database name CONSTRAINT_19) */
    lazy val tMeetingInfoFk = foreignKey("CONSTRAINT_19", mid, tMeetingInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    /** Foreign key referencing tUserInfo (database name CONSTRAINT_1) */
    lazy val tUserInfoFk = foreignKey("CONSTRAINT_1", uid, tUserInfo)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table tUserMeeting */
  lazy val tUserMeeting = new TableQuery(tag => new tUserMeeting(tag))
}
