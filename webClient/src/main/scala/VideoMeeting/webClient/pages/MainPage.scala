package VideoMeeting.webClient.pages

import mhtml._
import io.circe.syntax._
import io.circe.generic.auto._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol.{SignIn, SignInRsp, SignUp, SignUpRsp}
import VideoMeeting.webClient.common.Components.PopWindow
import VideoMeeting.webClient.common.Routes
import VideoMeeting.webClient.util.Http

import scala.concurrent.ExecutionContext.Implicits.global

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
        <button>此处是img</button>
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
        PopWindow.registerButton := <img src=""></img>
        val data = SignUp(account, password).asJson.noSpaces
        Http.postFormAndParse[SignUpRsp](Routes.UserRoutes.userRegister, data).map {
          case Right(rsp) =>
            if (rsp.errCode == 0) {

            } else {

            }
          case Left(error) =>
            PopWindow.commonPop(s"error:$error")
        }.foreach(_ => PopWindow.registerButton := <div class="pop-button" onclick={(e: Event) => MainPage.register(e, "pop-register")}>GO</div>)

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
    val data = SignIn(account, password).asJson.noSpaces
    Http.postFormAndParse[SignInRsp](Routes.UserRoutes.userLogin, data).map {
      case Right(rsp) =>
        if (rsp.errCode == 0) {
          if (rsp.userInfo.isDefined) {

          } else {
            println("don't get userInfo")
            PopWindow.commonPop(s"don't get userInfo")
          }
        }
    }
  }
}
