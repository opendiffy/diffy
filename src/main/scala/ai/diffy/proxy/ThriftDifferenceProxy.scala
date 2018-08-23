package ai.diffy.proxy

import java.io.File
import java.util.zip.ZipFile

import ai.diffy.analysis.{DifferenceAnalyzer, InMemoryDifferenceCollector, JoinedDifferences}
import ai.diffy.lifter.{MapLifterPool, Message, ThriftLifter}
import ai.diffy.scrooge.ZippedFileImporter
import com.twitter.finagle.thrift.{ClientId, ThriftClientRequest}
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finagle.{Resolver, Thrift, ThriftMux}
import com.twitter.util.{Future, Try}

import scala.collection.JavaConversions._

case class ThriftDifferenceProxy (
    settings: Settings,
    collector: InMemoryDifferenceCollector,
    joinedDifferences: JoinedDifferences,
    analyzer: DifferenceAnalyzer)
  extends DifferenceProxy
{
  override type Req = ThriftClientRequest
  override type Rep = Array[Byte]
  override type Srv = ThriftService

  private[this] lazy val clientId = new ClientId(settings.clientId)

  override val proxy = super.proxy

  private[this] val zipfile = new ZipFile(new File(settings.pathToThriftJar))

  private[this] val importer =
    ZippedFileImporter(Seq(zipfile))

  private[this] val filenames =
    zipfile.entries.toSeq collect {
      case zipEntry if !zipEntry.isDirectory && zipEntry.getName.endsWith(".thrift") =>
        zipEntry.getName
    }

  val lifter =
    MapLifterPool(
      ThriftLifter.fromImporter(
        importer,
        filenames,
        settings.serviceClass
      )
    )

  override def serviceFactory(serverset: String, label: String) = {
    val client = if (settings.enableThriftMux) {
      ThriftMux.client
        .withClientId(clientId)
        .newClient(serverset, label).toService
    } else {
      val config = if(settings.useFramedThriftTransport) {
        Thrift.client
      } else {
        Thrift.client.withBufferedTransport
      }

      config
        .withNoAttemptTTwitterUpgrade
        .withTracer(NullTracer)
        .withClientId(clientId)
        .newClient(serverset, label)
        .toService
    }

    ThriftService(client, Resolver.eval(serverset))
  }

  override lazy val server = {
    if (settings.enableThriftMux) {
      ThriftMux.serve(
        settings.servicePort,
        proxy map { req: Array[Byte] => new ThriftClientRequest(req, false) }
      )
    } else {
      val config = if(settings.useFramedThriftTransport) {
        Thrift.server
      } else {
        Thrift.server.withBufferedTransport()
      }
      config.withTracer(NullTracer).serve(
        settings.servicePort,
        proxy map { req: Array[Byte] => new ThriftClientRequest(req, false) }
      )
    }
  }

  override def liftRequest(req: ThriftClientRequest): Future[Message] = lifter(req.message)
  override def liftResponse(rep: Try[Array[Byte]]): Future[Message] =
    Future.const(rep) flatMap { lifter(_) }
}
