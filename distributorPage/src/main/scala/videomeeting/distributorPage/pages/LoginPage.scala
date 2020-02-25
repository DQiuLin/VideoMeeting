package videomeeting.distributorPage.pages

import org.scalajs.dom
import org.scalajs.dom.html.Input
import videomeeting.distributorPage.common.Page
import videomeeting.distributorPage.util.JsFunc

import scala.xml.Elem

/**
 * Author: Jason
 * Date: 2019/10/23
 * Time: 12:50
 */
object LoginPage extends Page {

  override val locationHashString: String = "#/login"

  private def userLogin(): Unit ={
    var userName= dom.document.getElementById("userName").asInstanceOf[Input].value
    var userPassword= dom.document.getElementById("userPassword").asInstanceOf[Input].value
    if ((userName,userPassword)==("xue","123")) dom.window.location.hash = "/admin"
    else JsFunc.alert("用户名或密码错误")


  }


  override def render: Elem = {
    <div>
      <div class="LoginForm">
        <h2>登陆</h2>
        <div class="inputContent">
          <span>用户名</span>
          <input id="userName"></input>
        </div>
        <div class="inputContent">
          <span>密码</span>
          <input id="userPassword" type="password"></input>
        </div>
        <button  onclick={() => userLogin()}>登陆</button>

      </div>
    </div>
  }



}
