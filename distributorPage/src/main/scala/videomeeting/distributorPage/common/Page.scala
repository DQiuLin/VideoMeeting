package videomeeting.distributorPage.common

import scala.xml.Elem

/**
 * Author: Jason
 * Date: 2019/10/23
 * Time: 11:53
 */
trait Page extends Component {

  protected val locationHashString: String = ""

}

object Page{
  def hashStr2Seq(str: String): IndexedSeq[String] = {
    if (str.length == 0) {
      IndexedSeq.empty[String]
    } else if (str.startsWith("#/")) {
      val t = str.substring(2).split("/").toIndexedSeq
      if (t.nonEmpty) {
        t
      } else IndexedSeq.empty[String]
    } else {
      throw new IllegalAccessException("error hash string:" + str + ". hash string must start with [#/]")
    }
  }
}

