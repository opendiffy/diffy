package ai.diffy

import java.io.{File, FileOutputStream}
import java.net.ServerSocket
import java.nio.file.Files
import java.util.zip.{ZipEntry, ZipOutputStream}

import ai.diffy.examples.thrift.ExampleServers
import ai.diffy.proxy.DifferenceProxy
import ai.diffy.thriftscala.Adder
import com.google.inject.Stage
import com.twitter.finagle.http.Status
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finagle.{Http, ThriftMux}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.Test
import com.twitter.util.{Await, Duration, Future, FuturePool}

import scala.io.Source

class ThriftFeatureTest extends Test {

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


  val thriftFile = new File("src/test/thrift/example.thrift")
  val data = Source.fromInputStream(Files.newInputStream(thriftFile.toPath), "UTF-8").mkString
  val thriftJar = Files.createTempFile("thrift", "jar")
  thriftJar.toFile.deleteOnExit()
  val out = new ZipOutputStream(new FileOutputStream(thriftJar.toFile))
  out.putNextEntry(new ZipEntry(thriftFile.getAbsolutePath))
  out.write(data.getBytes)
  out.closeEntry()
  out.close()

  val server = new EmbeddedHttpServer(
    twitterServer = diffy,
    flags = Map(
      "proxy.port" -> s":$d",
      "candidate" -> s"localhost:$c",
      "master.primary" -> s"localhost:$p",
      "master.secondary" -> s"localhost:$s",
      "serviceName" -> "myThriftService",
      "service.protocol" -> "thrift",
      "thrift.jar" -> thriftJar.toAbsolutePath.toString,
      "thrift.serviceClass" -> "Adder",
      "summary.email" -> "test"
    ),
    stage = Stage.PRODUCTION
  )

  val client = ThriftMux.client.build[Adder.MethodPerEndpoint](s"localhost:$d")

  test("verify startup") {
    server.assertHealthy()
  }

  test("verify DifferenceCollector") {
    assert(differenceProxy.collector.fields.isEmpty)
    Await.result(client.add(1, 1).liftToTry)
    var tries = 0
    while(differenceProxy.outstandingRequests.get() > 0 && tries < 10) {
      Await.result(Future.sleep(Duration.fromSeconds(1))(DefaultTimer))
      tries = tries + 1
    }
    assert(!differenceProxy.collector.fields.isEmpty)
  }

  test("verify present differences via API") {
    val response =
      Await.result(Http.fetchUrl(s"http://${server.externalHttpHostAndPort}/api/1/endpoints/add/stats"))
    assertResult(Status.Ok)(response.status)
    assert(response.getContentString().contains(""""differences":1"""))
  }

  test("verify absent endpoint in API") {
    val response =
      Await.result(Http.fetchUrl(s"http://${server.externalHttpHostAndPort}/api/1/endpoints/subtract/stats"))
    assertResult(Status.Ok)(response.status)
    assertResult("""{"error":"key not found: subtract"}""")(response.getContentString())
  }

}
