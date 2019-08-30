package ai.diffy

import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

import ai.diffy.lifter.{JsonLifter, Message}
import ai.diffy.util.ServiceInstance
import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.Provides
import com.twitter.finagle.Http
import com.twitter.finagle.http._
import com.twitter.finagle.util.DefaultTimer
import com.twitter.inject.TwitterModule
import com.twitter.io.Buf.ByteArray
import com.twitter.util._
import io.netty.handler.codec.http.HttpHeaders
import javax.inject.Singleton

import scala.io.Source

object IsotopeSdkModule extends TwitterModule {
  val isotopeConfig =
    flag("isotope.config", "local.isotope", "the isotope configuration file")

  val currentUser = Try(sys.env("USER")).getOrElse("unknown")
  val mbean = java.lang.management.ManagementFactory.getRuntimeMXBean()
  val startTime = mbean.getStartTime()
  val processName = mbean.getName()
  val Seq(currentPid, currentHostname) = processName.split("@").toSeq

  case class Child(
    id: String,
    `type` : String,
    path: String,
    method: String,
    input: JsonNode,
    input_meta: String,
    output: JsonNode,
    output_meta: String,
    wrapper: String,
    start: Long,
    end: Long,
    prod_id: String)
  case class Observation(
    globalTxId: String,
    parentId: String,
    root: Child,
    children: List[Child] = List.empty[Child])

  case class IsotopeContext(
    service_version: String,
    from_prod : Boolean,
    emitted_at: Long = System.currentTimeMillis(),
    hostname: String = currentHostname,
    pid: String = currentPid,
    server_start: Long = startTime,
    user: String = currentUser,
    sdk_version: String = "diffy-0.0.1-SNAPSHOT-28AUG2019")

  case class ExportPayload(isotope_context: IsotopeContext, recordings: Seq[Observation])

  val defaultVersion = s"${Try(sys.env("USER")).getOrElse("unknown")}@${
    java.util.Calendar.getInstance().getTime().toString()
      .replace(" ", "_")
      .replace(":", "_")
  }"
  case class Credentials(service_name: String, service_xid: String, service_auth: String)
  case class Config(credentials: Credentials, mode: String, localport: Int, service_version: String)

  trait IsotopeClient {
    val isConcrete = false
    def tx(globalTxId: String, path: String, req: Message, res: (Message, Long, Long)): Observation =
      Observation(
        globalTxId,
        "",
        Child(
          "0000",
          "RPC_SERVER",
          path,
          "",
          JsonLifter(req.result),
          "none",
          JsonLifter(res._1),
          "none",
          "none",
          res._2,
          res._3,
          globalTxId
        )
      )

    def save(
      globalTxId: String,
      path: String,
      req: Message,
      candidateResponse: (Message, Long, Long),
      primaryResponse: (Message, Long, Long),
      secondaryResponse: (Message, Long, Long)): Unit = {}
  }

  class ConcreteIsotopeClient(configFile: File, limit: Int = 100, timeout: Long = 10000) extends IsotopeClient {
    val json = Source.fromFile(configFile)
    val config = JsonLifter.Mapper.readValue[Config](json.reader).copy(service_version = defaultVersion)
    def version(trafficSource: ServiceInstance): String = s"${config.service_version}@${trafficSource.name}"
    def context(trafficSource: ServiceInstance) = IsotopeContext(version(trafficSource),trafficSource.isProduction)

    val buffers: Map[ServiceInstance, ConcurrentLinkedQueue[Observation]] =
      ServiceInstance.all map { _ -> (new ConcurrentLinkedQueue[Observation]()) } toMap

    def size():Int = buffers.foldLeft(0){ case (acc, (_, b)) => acc + b.size() }

    override val isConcrete = true
    override def save(
      globalTxId: String,
      path: String,
      req: Message,
      candidateResponse: (Message, Long, Long),
      primaryResponse: (Message, Long, Long),
      secondaryResponse: (Message, Long, Long)): Unit = {
      buffers(ServiceInstance.Primary).add(tx(globalTxId,path, req, primaryResponse))
      buffers(ServiceInstance.Secondary).add(tx(globalTxId,path, req, secondaryResponse))
      buffers(ServiceInstance.Candidate).add(tx(globalTxId,path, req, candidateResponse))

    }


    DefaultTimer.schedule(Duration.fromSeconds(1)){
      if (size() > 0) {
        val payloads = ServiceInstance.all map { source =>
          val buffer = buffers(source)
          val popped = List.concat(buffer.toArray(new Array[Observation](0)))
          popped.foreach(buffer.remove)
          ExportPayload(context(source), popped)
        }
        payloads.foldLeft[Future[Boolean]](Future.True) { case (acc, exportPayload) =>
          for {
            prev <- acc
            current <- publish(exportPayload)
          } yield { prev && current }
        }
      }
    }
    val isotopeService =
      Http.client.withTransport.connectTimeout(Duration.Top)
        .withRequestTimeout(Duration.Top)
        .withTransport.tlsWithoutValidation
        .newService("isotope-api.sn126.com:443")

    val uri = s"https://isotope-api.sn126.com/api/v1/collectors/ingestion/services/${config.credentials.service_xid}/observations"
    def publish(exportPayload: ExportPayload): Future[Boolean] = {
      val bytes = JsonLifter.encode(exportPayload).getBytes
      val post =
        RequestBuilder.safeBuildPost(
          RequestBuilder.create()
            .setHeader(Fields.UserAgent, "diffy")
            .setHeader(Fields.Connection, HttpHeaders.Values.KEEP_ALIVE)
            .setHeader("isotope-service-auth", config.credentials.service_auth)
            .setHeader(Fields.ContentType, MediaType.Json)
            .setHeader(Fields.ContentLength, bytes.length.toString)
            .url(new URL(uri)),
          ByteArray.Owned(bytes))

      isotopeService(post).liftToTry map {
        case Return(r) => {
          logger.debug(r.getContentString())
          true
        }
        case Throw(e) => {
          logger.debug(e.getMessage)
          false
        }
      }
    }
  }
  @Provides
  @Singleton
  def isotopeClient: IsotopeClient = {
    Try(new File(isotopeConfig())) respond {
      case Return(_) => logger.debug("Isotope config file found!")
      case Throw(e) => logger.debug(e.getMessage)
    } map { file =>
      new ConcreteIsotopeClient(file)
    } getOrElse(new IsotopeClient{})
  }

}
