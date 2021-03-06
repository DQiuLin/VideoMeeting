package videomeeting.webClient.pages

import mhtml.{Rx, Var}
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Video
import org.scalajs.dom.raw.HTMLElement
import videomeeting.protocol.ptcl.CommonInfo._
import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol._
import videomeeting.webClient.common.Page
import videomeeting.webClient.common.Routes.MeetingRoutes
import videomeeting.webClient.util.{Http, JsFunc, TimeTool}
import videomeeting.protocol.ptcl.CommonInfo

import concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

/**
  * created by dql on 2020/1/19
  * web我邀请的参会人员页面
  */
class InvitePage extends Page {

  val meetList: Var[List[InviteMeetingInfo]] = Var(Nil)
  val totalCount: Rx[Int] = meetList.map(_.length)
  val videoPlay: Var[Long] = Var(-1)

  def init() = {
    videoPlay := -1
  }

  def getList(): Unit = {
    val uid: Int = dom.window.localStorage.getItem("userId").toInt
    val url = MeetingRoutes.getInviteList(uid)
    Http.getAndParse[InviteRsp](url).map {
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
        pList += l.name + " "
        l.name
      }
    }
    pList
  }

  val meetTable = meetList.map { lst =>
    if (lst.isEmpty)
      <div class="list-table">
        <div class="list-th invite">
          <div>会议视频</div>
          <div>会议名称</div>
          <div>会议时间</div>
          <div>会议简介</div>
          <div>参会人员</div>
        </div>
      </div>
    else
      <div class="list-table">
        <div class="list-th invite">
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
          <div class="list-tr invite" style={bgdColor}>
            <div>
              <button onclick={() => MainPage.goRecord(item.id.toString, item.video.getOrElse(""), item.meetInfo.name, item.meetInfo.time.toString)}>查看视频</button>
            </div>
            <div>
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
          </div>
        }}
        </div>
      </div>
  }

  override def render: Elem = {
    if (dom.window.localStorage.getItem("userId") == null) {
      MainPage.goHome()
    }
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
