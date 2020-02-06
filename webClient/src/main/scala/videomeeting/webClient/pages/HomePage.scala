package videomeeting.webClient.pages

import videomeeting.webClient.common.Page
import videomeeting.webClient.util.JsFunc
import org.scalajs.dom

import scala.xml.Elem

/**
  * created by dql on 2020/2/6
  * web首页
  */

class HomePage extends Page {

  override def render: Elem =
    <div class="web-first">
      <div class="web-first-item">
        <button onclick={() => if (dom.window.localStorage.getItem("userId") != null) MainPage.goInitiate() else JsFunc.alert("请先登录！")}>我发起的</button>
      </div>
      <div class="web-first-item">
        <button onclick={() => if (dom.window.localStorage.getItem("userId") != null) MainPage.goAttend() else JsFunc.alert("请先登录！")}>我参与的</button>
      </div>
      <div class="web-first-item">
        <button onclick={() => if (dom.window.localStorage.getItem("userId") != null) MainPage.goInvite() else JsFunc.alert("请先登录！")}>邀请我的</button>
      </div>
    </div>
}
