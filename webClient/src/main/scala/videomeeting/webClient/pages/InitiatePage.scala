package videomeeting.webClient.pages

import mhtml.{Rx, Var}
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.{Input, Video}
import org.scalajs.dom.raw.HTMLElement
import videomeeting.protocol.ptcl.CommonInfo._
import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol._
import videomeeting.webClient.common.Components.ModalLarge
import videomeeting.webClient.common.{Page, Routes}
import videomeeting.webClient.common.Routes.MeetingRoutes
import videomeeting.webClient.util.{Http, JsFunc, TimeTool}
import videomeeting.protocol.ptcl.CommonInfo

import concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

/**
  * created by dql on 2020/1/19
  * web我发起的会议列表页面
  */
class InitiatePage extends Page {

  val meetList: Var[List[InitiateMeetingInfo]] = Var(Nil)
  val totalCount: Rx[Int] = meetList.map(_.length)
  val videoPlay: Var[Long] = Var(-1)

  def init() = {
    videoPlay := -1
  }

  def getList(): Unit = {
    val uid: Int = dom.window.localStorage.getItem("userId").toInt
    val url = MeetingRoutes.getInitiateList(uid)
    Http.getAndParse[InitiateRsp](url).map {
      case Right(r) =>
        if (r.errCode != 0) {
          JsFunc.alert(s"${r.msg}")
        } else {
          meetList := r.meetingList.getOrElse(Nil)
        }
      case Left(e) =>
        println(s"$e")
    }
  }

  def peopleListToString(lst: List[CommonInfo.PeopleInfo]): String = {
    var pList: String = ""
    if (lst.nonEmpty) {
      lst.map { l =>
        pList += l.name
        l.name
      }
    }
    pList
  }

  def invite(meetId: Int): Unit = {
    val inviteId: Int = dom.window.localStorage.getItem("userId").toInt
    val invitedId = dom.document.getElementById("invitedId").asInstanceOf[Input].value.trim.toInt
    val url = Routes.MeetingRoutes.invite
    val data = AddInvite(inviteId, meetId, invitedId).asJson.noSpaces
    Http.postJsonAndParse[AddInviteRsp](url, data).map {
      case Right(rsp) =>
        if (rsp.errCode == 0) {
          getList()
        } else {
          JsFunc.alert("邀请失败！")
          println(rsp.msg)
        }
      case Left(error) =>
        println(s"parse error,$error")
    }
  }

  def removeInvite(meetId: Int, userId: Int): Unit = {
    val url = Routes.MeetingRoutes.deleteInvite
    val data = Remove(meetId, userId).asJson.noSpaces
    Http.postJsonAndParse[RemoveRsp](url, data).map {
      case Right(rsp) =>
        if (rsp.errCode == 0) {
          getList()
        } else {
          JsFunc.alert("移除失败！")
          println(rsp.msg)
        }
      case Left(error) =>
        println(s"parse error,$error")
    }
  }

  def peopleManageModal(meetId: Int, list: List[PeopleInfo]): Unit = {
    val title = "会议可见人员管理"
    val pList: Var[List[PeopleInfo]] = Var(list)
    val body =
      <div class="modal-body">
        <div class="modal-add">
          <input placeHolder="请输入邀请者id" class="modal-add" id="invitedId"></input>
          <button onclick={() => invite(meetId)}>+邀请</button>
        </div>{pList.map { lst =>
        if (lst.isEmpty)
          <div class="modal-table">
            <div class="modal-table-th people">
              <div>用户名</div>
              <div>类型</div>
              <div>操作</div>
            </div>
          </div>
        else
          <div class="modal-table">
            <div class="modal-table-th people">
              <div>用户名</div>
              <div>类型</div>
              <div>操作</div>
            </div>
            <div style="width:100%;height:auto;">
              {lst.zipWithIndex.map { l =>
              val bgdColor = if (l._2 % 2 == 1) "background-color:rgba(242,245,250,1)" else "background-color:rgba(255,255,255,1)"
              val item = l._1
              <div class="modal-table-tr people" style={bgdColor}>
                <div>
                  {item.name}
                </div>
                <div>
                  {if (item.pType == 0) "被邀请人员"
                else "参会人员"}
                </div>
                <div>
                  <button onclick={() => removeInvite(meetId, item.id)}>取消邀请</button>
                </div>
              </div>
            }}
            </div>
          </div>
      }}
      </div>
    ModalLarge(title, body, 400, 500, () => ())
  }

  def deleteComment(commentId: Int): Unit = {
    val url = Routes.MeetingRoutes.deleteComment
    val data = Delete(commentId).asJson.noSpaces
    Http.postJsonAndParse[DeleteRsp](url, data).map {
      case Right(rsp) =>
        if (rsp.errCode == 0) {
          getList()
        } else {
          JsFunc.alert("删除失败！")
          println(rsp.msg)
        }
      case Left(error) =>
        println(s"parse error,$error")
    }
  }

  def commentManageModal(meetId: Long, list: List[CommentInfo]): Unit = {
    val title = "评论管理"
    val cList: Var[List[CommentInfo]] = Var(list)
    val body =
      <div class="modal-body">
        {cList.map { lst =>
        if (lst.isEmpty)
          <div class="modal-table">
            <div class="modal-table-th comment">
              <div>用户名</div>
              <div>评论</div>
              <div>时间</div>
              <div>操作</div>
            </div>
          </div>
        else
          <div class="modal-table">
            <div class="modal-table-th comment">
              <div>用户名</div>
              <div>时间</div>
              <div>评论</div>
              <div>操作</div>
            </div>
            <div style="width:100%;height:auto;">
              {lst.zipWithIndex.map { l =>
              val bgdColor = if (l._2 % 2 == 1) "background-color:rgba(242,245,250,1)" else "background-color:rgba(255,255,255,1)"
              val item = l._1
              <div class="modal-table-tr comment" style={bgdColor}>
                <div>
                  {item.usrName}
                </div>
                <div>
                  {TimeTool.dateFormatDefault(item.time)}
                </div>
                <div>
                  {item.content}
                </div>
                <div>
                  <button onclick={() => deleteComment(item.id)}>删除</button>
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
              {TimeTool.dateFormatDefault(item.meetInfo.time)}
            </div>
            <div>
              {item.meetInfo.intro}
            </div>
            <div>
              {peopleListToString(item.meetInfo.people)}
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

  override def render: Elem = {
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
