package videomeeting.webClient.pages

import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Video
import org.scalajs.dom.raw.HTMLElement

/**
  * created by dql on 2020/1/19
  * web我参与的会议列表页面
  */
object Attend {

  //先写在前端，之后加入协议-------
  case class AttendMeetingInfo(
                                id: Long,
                                picture: Option[String], //会议视频封面
                                video: Option[String], //会议视频url
                                meetInfo: MeetInfo
                              )

  case class MeetInfo(
                       name: String, //会议名称
                       time: Long, //会议时间
                       intro: String, //会议简介
                       people: List[PeopleInfo], //此会议相关用户
                       comment: List[CommentInfo] //评论
                     )

  case class PeopleInfo(
                         id: Long, //若用户名是唯一索引，id可省
                         name: String, //用户名
                         pType: Int //类型：参会/邀请 即：参会人员还是之后被邀请查看会议视频的人员
                       )

  case class CommentInfo(
                          id: Long,
                          usrName: String,
                          time: Long,
                          content: String
                        )

  //-------

  val meetList: Var[List[AttendMeetingInfo]] = Var(Nil)
  val totalCount: Rx[Int] = meetList.map(_.length)
  val videoPlay: Var[Long] = Var(-1)

  def init() = {
    videoPlay := -1
  }

  def getList(): Unit = {

  }

  val meetTable = meetList.map { lst =>
    if (lst.isEmpty)
      <div class="list-table">
        <div class="list-th attend">
          <div>会议视频</div>
          <div>会议名称</div>
          <div>会议时间</div>
          <div>会议简介</div>
          <div>参会人员</div>
        </div>
      </div>
    else
      <div class="list-table">
        <div class="list-th attend">
          <div>会议视频</div>
          <div>会议名称</div>
          <div>会议时间</div>
          <div>会议简介</div>
          <div>参会人员</div>
        </div>
        <div style="width:100%;height:auto;">
          {lst.zipWithIndex.map { l =>
          val bgdColor = if (l._2 % 2 == 1) "background-color:rgba(242,245,250,1)" else "background-color:rgba(255,255,255,1)"
          val item = l._1
          <div class="list-tr attend" style={bgdColor}>
            {if (item.video.isDefined) {
            <div>
              {var videoHeight = 0
            videoPlay.map { id =>
              if (id == item.id) {
                <div>
                  <video id={s"video-${item.id}"} src={item.video.get} width="100%" height={s"$videoHeight"} controls="controls"
                         style="outline:none;" x5-playsinline="true" playsinline="true" webkit-playsinline="true"></video>
                </div>
              } else {
                val posterUrl = item.picture.getOrElse("")
                <div style="position:relative">
                  <img src={posterUrl}></img>
                  <img src="/VideoMeetiong/webClient/static/img/play@2x.png"
                       style="position:absolute;top:0.64rem;left:1.28rem;width:0.4rem;height:0.4rem;"
                       onclick={(e: Event) =>
                         videoHeight = e.target.asInstanceOf[HTMLElement].parentElement.clientHeight
                         videoPlay := item.id
                         dom.document.getElementById(s"video-${item.id}").asInstanceOf[Video].play()}></img>
                </div>
              }
            }}
            </div>
          } else if (item.picture.isDefined) {
            <img src={item.picture.get}></img>
          } else {
            <p>
              {item.meetInfo.name}
            </p>
          }}<div>
            {item.meetInfo.name}
          </div>
            <div>
              {item.meetInfo.time}
            </div>
            <div>
              {item.meetInfo.intro}
            </div>
            <div>
              {item.meetInfo.people}
            </div>
          </div>
        }}
        </div>
      </div>
  }

  def app: xml.Node = {
    init()
    getList()
    <div>
      <div class="list-bg">
        {meetTable}
        <div class="total-count">
          总共有{totalCount}条记录
        </div>
      </div>
    </div>
  }
}

