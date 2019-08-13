package ai.diffy

import ai.diffy.proxy._
import ai.diffy.workflow._
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter

object Main extends MainService

class MainService extends HttpServer {
  //Set root log level to INFO to suppress chatty libraries
  org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[ch.qos.logback.classic.Logger]
    .setLevel(ch.qos.logback.classic.Level.INFO)

  override val name = "diffy"

  override val modules =
    Seq(
      IsotopeSdkModule,
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