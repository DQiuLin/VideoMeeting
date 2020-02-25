package videomeeting.protocol.ptcl.processer2Manager

object Processor {

  sealed trait CommonRsp {
    val errCode: Int
    val msg: String
  }

  /**  url:processor/newConnect
    *  post
    */
  case class NewConnect(
                         roomId: Long,
                         host: String,
                         client: List[String], //多个参会人员的liveId
                         pushLiveId:String,
                         pushLiveCode:String,
                         startTime: Long
                       )

  case class NewConnectRsp(
                          errCode: Int = 0,
                          msg:String = "ok"
                          ) extends CommonRsp

  /**  url:processor/closeRoom
    *  post
    */
  case class CloseRoom(
                        roomId: Long
                      )

  case class CloseRoomRsp(
                            errCode: Int = 0,
                            msg:String = "ok"
                          ) extends CommonRsp


  /**  url:processor/update
    *  post
    */
  case class UpdateRoomInfo(
                             roomId: Long,
                             layout: Int
                           )

  case class UpdateRsp(
                        errCode: Int = 0,
                        msg:String = "ok"
                      ) extends CommonRsp

  /**  url:processor/forceExit
    *  post
    */
  case class ForceExit(
                        roomId: Long,
                        liveId: String,
                        startTime: Long
                      )

  case class ExitRsp(
                      errCode: Int = 0,
                      msg: String = "ok"
                    ) extends CommonRsp
}
