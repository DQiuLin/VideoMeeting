package VideoMeeting.webClient

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import VideoMeeting.webClient.pages.MainPage

/**
  * create by zhaoyin
  * 2019/7/17  2:35 PM
  */

@JSExportTopLevel("front.Main")
object Main {
  def main(args: Array[String]): Unit = {
    run()
  }

  @JSExport
  def run(): Unit = {
    MainPage.show()
  }
}
