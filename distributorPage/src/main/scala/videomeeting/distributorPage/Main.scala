package videomeeting.distributorPage

import mhtml.{Rx, mount}
import org.scalajs.dom
import videomeeting.distributorPage.common.PageSwitcher
import videomeeting.distributorPage.pages.{AdminPage, DashPlayerPage, LoginPage}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.xml.Node

/**
 * Author: Jason
 * Date: 2019/10/23
 * Time: 11:56
 */

@JSExportTopLevel("front.Main")
object Main extends PageSwitcher{
  def main(args: Array[String]): Unit ={
    run()
  }

  private var src =  "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd"

  def setSrc(src: String): Unit = this.src = src

  val currentPage: Rx[Node] = currentHashVar.map { ls =>
    println(s"currentPage change to ${ls.mkString(",")}")
    ls match {
      case "login" :: Nil => LoginPage.render
      case "admin" :: Nil => AdminPage.render
      case "dashPlayer" :: Nil => new DashPlayerPage(src).render
      case _ => LoginPage.render
    }

  }

  @JSExport
  def run(): Unit = {
    switchPageByHash()

    val page =
      <div >
        {currentPage}
      </div>
    mount(dom.document.body, page)
  }
}
