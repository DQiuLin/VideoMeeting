package videomeeting.distributorPage.common

/**
 * Author: Jason
 * Date: 2019/10/23
 * Time: 11:55
 */
object Routes {

  private val base = "/theia/distributor"


  object AdminRoutes{

    private val urlAdmin = base + "/admin"

    val getAllLiveInfo: String = urlAdmin + "/getAllLiveInfo"

    val closeStream: String = urlAdmin + "/finishPull"


  }

}
