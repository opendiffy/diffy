package ai.diffy

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

import ai.diffy.IsotopeSdkModule.IsotopeClient.ExportPayloadRequest
import ai.diffy.lifter.{JsonLifter, Message}
import ai.diffy.util.TrafficSource
import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.Provides
import com.twitter.finagle.Http
import com.twitter.finagle.http.{Method, Request, RequestProxy}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.inject.TwitterModule
import com.twitter.util.{Duration, Try}
import javax.inject.Singleton

object IsotopeSdkModule extends TwitterModule {
  val isotopeConfig =
    flag("isotope.config", "local.isotope", "the isotope configuration file")

  case class Child(
    id: String,
    type_ : String,
    path: String,
    method: String,
    input: JsonNode,
    input_meta: JsonNode,
    output: JsonNode,
    output_meta: JsonNode,
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
    emitted_at: Long = System.currentTimeMillis())

  case class ExportPayload(isotope_context: IsotopeContext, recordings: List[Observation])

  object IsotopeClient {
    case class ExportPayloadRequest(payload: ExportPayload, service_xid: String) extends RequestProxy {
      override lazy val request = Request(Method.Post,  servicePath(service_xid))
      request.setContentString(JsonLifter.encode(payload))
      request.setContentTypeJson()

      def servicePath(service_xid: String) =
        s"isotope.sn126.com/api/v1/collectors/ingestion/services/${service_xid}/observations"
    }
  }
  trait IsotopeClient {
//    def tx(req: Message, res: Message): Transaction = _
//    def push(transaction: Transaction, from_prod: Boolean): Unit = {}
    def save(req: Message, candidateResponse: Message, primaryResponse: Message, secondaryResponse: Message): Unit = {
//      push(tx(req, candidateResponse), false)
//      push(tx(req, primaryResponse), true)
//      push(tx(req, secondaryResponse), false)
    }
  }

  class ConcreteIsotopeClient(configFile: File, limit: Int = 100, timeout:Long = 10000) extends IsotopeClient {
    val buffers: Map[TrafficSource, ConcurrentLinkedQueue[Observation]] =
      TrafficSource.all map { _ -> (new ConcurrentLinkedQueue[Observation]()) } toMap

    def size():Int = buffers.foldLeft(0){ case (acc, (_, b)) => acc + b.size() }
    def drain(isotope_context: IsotopeContext, buffer: ConcurrentLinkedQueue[Observation]): Unit  = {
    }


    DefaultTimer.schedule(Duration.fromSeconds(1)){
      if (size() > 0) {
        buffers foreach { case (source, buffer) =>

          val popped: Seq[Observation] = List.concat(buffer.toArray(new Array[Observation](0)))
          popped.foreach(buffer.remove)
          val exportPayload = ExportPayload(IsotopeContext("",false), List.concat(popped))
          System.out.println(exportPayload)
        }
      }
    }
    val isotopeService =
      Http.client
        .withTls("")
        .newService("")

    def publish(exportPayload: ExportPayload): Unit = {
      isotopeService(ExportPayloadRequest(exportPayload, "")).map(_.contentString).foreach(println)
    }
  }
  @Provides
  @Singleton
  def isotopeClient: IsotopeClient = {
    Try(new File(isotopeConfig())) map { file =>
      new ConcreteIsotopeClient(file)
    } getOrElse(new IsotopeClient{})
  }

}
