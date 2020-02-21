package videomeeting.distributorPage.common

import scala.xml.Elem

/**
 * Author: Jason
 * Date: 2019/10/23
 * Time: 11:53
 */
trait Component {

  def render: Elem

}

object Component {
  implicit def component2Element(comp: Component): Elem = comp.render
}
