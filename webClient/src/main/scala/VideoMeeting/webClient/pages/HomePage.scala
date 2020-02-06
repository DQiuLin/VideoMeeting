package VideoMeeting.webClient.pages

import VideoMeeting.webClient.common.Page

import scala.xml.Elem

/**
  * created by dql on 2020/2/6
  * web首页
  */

class HomePage extends Page {

  override def render: Elem =
    <div>
      <div>我发起的</div>
      <div>我参与的</div>
      <div>邀请我的</div>
    </div>
}
