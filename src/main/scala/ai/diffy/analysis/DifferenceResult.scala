package ai.diffy.analysis

case class DifferenceResult(
  id: Long,
  traceId: String,
  endpoint: String,
  timestampMsec: Long,
  differences: Map[String, String],
  request: String,
  responses: Responses)

case class Responses(
  primary: String,
  secondary: String,
  candidate: String)