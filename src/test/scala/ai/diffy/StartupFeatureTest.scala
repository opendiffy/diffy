package ai.diffy

import java.net.ServerSocket

import ai.diffy.proxy.DifferenceProxy
import com.google.common.collect.ImmutableMap
import com.google.inject.Stage
import com.twitter.finagle.Http
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.Test
import com.twitter.util.TimeConversions._
import com.twitter.util.{Await, Future, FuturePool}
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
    twitterServer = diffy,
    flags = ImmutableMap.of[String, String](
      "proxy.port", s":$d",
      "candidate", s"localhost:$c",
      "master.primary", s"localhost:$p",
      "master.secondary", s"localhost:$s",
      "service.protocol", "http"
    ),
    stage = Stage.PRODUCTION
  )

  test("verify startup") {
    server.assertHealthy()
  }

  test("verify DifferenceCollector") {
    assert(differenceProxy.collector.fields.isEmpty)
    Await.result(Http.fetchUrl(s"http://localhost:$d/json?Twitter").liftToTry)
    var tries = 0
    while(differenceProxy.outstandingRequests.get() > 0 && tries < 10) {
      Await.result(Future.sleep(1.seconds)(DefaultTimer.twitter))
      tries = tries + 1
    }
    assert(!differenceProxy.collector.fields.isEmpty)
  }

  test("verify present differences via API") {
    val response =
      Await.result(Http.fetchUrl(s"http://${server.externalHttpHostAndPort}/api/1/endpoints/undefined_endpoint/stats"))
    assertResult(HttpResponseStatus.OK.getCode)(response.getStatusCode())
    assert(response.getContentString().contains(""""differences":1"""))
  }

  test("verify absent endpoint in API") {
    val response =
      Await.result(Http.fetchUrl(s"http://${server.externalHttpHostAndPort}/api/1/endpoints/json/stats"))
    assertResult(HttpResponseStatus.OK.getCode)(response.getStatusCode)
    assertResult("""{"error":"key not found: json"}""")(response.getContentString())
  }
}
