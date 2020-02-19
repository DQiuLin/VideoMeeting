package videomeeting.pcClient.common

import javafx.scene.image.{Image, ImageView}
import videomeeting.pcClient.Boot.netImageProcessor
import videomeeting.pcClient.core.NetImageProcessor

import scala.collection.mutable
import org.slf4j.LoggerFactory

/**
  * Author: zwq
  * Date: 2019/8/23
  * Time: 15:41
  */
object Pictures {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  val pictureMap: mutable.HashMap[String, Image] = mutable.HashMap.empty // key-图片名

  var loadingImages: List[(String, ImageView)] = List.empty //key-完整hestiaUrl

  def getPic(picUrl: String, isHeader: Boolean = true): ImageView = {

    if (pictureMap.get(picUrl.split("/").last).nonEmpty) {
      val picCp = pictureMap(picUrl.split("/").last)
      new ImageView(picCp)
    } else {
//      log.debug(s"caches: $pictureMap")
      val pic = if (isHeader) new ImageView("img/header.png") else new ImageView("img/defaultCover.jpg")
      loadingImages = (picUrl, pic) :: loadingImages
      netImageProcessor ! NetImageProcessor.GetNetImage(picUrl)
      pic
    }
  }
}