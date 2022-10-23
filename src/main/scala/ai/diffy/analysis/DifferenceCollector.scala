package ai.diffy.analysis

import ai.diffy.compare.{Difference, PrimitiveDifference}
import ai.diffy.lifter.{JsonLifter, Message}
import ai.diffy.repository.DifferenceResultRepository
import io.opentelemetry.api.trace.Span
import org.slf4j.LoggerFactory

import java.util.Date
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Random

object DifferenceAnalyzer {
  val log = LoggerFactory.getLogger(classOf[DifferenceAnalyzer])
  val UndefinedEndpoint = Some("undefined_endpoint")
  def normalizeEndpointName(name: String) = name.replace("/", "-")
}

case class Field(endpoint: String, prefix: String)

class DifferenceAnalyzer(
    rawCounter: RawDifferenceCounter,
    noiseCounter: NoiseDifferenceCounter,
    store: InMemoryDifferenceCollector,
    repository: DifferenceResultRepository)
{
  import DifferenceAnalyzer._

  def apply(
    request: Message,
    candidate: Message,
    primary: Message,
    secondary: Message
  ): Option[DifferenceResult] = {
    getEndpointName(request.endpoint, candidate.endpoint,
        primary.endpoint, secondary.endpoint) flatMap { endpointName =>
      val rawDiff = Difference(primary, candidate).flattened
      val noiseDiff = Difference(primary, secondary).flattened

      val id = new String(Random.alphanumeric.take(10).toArray);
      rawCounter.counter.count(endpointName, rawDiff)
      noiseCounter.counter.count(endpointName, noiseDiff)

      if (rawDiff.size > 0) {
        val diffResult = new DifferenceResult(
          id,
          Span.current().getSpanContext.getTraceId,
          endpointName,
          new Date().getTime,
          differencesToJson(rawDiff).asJava,
          JsonLifter.encode(request.result),
          new Responses(
            JsonLifter.encode(candidate.result),
            JsonLifter.encode(primary.result),
            JsonLifter.encode(secondary.result)
          ));
        store.create(diffResult)
        val saved = repository.save(diffResult)
        log.info(s"repository saved $endpointName -- ${saved.id} -- ${saved.traceId}")
        Some(saved)
      } else {
        log.debug(s"endpoint[$endpointName]diff[$id]=NoDifference")
        None
      }
    }
  }

  def clear(): Unit = {
    rawCounter.counter.clear()
    noiseCounter.counter.clear()
    store.clear()
    repository.deleteAll()
  }

  def differencesToJson(diffs: Map[String, Difference]): Seq[FieldDifference] =
    diffs.toSeq map {
      case (field, diff @ PrimitiveDifference(_: Long, _)) =>
        new FieldDifference(
          field,
          JsonLifter.encode(
            diff.toMap map {
              case (k, v) => k -> v.toString
            }
          )
        )

      case (field, diff) => new FieldDifference(field, JsonLifter.encode(diff.toMap))
    }

  private[this] def getEndpointName(
      requestEndpoint: Option[String],
      candidateEndpoint: Option[String],
      primaryEndpoint: Option[String],
      secondaryEndpoint: Option[String]): Option[String] = {
    val rawEndpointName = (requestEndpoint, candidateEndpoint, primaryEndpoint, secondaryEndpoint) match {
      case (Some(_), _, _, _) => requestEndpoint
      // undefined endpoint when action header is missing from all three instances
      case (_, None, None, None) => UndefinedEndpoint
      // the assumption is that primary and secondary should call the same endpoint,
      // otherwise it's noise and we should discard the request
      case (_, None, _, _) if primaryEndpoint == secondaryEndpoint => primaryEndpoint
      case (_, None, _, _) => None
      case (_, Some(_), _, _) => candidateEndpoint
    }

    rawEndpointName map { normalizeEndpointName(_) }
  }
}
