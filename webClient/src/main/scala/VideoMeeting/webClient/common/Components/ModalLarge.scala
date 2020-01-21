package VideoMeeting.webClient.common.Components

import mhtml.mount
import org.scalajs.dom

import scala.xml.Elem

/**
  * 模态框，增加宽度
  */
class ModalLarge(title:String, body:Elem, minHeight:Int, minWidth:Int, closeFunc: () => Unit) {

  val modalDom =
    <div id="modaldom" class="modal">
      <div class="modalbox" style={s"min-height:${minHeight}px; min-width:${minWidth}px;"}>
        <div style="background-color:#F2F5FA;height:44px;border-radius:8px 8px 0 0;">
          <span style="font-size: 16px;color: #1D2341;letter-spacing: 0.19px;line-height:44px;float:left;margin-left:18px;">
            {s"$title"}
          </span>
          <span style="color:#8E98B4;line-height:44px;float:right;margin-right:17px;font-size:17px;cursor:pointer;" onclick={() => {
            closeFunc();
            hide()
          }}>
            X
          </span>
        </div>{body}
      </div>
    </div>

  def hide(): Unit = {
    dom.document.body.removeChild(dom.document.getElementById("modaldom"))
  }

  mount(dom.document.body, modalDom)

}


object ModalLarge {

  def apply(title: String, body: Elem, minHeight: Int, minWidth: Int, closeFunc: () => Unit): ModalLarge = new ModalLarge(title, body, minHeight, minWidth, closeFunc)

  def hide(): Unit = {
    val modal = dom.document.getElementById("modaldom")
    if (modal != null)
      dom.document.body.removeChild(modal)
  }
}
