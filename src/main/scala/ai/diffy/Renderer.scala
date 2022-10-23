package ai.diffy

import ai.diffy.analysis.{DifferenceResult, EndpointMetadata, FieldDifference, FieldMetadata, JoinedField}
import ai.diffy.lifter.JsonLifter

import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.language.postfixOps

object Renderer {
  def differences(diffs: Iterable[FieldDifference]) =
    diffs map { case fd => fd.field -> JsonLifter.decode(fd.difference) } toMap

  def differenceResults(drs: Iterable[DifferenceResult], includeRequestResponses: Boolean = false) =
    drs map { differenceResult(_, includeRequestResponses) }

  def differenceResult(dr: DifferenceResult, includeRequestResponses: Boolean = false) =
    Map(
      "id" -> dr.id,
      "trace_id" -> dr.traceId,
      "timestamp_msec" -> dr.timestampMsec,
      "endpoint" -> dr.endpoint,
      "differences" -> differences(dr.differences.asScala)
    ) ++ {
      if (includeRequestResponses) {
        Map(
          "request" -> JsonLifter.decode(dr.request),
          "left" -> JsonLifter.decode(dr.responses.primary),
          "right" -> JsonLifter.decode(dr.responses.candidate)
        )
      } else {
        Map.empty[String, Any]
      }
    }

  def endpoints(endpoints: Map[String, EndpointMetadata]): Map[String, Map[String, Int]] =
    endpoints map { case (ep, meta) =>
      ep -> endpoint(meta)
    }

  def endpoint(endpoint: EndpointMetadata) = Map(
    "total" -> endpoint.total,
    "differences" -> endpoint.differences
  )

  def field(field: FieldMetadata, includeWeight: Boolean) =
    Map("differences" -> field.differences) ++ {
      if (includeWeight) {
        Map("weight" -> field.weight)
      } else {
        Map.empty[String, Any]
      }
    }

  def field(field: JoinedField, includeWeight: Boolean) =
    Map(
      "differences" -> field.raw.differences,
      "noise" -> field.noise.differences,
      "relative_difference" -> field.relativeDifference,
      "absolute_difference" -> field.absoluteDifference
    ) ++ {
      if (includeWeight) Map("weight" -> field.raw.weight) else Map.empty
    }

  def fields(
    fields: Map[String, JoinedField],
    includeWeight: Boolean = false
  ) =
    fields map { case (path, meta) =>
      path -> field(meta, includeWeight)
    } toMap

  def error(message: String) =
    Map("error" -> message)

  def success(message: String) =
    Map("success" -> message)
}