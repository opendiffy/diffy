package ai.diffy.analysis

import ai.diffy.compare.{Difference, PrimitiveDifference}
import ai.diffy.lifter.{JsonLifter, Message}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.util.Date
import scala.util.Random

object DifferenceAnalyzer {
  val UndefinedEndpoint = Some("undefined_endpoint")
  def normalizeEndpointName(name: String) = name.replace("/", "-")
}

case class Field(endpoint: String, prefix: String)

class DifferenceAnalyzer(
    rawCounter: RawDifferenceCounter,
    noiseCounter: NoiseDifferenceCounter,
    store: InMemoryDifferenceCollector)
{
  import DifferenceAnalyzer._

  def apply(
    request: Message,
    candidate: Message,
    primary: Message,
    secondary: Message
  ): Unit = {
    getEndpointName(request.endpoint, candidate.endpoint,
        primary.endpoint, secondary.endpoint) foreach { endpointName =>
      val rawDiff = Difference(primary, candidate).flattened
      val noiseDiff = Difference(primary, secondary).flattened

      val id = Random.nextLong();
      rawCounter.counter.count(endpointName, rawDiff)
      noiseCounter.counter.count(endpointName, noiseDiff)

      if (rawDiff.size > 0) {
        val diffResult = DifferenceResult(
          id,
          id.toString,
          endpointName,
          new Date().getTime,
          differencesToJson(rawDiff),
          JsonLifter.encode(request.result),
          Responses(
            candidate = JsonLifter.encode(candidate.result),
            primary = JsonLifter.encode(primary.result),
            secondary = JsonLifter.encode(secondary.result)
          )
        )

//        log.info(s"endpoint[$endpointName]diff[$id]=$diffResult")
        store.create(diffResult)
      } else {
//        log.debug(s"endpoint[$endpointName]diff[$id]=NoDifference")
      }
    }
  }

  def clear(): Unit = {
    rawCounter.counter.clear()
    noiseCounter.counter.clear()
    store.clear()
  }

  def differencesToJson(diffs: Map[String, Difference]): Map[String, String] =
    diffs map {
      case (field, diff @ PrimitiveDifference(_: Long, _)) =>
        field ->
          JsonLifter.encode(
            diff.toMap map {
              case (k, v) => k -> v.toString
            }
          )

      case (field, diff) => field -> JsonLifter.encode(diff.toMap)
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
      case (_, Some(candidate), _, _) => candidateEndpoint
    }

    rawEndpointName map { normalizeEndpointName(_) }
  }
}
