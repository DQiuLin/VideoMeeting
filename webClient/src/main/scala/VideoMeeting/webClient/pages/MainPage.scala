package VideoMeeting.webClient.pages

import VideoMeeting.webClient.common.Components.PopWindow
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input

/**
  * created by dql on 2020/1/19
  * web界面首页
  */
object MainPage {

  //用户信息协议，暂时写在前端-----
  case class User(id: Long, name: String, password: String, avatar: String)

  //-----

  val isLogin = Var(0)

  private val headerBox = isLogin.map {
    case 0 =>
      <div class="first-header">
        <button>登录</button>
        <button>注册</button>
      </div>
    case 1 =>
      <div class="first-header">
        <img src=""></img>
        <div class="first-header-name">huahua</div>
        <button>登出</button>
      </div>
    case _ =>
      <div class="first-header">
        <button>登录</button>
        <button>注册</button>
      </div>
  }

  def show(): Unit = {
    <div class="first-bg">
      {headerBox}<div calss="first-content">
      <div>我发起的</div>
      <div>我参会的</div>
      <div>邀请我的</div>
    </div>
    </div>
  }

  //function
  def register(e: Event, popId: String): Unit = {
    val account = dom.document.getElementById("register-account").asInstanceOf[Input].value
    val password = dom.document.getElementById("register-password").asInstanceOf[Input].value
    val password2 = dom.document.getElementById("register-password2").asInstanceOf[Input].value
    if (!account.trim.equals("") && !password.trim.equals("") && !password2.trim.equals("")) {
      if (password.equals(password2)) {


      }
      else {
        PopWindow.commonPop("输入相同的密码！")
      }
    } else {
      PopWindow.commonPop("注册项均不能为空！")
    }
  }

  def login(e: Event, popId: String): Unit = {
    val account = dom.document.getElementById("login-account").asInstanceOf[Input].value
    val password = dom.document.getElementById("login-password").asInstanceOf[Input].value

  }
}
