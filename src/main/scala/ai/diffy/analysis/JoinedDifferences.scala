package ai.diffy.analysis

import scala.math.abs

object DifferencesFilterFactory {
  def apply(relative: Double, absolute: Double): JoinedField => Boolean = {
    (field: JoinedField) =>
      field.raw.differences > field.noise.differences &&
        field.relativeDifference > relative &&
        field.absoluteDifference > absolute
  }
}

case class JoinedDifferences(raw: RawDifferenceCounter, noise: NoiseDifferenceCounter) {
  def endpoints: Map[String, JoinedEndpoint] = {
    raw.counter.endpoints map { case (k, _) => k -> endpoint(k) }
  }

  def endpoint(endpoint: String): JoinedEndpoint = {
    (
      raw.counter.endpoint(endpoint),
      raw.counter.fields(endpoint),
      noise.counter.fields(endpoint)
    ) match { case (endpoint, rawFields, noiseFields) =>
      JoinedEndpoint(endpoint, rawFields, noiseFields)
    }
  }
}

case class JoinedEndpoint(
  endpoint: EndpointMetadata,
  original: Map[String, FieldMetadata],
  noise: Map[String, FieldMetadata])
{
  def differences = endpoint.differences
  def total = endpoint.total
  def fields: Map[String, JoinedField] = original map { case (path, field) =>
    path -> JoinedField(endpoint, field, noise.getOrElse(path, FieldMetadata.Empty))
  }
}

case class JoinedField(endpoint: EndpointMetadata, raw: FieldMetadata, noise: FieldMetadata) {
  // the percent difference out of the total # of requests
  def absoluteDifference = abs(raw.differences - noise.differences) / endpoint.total.toDouble * 100
  // the square error between this field's differences and the noisy counterpart's differences
  def relativeDifference = abs(raw.differences - noise.differences) / (raw.differences + noise.differences).toDouble * 100
}
