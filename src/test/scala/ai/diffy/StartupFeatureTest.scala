package ai.diffy

import java.net.ServerSocket

import com.google.inject.Stage
import ai.diffy.proxy.DifferenceProxy
import com.twitter.finagle.Http
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.Test
import com.twitter.util.{Await, Future, FuturePool}
import com.twitter.util.TimeConversions._
import org.jboss.netty.handler.codec.http.HttpResponseStatus

class StartupFeatureTest extends Test {

  def getPort(): Int = {
    val s  = new ServerSocket(0)
    val port = s.getLocalPort
    s.close()
    port
  }

  val env@Seq(p,s,c,d) = Seq.fill(4)(getPort())
  val environment = FuturePool.unboundedPool(ExampleServers.main(env.take(3).map(_.toString).toArray))

  val diffy = new MainService
  lazy val differenceProxy = diffy.injector.instance[DifferenceProxy]

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = diffy,
    extraArgs = Seq(
      s"-proxy.port=:$d",
      s"-candidate=localhost:$c",
      s"-master.primary=localhost:$p",
      s"-master.secondary=localhost:$s",
      "-service.protocol=http"))

  "verify startup" in {
    server.assertHealthy()
  }

  "verify DifferenceCollector" in {
    assert(differenceProxy.collector.fields.isEmpty)
    Await.result(Http.fetchUrl(s"http://localhost:$d/json?Twitter").liftToTry)
    var tries = 0
    while(differenceProxy.outstandingRequests.get() > 0 && tries < 10) {
      Await.result(Future.sleep(1.seconds)(DefaultTimer.twitter))
      tries = tries + 1
    }
    assert(!differenceProxy.collector.fields.isEmpty)
  }

  "verify present differences via API" in {
    val response =
      Await.result(Http.fetchUrl(s"http://${server.externalHttpHostAndPort}/api/1/endpoints/undefined_endpoint/stats"))
    assertResult(HttpResponseStatus.OK)(response.getStatus)
    assert(new String(response.getContent.array()).contains(""""differences":1"""))
  }

  "verify absent endpoint in API" in {
    val response =
      Await.result(Http.fetchUrl(s"http://${server.externalHttpHostAndPort}/api/1/endpoints/json/stats"))
    assertResult(HttpResponseStatus.OK)(response.getStatus)
    assertResult("""{"error":"key not found: json"}""")(new String(response.getContent.array()))
  }
}
