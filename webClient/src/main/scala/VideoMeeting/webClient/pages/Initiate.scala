package VideoMeeting.webClient.pages


import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Video
import org.scalajs.dom.raw.HTMLElement

/**
  * created by dql on 2020/1/19
  * web我发起的会议列表页面
  */
object Initiate {

  //先写在前端，之后加入协议-------
  case class InitiateMeetingInfo(
                                  id: Long,
                                  picture: Option[String],
                                  video: Option[String],
                                  meetInfo: MeetInfo
                                )

  case class MeetInfo(
                       id: Long,
                       name: String, //会议名称
                       time: Long, //会议时间
                       intro: String, //会议简介
                       people: List[PeopleInfo], //参会人员
                       comment: List[CommentInfo] //评论
                     )

  case class PeopleInfo(
                         id: Long,
                         name: String,
                         pType: Int
                       )

  case class CommentInfo(
                          id: Long,
                          usrName: String,
                          time: Long
                        )

  //-------

  val meetList: Var[List[InitiateMeetingInfo]] = Var(Nil)
  val totalCount = meetList.map(_.length)
  val videoPlay: Var[Long] = Var(-1)

  def init() = {
    videoPlay := -1
  }

  def getList(): Unit = {

  }

  def peopleManageModal(meetId: Long): Unit = {

  }

  def commentManageModal(meetId: Long): Unit = {

  }

  val meetTable = meetList.map { lst =>
    if (lst.isEmpty)
      <div class="list-table">
        <div class="list-th initiate">
          <div>会议视频</div>
          <div>会议名称</div>
          <div>会议时间</div>
          <div>会议简介</div>
          <div>参会人员</div>
          <div>操作</div>
          <div>操作</div>
        </div>
      </div>
    else
      <div class="list-table">
        <div class="list-th initiate">
          <div>会议视频</div>
          <div>会议名称</div>
          <div>会议时间</div>
          <div>会议简介</div>
          <div>参会人员</div>
          <div>操作</div>
          <div>操作</div>
        </div>
        <div style="width:100%;height:auto;">
          {lst.zipWithIndex.map { l =>
          val bgdColor = if (l._2 % 2 == 1) "background-color:rgba(242,245,250,1)" else "background-color:rgba(255,255,255,1)"
          val item = l._1
          <div class="list-tr initiate" style={bgdColor}>
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
            <div>
              <button onclick={() => ()}>用户管理</button>
            </div>
            <div>
              <button onclick={() => ()}>评论管理</button>
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
