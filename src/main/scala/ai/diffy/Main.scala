package ai.diffy

import ai.diffy.proxy._
import ai.diffy.workflow._
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter

object Main extends MainService

class MainService extends HttpServer {
  override val name = "diffy"

  override val modules =
    Seq(
      DiffyServiceModule,
      DifferenceProxyModule,
      TimerModule
    )

  override def configureHttp(router: HttpRouter): Unit = {
    val proxy: DifferenceProxy = injector.instance[DifferenceProxy]
    proxy.server

    val workflow: Workflow = injector.instance[FunctionalReport]
    val stats: DifferenceStatsMonitor = injector.instance[DifferenceStatsMonitor]

    stats.schedule()
    workflow.schedule()

    router
      .filter[AllowLocalAccess]
      .add[ApiController]
      .add[Frontend]
  }
}
