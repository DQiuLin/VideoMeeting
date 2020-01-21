package VideoMeeting.webClient.common.Components

import mhtml.mount
import org.scalajs.dom
import org.scalajs.dom.html.Button

import scala.xml.Elem

/**
  *模态框
  */
class Modal(title:String, body:Elem, confirmStr:String, minHeight:Int, minWidth:Int, successFun:()=>Unit, cancelFunc: () => Unit) {

  val modalDom =
    <div id="modaldom" class="modal">
      <div class="modalbox" style={s"min-height:${minHeight}px; width:${minWidth}px; margin:12.25% 33.33%"}>
        <div style="background-color:#F2F5FA;height:44px;border-radius:8px 8px 0 0;">
          <span style="font-size: 16px;color: #1D2341;letter-spacing: 0.19px;line-height:44px;float:left;margin-left:18px;">
            {s"$title"}
          </span>
          <span style="color:#8E98B4;line-height:44px;float:right;margin-right:17px;font-size:17px;cursor:pointer;" onclick={() => {
            hide()
          }}>
            X
          </span>
        </div>{body}<div class="modalbottom" style="text-align: center; position: relative; border-top: 1px solid #D9DFEB;">
        <button class="modalbutton" id="confirm-btn" style="background-color:#4D78FB;" onclick={() => {
          successFun()
        }}>
          <pre>
            {s"$confirmStr"}
          </pre>
        </button>
        <button class="modalbutton" style="color: #4D78FB;border: 1px solid #4D78FB;background-color:#FFFFFF;" onclick={() => {
          hide()
        }}>取 消</button>
      </div>
      </div>
    </div>

  def hide(): Unit = {
    dom.document.body.removeChild(dom.document.getElementById("modaldom"))
    if (cancelFunc != null)
      cancelFunc()
  }

  mount(dom.document.body, modalDom)

}


object Modal {

  def apply(title: String, body: Elem, confirmStr: String, minHeight: Int, minWidth: Int, successFun: () => Unit, cancelFunc: () => Unit = null): Modal = new Modal(title, body, confirmStr, minHeight, minWidth, successFun, cancelFunc)

  def disableConfirmBtn(): Unit = {
    val btn = dom.document.getElementById("confirm-btn").asInstanceOf[Button]
    btn.setAttribute("disabled", "disabled")
    btn.removeAttribute("style")
  }

  def ableConfirmBtn(): Unit = {
    val btn = dom.document.getElementById("confirm-btn").asInstanceOf[Button]
    btn.removeAttribute("disabled")
    btn.setAttribute("style", "background-color:#4D78FB;")
  }

  def hide(): Unit = {
    val modal = dom.document.getElementById("modaldom")
    if (modal != null)
      dom.document.body.removeChild(modal)
  }
}
