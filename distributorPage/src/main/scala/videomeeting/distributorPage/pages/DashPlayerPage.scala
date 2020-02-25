package videomeeting.distributorPage.pages

import videomeeting.distributorPage.common.Page

import scala.xml.Elem

/**
 * Author: Jason
 * Date: 2019/10/25
 * Time: 9:56
 */

class DashPlayerPage(val src: String) extends Page{
  override val locationHashString: String = "#/dashPlayer"

//  private val src = "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd"

  override def render: Elem = {
    <div style="text-align: center">
      <h1>Adaptive Streaming with HTML5</h1>
      <button onclick={s"setupVideo('$src')"}>play</button>
      <video id="videoplayer" ></video>
    </div>
  }
}
