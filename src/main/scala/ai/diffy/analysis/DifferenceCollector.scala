package ai.diffy.analysis

import ai.diffy.IsotopeSdkModule.IsotopeClient
import ai.diffy.compare.{Difference, PrimitiveDifference}
import ai.diffy.lifter.{JsonLifter, Message}
import ai.diffy.thriftscala.{DifferenceResult, Responses}
import com.twitter.finagle.tracing.Trace
import com.twitter.logging._
import com.twitter.util.{Future, StorageUnit, Time, Try}
import javax.inject.Inject

import scala.util.Random

object DifferenceAnalyzer {
  val UndefinedEndpoint = Some("undefined_endpoint")
  val log = Logger(classOf[DifferenceAnalyzer])
  log.setUseParentHandlers(false)
  log.addHandler(
    FileHandler(
      filename = "differences.log",
      rollPolicy = Policy.MaxSize(StorageUnit.fromMegabytes(128)),
      rotateCount = 2
    )()
  )

  def normalizeEndpointName(name: String) = name.replace("/", "-")
}

case class Field(endpoint: String, prefix: String)

class DifferenceAnalyzer @Inject()(
    rawCounter: RawDifferenceCounter,
    noiseCounter: NoiseDifferenceCounter,
    store: InMemoryDifferenceCollector,
    isotopeClient: IsotopeClient)
{
  import DifferenceAnalyzer._

  def apply(
    request: Message,
    candidate: (Message, Long, Long),
    primary: (Message, Long, Long),
    secondary: (Message, Long, Long)
  ): Unit = {
    getEndpointName(request.endpoint, candidate._1.endpoint,
        primary._1.endpoint, secondary._1.endpoint) foreach { endpointName =>
      // If there is no traceId then generate our own
      val id = Trace.idOption map { _.traceId.toLong } getOrElse(Random.nextLong)
      Try(isotopeClient.save(id.toString, endpointName, request,candidate, primary, secondary))
      val rawDiff = Difference(primary._1, candidate._1).flattened
      val noiseDiff = Difference(primary._1, secondary._1).flattened

      rawCounter.counter.count(endpointName, rawDiff)
      noiseCounter.counter.count(endpointName, noiseDiff)

      if (rawDiff.size > 0) {
        val diffResult = DifferenceResult(
          id,
          Trace.idOption map { _.traceId.toLong },
          endpointName,
          Time.now.inMillis,
          differencesToJson(rawDiff),
          JsonLifter.encode(request.result),
          Responses(
            candidate = JsonLifter.encode(candidate._1.result),
            primary = JsonLifter.encode(primary._1.result),
            secondary = JsonLifter.encode(secondary._1.result)
          )
        )

        log.info(s"endpoint[$endpointName]diff[$id]=$diffResult")
        store.create(diffResult)
      } else {
        log.debug(s"endpoint[$endpointName]diff[$id]=NoDifference")
      }
    }
  }

  def clear(): Future[Unit] =
    Future.join(
      rawCounter.counter.clear(),
      noiseCounter.counter.clear(),
      store.clear()
    ) map { _ => () }

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
