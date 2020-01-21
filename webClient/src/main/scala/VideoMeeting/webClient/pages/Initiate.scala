package VideoMeeting.webClient.pages


import VideoMeeting.webClient.common.Components.{Modal, ModalLarge}
import mhtml.{Rx, Var}
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

  val meetList: Var[List[InitiateMeetingInfo]] = Var(Nil)
  val totalCount: Rx[Int] = meetList.map(_.length)
  val videoPlay: Var[Long] = Var(-1)

  def init() = {
    videoPlay := -1
  }

  def getList(): Unit = {

  }

  def peopleManageModal(meetId: Long, list: List[PeopleInfo]): Unit = {
    val title = "会议可见人员管理"
    val pList: Var[List[PeopleInfo]] = Var(list)
    val body =
      <div class="modal-body">
        <div class="modal-add">
          <button onclick={() => ()}>+邀请</button>
        </div>{pList.map { lst =>
        if (lst.isEmpty)
          <div class="modal-table">
            <div class="list-th modal">
              <div>用户名</div>
              <div>类型</div>
              <div>操作</div>
            </div>
          </div>
        else
          <div class="modal-table">
            <div class="list-th modal">
              <div>用户名</div>
              <div>类型</div>
              <div>操作</div>
            </div>
            <div style="width:100%;height:auto;">
              {lst.zipWithIndex.map { l =>
              val bgdColor = if (l._2 % 2 == 1) "background-color:rgba(242,245,250,1)" else "background-color:rgba(255,255,255,1)"
              val item = l._1
              <div class="list-tr modal" style={bgdColor}>
                <div>
                  {item.name}
                </div>
                <div>
                  {item.pType}
                </div>
                <div>
                  <button onclick={() => ()}>取消邀请</button>
                </div>
              </div>
            }}
            </div>
          </div>
      }}
      </div>
    ModalLarge(title, body, 400, 500, () => ())
  }

  def commentManageModal(meetId: Long, list: List[CommentInfo]): Unit = {
    val title = "评论管理"
    val cList: Var[List[CommentInfo]] = Var(list)
    val body =
      <div class="modal-body">
        {cList.map { lst =>
        if (lst.isEmpty)
          <div class="modal-table">
            <div class="list-th modal">
              <div>用户名</div>
              <div>评论</div>
              <div>时间</div>
              <div>操作</div>
            </div>
          </div>
        else
          <div class="modal-table">
            <div class="list-th modal">
              <div>用户名</div>
              <div>时间</div>
              <div>评论</div>
              <div>操作</div>
            </div>
            <div style="width:100%;height:auto;">
              {lst.zipWithIndex.map { l =>
              val bgdColor = if (l._2 % 2 == 1) "background-color:rgba(242,245,250,1)" else "background-color:rgba(255,255,255,1)"
              val item = l._1
              <div class="list-tr modal" style={bgdColor}>
                <div>
                  {item.usrName}
                </div>
                <div>
                  {item.time}
                </div>
                <div>
                  {item.content}
                </div>
                <div>
                  <button onclick={() => ()}>删除</button>
                </div>
              </div>
            }}
            </div>
          </div>
      }}
      </div>
    ModalLarge(title, body, 400, 500, () => ())
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
              <button onclick={() => peopleManageModal(item.id, item.meetInfo.people)}>用户管理</button>
            </div>
            <div>
              <button onclick={() => commentManageModal(item.id, item.meetInfo.comment)}>评论管理</button>
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
