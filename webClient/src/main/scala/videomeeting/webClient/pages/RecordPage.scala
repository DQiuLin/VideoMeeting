package videomeeting.webClient.pages

import mhtml._
import io.circe.syntax._
import io.circe.generic.auto._

import scala.xml.{Elem, Node}
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom
import org.scalajs.dom.html._
import videomeeting.protocol.ptcl.CommonInfo.CommentInfo
import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol.{Comment, CommentRsp, GetCommentRsp}
import videomeeting.webClient.common.Components.PopWindow
import videomeeting.webClient.common.{Page, Routes}
import videomeeting.webClient.util.{Http, JsFunc, TimeTool}

/**
  * created by dql on 2020/2/11
  * web查看会议视频和评论页面
  */
class RecordPage(meetingId:String,videoUrl:String,videoName:String,videoTime:String) extends Page {

  private val mp4Url = Var(videoUrl)
  private val mp4Name = Var(videoName)
  private val mp4Time = Var(videoTime)

  private val userId = dom.window.localStorage.getItem("userId").toInt
  private val userName = dom.window.localStorage.getItem("userName")
  private val userHeaderImgUrl = dom.window.localStorage.getItem("userHeaderImgUrl")
  val headImg = Var(userHeaderImgUrl)
  val commentInfo = Var(List.empty[CommentInfo])

  def videoOnEnded(): Unit = {
    val menu = dom.document.getElementById("playback-menu")
    val video = dom.document.getElementById("recordVideo").asInstanceOf[Video]
    menu.setAttribute("class", "playback-menu-close")
    if (video != null) {
      video.load()
      video.play()
    }
  }

  def videoPlayback(): Unit = {
    val menu = dom.document.getElementById("playback-menu")
    menu.setAttribute("class", "playback-menu-open")
  }

  def sendComment(): Unit = {
    val b_area = dom.document.getElementById("ipt-txt").asInstanceOf[TextArea]
    val currentTime = System.currentTimeMillis()
    if (b_area.value.length != 0) {
      val data = Comment(meetingId.toInt, userId, b_area.value, currentTime).asJson.noSpaces
      Http.postJsonAndParse[CommentRsp](Routes.MeetingRoutes.comment, data).map {
        case Right(rsp) =>
          if (rsp.errCode == 0) {
            commentInfo.update(c => c :+ CommentInfo(-1, userName, userHeaderImgUrl, currentTime, b_area.value))
            b_area.value = ""
          } else {
            PopWindow.commonPop(rsp.msg)
          }
        case Left(e) =>
          println("error happen: " + e)
      }
    }
  }

  def getCommentInfo(): Unit = {
    val url = Routes.MeetingRoutes.getCommentList(meetingId.toInt)
    Http.getAndParse[GetCommentRsp](url).map {
      case Right(r) =>
        if (r.errCode != 0) {
          JsFunc.alert(s"${r.msg}")
        } else {
          commentInfo := r.commentList.getOrElse(Nil)
        }
      case Left(e) =>
        println(s"$e")
    }
  }

  val comments: Rx[Node] = commentInfo.map { cf =>
    def createCommentItem(item: CommentInfo): Elem = {
      <div class="rcl-item">
        <div class="user-face">
          <img class="userface" src={item.headImg}></img>
        </div>
        <div class="rcl-con">
          <div class="rcl-con-name">
            {item.usrName}
          </div>
          <div class="rcl-con-con">
            {item.content}
          </div>
          <div class="rcl-con-time">
            {TimeTool.dateFormatDefault(item.time)}
          </div>
        </div>
      </div>
    }

    <div class="comment-list">
      {cf.map(createCommentItem)}
    </div>
  }

  override def render: Elem = {
    getCommentInfo()
    <div>
      <div class="audienceInfo" style="margin-left:250px;margin-top:20px;width:60%">
        <div class="anchorInfo">
          <div class="showInfo">
            <div style="margin-left:10px;color:#222">
              <div class="recordName">
                {mp4Name}
              </div>
              <div class="recordTime" style="color: #808080;font-size: 12px;margin-top: 10px;">
                {mp4Time.map(i => TimeTool.dateFormatDefault(i.toLong))}
              </div>
            </div>
          </div>
        </div>
        <div style="padding-bottom:20px!important" class="dash-video-player anchor-all" id="dash-video-player">
          <div style="position: relative">
            <video id="recordVideo" controls="controls" style="height:500px;width:100%;object-fit:contain;background-color:#000;" onended={() => videoOnEnded()}>
              <source src={mp4Url} type="video/mp4"></source>
            </video>
            <div id="playback-menu" class="playback-menu-close">
              <div class="playback-point">
                <img class="playback-button" src="/videomeeting/meetingManager/static/img/homePage/replay.png" onclick={() => videoPlayback()}></img>
                <div class="playback-text">重新播放</div>
              </div>
            </div>
          </div>
        </div>

        <div class="r-comment" id="r-comment">
          <div class="rc-head">全部评论(
            {commentInfo.map { ci =>
            if (ci.isEmpty) {
              0
            } else {
              ci.length
            }
          }}
            )</div>
          <div class="rc-content">
            <div class="comment-send">
              <div class="user-face">
                <img class="userface" src={headImg}></img>
              </div>
              <div class="textarea-container">
                <textarea cols="80" name="msg" rows="5" placeholder="请自觉遵守互联网相关的政策法规，严禁发布色情、暴力、反动的言论。" class="ipt-txt" id="ipt-txt"
                          onkeydown={(e: dom.KeyboardEvent) => if (e.keyCode == 13) sendComment()}></textarea>
                <div class="rsb-button">
                  <button type="submit" class="comment-submit" id="comment-submit" onclick={() => sendComment()}>发表评论</button>
                </div>
              </div>
            </div>{comments}
          </div>
        </div>

      </div>
    </div>
  }

}