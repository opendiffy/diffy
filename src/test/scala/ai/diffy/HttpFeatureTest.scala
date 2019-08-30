package ai.diffy

import java.net.ServerSocket

import ai.diffy.examples.http.ExampleServers
import ai.diffy.proxy.DifferenceProxy
import com.google.common.collect.ImmutableMap
import com.google.inject.Stage
import com.twitter.finagle.Http
import com.twitter.finagle.http.Status
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.Test
import com.twitter.util.{Await, Duration, Future, FuturePool}

class HttpFeatureTest extends Test {
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
    flags = Map(
      "proxy.port" -> s":$d",
      "candidate" -> s"localhost:$c",
      "master.primary" -> s"localhost:$p",
      "master.secondary" -> s"localhost:$s",
      "serviceName" -> "myHttpService",
      "service.protocol" -> "http",
      "summary.email" ->"test"
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
      Await.result(Future.sleep(Duration.fromSeconds(1))(DefaultTimer.twitter))
      tries = tries + 1
    }
    assert(!differenceProxy.collector.fields.isEmpty)
  }

  test("verify present differences via API") {
    val response =
      Await.result(Http.fetchUrl(s"http://${server.externalHttpHostAndPort}/api/1/endpoints/undefined_endpoint/stats"))
    assertResult(Status.Ok)(response.status)
    assert(response.getContentString().contains(""""differences":1"""))
  }

  test("verify absent endpoint in API") {
    val response =
      Await.result(Http.fetchUrl(s"http://${server.externalHttpHostAndPort}/api/1/endpoints/json/stats"))
    assertResult(Status.Ok)(response.status)
    assertResult("""{"error":"key not found: json"}""")(response.getContentString())
  }
  Seq(
    "/api/1/overview",
    "/api/1/report",
    "/api/1/endpoints",
    "/api/1/endpoints/json/stats",
    "/api/1/endpoints/json/fields/result.200.values.value.name.PrimitiveDifference/results",
    "/api/1/endpoints/json/fields/result.200.values.value.name.PrimitiveDifference/results/0"
  ) foreach { endpoint =>
    test(s"ping ${endpoint}") {
      val response =
        Await.result(Http.fetchUrl(s"http://${server.externalHttpHostAndPort}${endpoint}"))
      assertResult(Status.Ok)(response.status)
    }
  }
}
