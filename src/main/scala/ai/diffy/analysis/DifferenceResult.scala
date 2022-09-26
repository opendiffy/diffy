package ai.diffy.analysis

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
case class DifferenceResult(
  @Id id: Long,
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