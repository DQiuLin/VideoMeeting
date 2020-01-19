package VideoMeeting.webClient.pages

import mhtml.Var
import org.scalajs.dom.Event

/**
  * created by dql on 2020/1/19
  * web界面首页
  */
object MainPage {

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

  //  function
  def register(e: Event, popId: String): Unit = {

  }

  def login(e: Event, popId: String): Unit = {

  }
}
