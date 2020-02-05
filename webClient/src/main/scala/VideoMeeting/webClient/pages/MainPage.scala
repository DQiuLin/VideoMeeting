package VideoMeeting.webClient.pages

import mhtml._
import io.circe.syntax._
import io.circe.generic.auto._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import videomeeting.protocol.ptcl.client2Manager.http.CommonProtocol.{SignIn, SignInRsp, SignUp, SignUpRsp}
import VideoMeeting.webClient.common.Components.PopWindow
import VideoMeeting.webClient.common.PageSwitcher._
import VideoMeeting.webClient.common.Routes
import VideoMeeting.webClient.common.Routes.UserRoutes
import VideoMeeting.webClient.util.Http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

/**
  * created by dql on 2020/1/19
  * web界面首页
  */
object MainPage {
  //其他页面共用的数据都在localStorage里面
  var userShowName = if (dom.window.localStorage.getItem("userName") == null) Var("") else Var(dom.window.localStorage.getItem("userName"))
  var userShowImg = if (dom.window.localStorage.getItem("userHeaderImgUrl") == null) Var("") else Var(dom.window.localStorage.getItem("userHeaderImgUrl"))
  var showPersonCenter = Var(emptyHTML)

  private val exitButton: Elem =
    <div class="header-exit" onclick={() => dom.window.location.hash = "#/Home"}>
      <img src="/videomeeting/meetingManager/static/img/logo.png" title="主页"></img>
      <div>主页</div>
    </div>

  private val noUserShow: Elem =
    <div class="header-content-nologin">
      <label class="header-login" id="login" for="pop-login">登录</label>{PopWindow.loginPop}<label class="header-register" id="register" for="pop-register">注册</label>{PopWindow.registerPop}
    </div>

  private val userShow: Elem =
    <div class="header-content">
      <div style="display:flex">
        <div class="header-defaultimg">
          <img src={userShowImg} onclick={() =>
            showPersonCenter := PopWindow.personalCenter(dom.window.localStorage.getItem("userId").toLong,
              dom.window.localStorage.getItem("userName"))} id="userHeadImg"></img>
          <div class="header-user">
            {userShowName}
          </div>
        </div>{showPersonCenter}<div class="header-button" onclick={() => loginOut()}>登出</div>
      </div>
    </div>

  private val menuShow = if (dom.window.localStorage.getItem("userName") != null
    && dom.window.localStorage.getItem("isTemUser") == null) {
    Var(userShow)
  } else Var(noUserShow)

  private val exitShow = Var(emptyHTML)

  private val currentPage = {
    //todo 页面切换
  }

  def show(): Cancelable = {
    switchPageByHash()
    val page =
      <div>
        {PopWindow.showPop}<div class="header">
        {exitShow}{menuShow}
      </div>{currentPage}
      </div>
    mount(dom.document.body, page)
  }

  //function
  def register(e: Event, popId: String): Unit = {
    val account = dom.document.getElementById("register-account").asInstanceOf[Input].value
    val password = dom.document.getElementById("register-password").asInstanceOf[Input].value
    val password2 = dom.document.getElementById("register-password2").asInstanceOf[Input].value
    if (!account.trim.equals("") && !password.trim.equals("") && !password2.trim.equals("")) {
      if (password.equals(password2)) {
        PopWindow.registerButton := <img src="/videomeeting/meetingManager/static/img/loading.gif"></img>
        val data = SignUp(account, password).asJson.noSpaces
        Http.postJsonAndParse[SignUpRsp](UserRoutes.userRegister, data).map {
          case Right(rsp) =>
            if (rsp.errCode == 0) {
              PopWindow.commonPop(s"${rsp.msg}")
            } else {
              PopWindow.commonPop(s"error happened:${rsp.msg}")
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
    Http.postJsonAndParse[SignInRsp](Routes.UserRoutes.userLogin, data).map {
      case Right(rsp) =>
        if (rsp.errCode == 0) {
          //登陆之后获取到用户信息
          if (rsp.userInfo.isDefined) {
            dom.window.localStorage.setItem("userName", account)
            dom.window.localStorage.setItem("userHeaderImgUrl", rsp.userInfo.get.avatar)
            dom.window.localStorage.setItem("userId", rsp.userInfo.get.id.toString)
            val coverImgUrl = dom.window.sessionStorage.getItem("coverImgUrl")
            if (coverImgUrl != null) {
              dom.window.localStorage.setItem("coverImgUrl", coverImgUrl)
            }
            userShowName := dom.window.localStorage.getItem("userName")
            userShowImg := dom.window.localStorage.getItem("userHeaderImgUrl")
            //userInfo = rsp.userInfo.get
            dom.window.localStorage.removeItem("isTemUser")
            menuShow := userShow
          } else {
            println("don't get userInfo")
            PopWindow.commonPop(s"don't get userInfo")
          }
          PopWindow.loginButton := <div class="pop-button" onclick={(e: Event) => MainPage.login(e, "pop-login")}>GO</div>
          //refresh()
          PopWindow.closePop(e, popId)
        } else {
          PopWindow.commonPop(s"error happened: ${rsp.msg}")
        }
      case Left(error) =>
        PopWindow.commonPop(s"error: $error")
    }.foreach(_ => PopWindow.loginButton := <div class="pop-button" onclick={(e: Event) => MainPage.login(e, "pop-login")}>GO</div>)
  }

  def loginOut(): Unit = {
    menuShow := noUserShow

    dom.window.localStorage.removeItem("userName")
    dom.window.localStorage.removeItem("userHeaderImgUrl")
    dom.window.localStorage.removeItem("userId")

    val coverImgUrl = dom.window.localStorage.getItem("coverImgUrl")
    if (coverImgUrl != null) {
      dom.window.sessionStorage.setItem("coverImgUrl", coverImgUrl)
    }

    if (dom.window.localStorage.getItem("myRoomId") != null) {
      dom.window.localStorage.removeItem("myRoomId")
    }
    //refresh()
  }

  def refresh() = {

  }
}
