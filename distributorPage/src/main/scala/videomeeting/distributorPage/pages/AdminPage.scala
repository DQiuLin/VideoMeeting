package videomeeting.distributorPage.pages

import io.circe.generic.auto._
import io.circe.syntax._
import mhtml.Var
import org.scalajs.dom
import videomeeting.distributorPage.Main
import videomeeting.distributorPage.common.{Page, Routes}
import videomeeting.distributorPage.common.Routes.AdminRoutes
import videomeeting.distributorPage.util.{Http, JsFunc}
import videomeeting.protocol.ptcl.distributor2Manager.DistributorProtocol.{CloseStreamReq, CloseStreamRsp, GetAllLiveInfoReq, GetAllLiveInfoRsp, liveInfo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem

/**
 * Author: Jason
 * Date: 2019/10/23
 * Time: 15:10
 */

object AdminPage extends Page{
  override protected val locationHashString: String = "#/admin"

  private val allInfo: Var[List[liveInfo]] = Var(List.empty)

  private var loopId = -1

  private var btnFlag = Var(false)

  def getAllLiveInfo: Future[Unit] = {
    val url = AdminRoutes.getAllLiveInfo
    val data = GetAllLiveInfoReq().asJson.noSpaces
    Http.postJsonAndParse[GetAllLiveInfoRsp](url, data).map {
      rsp =>
        try {
          if (rsp.errCode == 0) {
            allInfo := rsp.info
            println(s"got it : ${rsp.info.length}")
          }
          else {
            println("error======" + rsp.msg)
            JsFunc.alert(rsp.msg)
          }
        }
        catch {
          case e: Exception =>
            println(e)
        }
    }
  }

  def closeStream(liveId: String): Future[Unit] = {
    val url = Routes.AdminRoutes.closeStream
    val data = CloseStreamReq(liveId).asJson.noSpaces
    Http.postJsonAndParse[CloseStreamRsp](url, data).map {
      rsp =>
        try {
          if (rsp.errCode == 0) {
            println(s"got it : ${rsp.info}")
          }
          else {
            println("error======" + rsp.msg)
            JsFunc.alert(rsp.msg)
          }
        }
        catch {
          case e: Exception =>
            println(e)
        }
    }
  }

  private val allInfoRx = allInfo.map{
    case Nil => <div style ="margin: 30px; font-size: 17px; text-align: center">暂无记录
    </div>
    case list => <div style ="margin: 20px; font-size: 17px; text-align: center">
      <table class="table fadeInUp" id ="article" style ="width: 100%; border-collapse:collapse; border: solid;border-width:1px; text-align: center">
        <thead>
          <tr style="border: solid;border-width:1px;">
            <th class="table-active rounded">RoomId</th>
            <th class="table-success rounded">LiveId</th>
            <th class="table-primary rounded">port</th>
            <th class="table-secondary rounded">status</th>
            <th class="table-secondary rounded">url</th>
            <th class="table-secondary rounded">shut down btn</th>
          </tr>
        </thead>
        <tbody>
          {list.map { l =>
          <tr style="border: solid;border-width:1px;">
            <td class="table-active rounded">{l.roomId}</td>
            <td class="table-active rounded">{l.liveId}</td>
            <td class="table-active rounded">{l.port}</td>
            <td class="table-active rounded">{l.status}</td>
            <td class="table-active rounded"><a onclick={() => Main.setSrc(l.Url);stopLoop()} href="#/dashPlayer">{l.Url}</a></td>
            <td class="table-active rounded">
              <button onclick={() => closeStream(l.liveId);()}>close</button>
            </td>
          </tr>
        }
          }
        </tbody>
      </table>
    </div>
  }

  private val btnRx = btnFlag.map (
    b =>
      if (b) <button onclick={() => {stopLoop()}}>Stop</button>
      else <button onclick={() => {startLoop()}}>Start</button>
  )

  def startLoop(): Unit = {
    loopId = dom.window.setInterval(() => getAllLiveInfo, 2000)
    btnFlag.update(f => !f)
  }

  def stopLoop(): Unit = {
    dom.window.clearInterval(loopId)
    btnFlag.update(f => !f)
  }

  override def render: Elem = {
    <div>
      {btnRx}
      <h1 style="text-align: center">All Info</h1>
      {allInfoRx}
    </div>
  }
}
