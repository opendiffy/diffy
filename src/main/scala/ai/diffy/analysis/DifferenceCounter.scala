package ai.diffy.analysis

import ai.diffy.compare.Difference

trait EndpointMetadata {
  // number of differences seen at this endpoint
  def differences: Int
  // total # of requests seen for this endpoint
  def total: Int
}

object FieldMetadata {
  val Empty = new FieldMetadata {
    override val differences = 0
    override val weight = 0
  }
}

trait FieldMetadata {
  // number of difference seen for this field
  def differences: Int
  // weight of this field relative to other fields, this number is calculated by counting the
  // number of fields that saw differences on every request that this field saw a difference in
  def weight: Int
}

trait DifferenceCounter {
  def count(endpoint: String, diffs: Map[String, Difference]): Unit
  def endpoints: Map[String, EndpointMetadata]
  def endpoint(endpoint: String):EndpointMetadata = endpoints(endpoint)
  def fields(endpoint: String): Map[String, FieldMetadata]
  def clear(): Unit
}

case class RawDifferenceCounter(counter: DifferenceCounter)
case class NoiseDifferenceCounter(counter: DifferenceCounter)