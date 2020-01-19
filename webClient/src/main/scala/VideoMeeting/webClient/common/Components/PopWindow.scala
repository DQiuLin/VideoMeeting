package VideoMeeting.webClient.common.Components

import VideoMeeting.webClient.pages.MainPage
import mhtml.{Var, emptyHTML}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input

import scala.xml.Elem

object PopWindow {

  val showPop = Var(emptyHTML)
  val loginButton = Var(<div class="pop-button" onclick={(e: Event) => MainPage.login(e, "pop-login")}>GO</div>)
  val registerButton = Var(<div class="pop-button" onclick={(e: Event) => MainPage.register(e, "pop-register")}>GO</div>)

  //防止弹窗消失
  def stopCancel(e: Event, id: String): Unit = {
    //stopPropagation防止事件冒泡
    e.stopPropagation()
    dom.document.getElementById(id).setAttribute("disabled", "")
  }

  //生成令弹窗消失的必须条件（用于解除防止弹窗消失）
  def closeReport(e: Event, id: String): Unit = {
    e.stopPropagation()
    dom.document.getElementById(id).removeAttribute("disabled")
  }

  //使用input标签for属性的弹窗的关闭方法（因为input标签可以自己检测到点击事件，此方法主要用于在不允许点击关闭的情况下自动关闭或强制关闭）
  def closePop(e: Event, id: String): Unit = {
    closeReport(e, id)
    dom.document.getElementById(id).asInstanceOf[Input].checked = false
  }

  /** 这里使用了input标签和for属性来生成弹窗。相对于使用Var的方式（手机端生成弹窗的方式）生成的弹窗，它可以不使用Js而使用css样式伪类checked来制作页面动画。
    * 但是其弹窗上的元素交互功能写起来会相对麻烦，所以对于静止的和有交互的弹窗不推荐使用（推荐手机端的方式生成弹窗）
    */
  def commonPop(text: String): Unit = {
    showPop := {
      <div>
        <input id="pop-common" style="display: none;" type="checkbox" checked="checked"></input>
        <label class="pop-background" for="pop-common" style="z-index: 3;">
          <div class="pop-main" onclick={(e: Event) => stopCancel(e, "pop-common")}>
            <div class="pop-header"></div>
            <div class="pop-content">
              <div class="pop-text">
                {text}
              </div>
            </div>
            <div class="pop-confirm">
              <div class="pop-button" onclick={(e: Event) => closeReport(e, "pop-common")}>确认</div>
            </div>
          </div>
        </label>
      </div>
    }
  }

  // 'for' is 'pop-login'
  def loginPop: Elem =
    <div>
      <input id="pop-login" style="display: none;" type="checkbox"></input>
      <label class="pop-background" for="pop-login" onclick={(e: Event) => closeReport(e, "pop-login")}>
        <div class="pop-main" onclick={(e: Event) => stopCancel(e, "pop-login")}>
          <div class="pop-header"></div>
          <div class="pop-title">用户登录</div>
          <div class="pop-content">
            <input class="pop-input" id="login-account" placeholder="用户名"></input>
            <input class="pop-input" type="password" id="login-password" placeholder="密码"></input>
          </div>
          <label class="pop-tip" for="pop-emailLogin" onclick={(e: Event) => closePop(e, "pop-login")}>试试邮箱登录？</label>
          <div class="pop-confirm">
            {loginButton}
          </div>
        </div>
      </label>
    </div>


  def registerPop: Elem =
    <div>
      <input id="pop-register" style="display: none;" type="checkbox"></input>
      <label class="pop-background" for="pop-register" onclick={(e: Event) => closeReport(e, "pop-register")}>
        <div class="pop-main" onclick={(e: Event) => stopCancel(e, "pop-register")}>
          <div class="pop-header"></div>
          <div class="pop-title">用户注册</div>
          <div class="pop-content">
            <input class="pop-input" id="register-email" placeholder="邮箱"></input>
            <input class="pop-input" id="register-account" placeholder="注册用户名"></input>
            <input class="pop-input" id="register-password" type="password" placeholder="注册密码"></input>
            <input class="pop-input" id="register-password2" type="password" placeholder="确认密码"></input>
          </div>
          <div class="pop-confirm">
            {registerButton}
          </div>
        </div>
      </label>
    </div>
}
