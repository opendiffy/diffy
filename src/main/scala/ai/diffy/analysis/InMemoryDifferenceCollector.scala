package ai.diffy.analysis

import ai.diffy.compare.Difference
import ai.diffy.metrics.MetricsReceiver
import io.micrometer.core.instrument.{Counter, Metrics}

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable

class InMemoryDifferenceCounter(name: String) extends DifferenceCounter {
  lazy val receiver = MetricsReceiver.root.get(name)

  val endpointsMap: mutable.Map[String, InMemoryEndpointMetadata] = mutable.Map.empty

  protected[this] def endpointCollector(ep: String) =
    endpointsMap.getOrElseUpdate(ep, new InMemoryEndpointMetadata(receiver.get(ep)))

  override def endpoints: Map[String, EndpointMetadata] =
    endpointsMap.toMap filter { _._2.total > 0 }

  override def clear(): Unit = endpointsMap.clear()

  override def fields(ep: String): Map[String, FieldMetadata] = endpointCollector(ep).fields

  override def count(endpoint: String, diffs: Map[String, Difference]): Unit =
    endpointCollector(endpoint).add(diffs)
}

class InMemoryFieldMetadata(receiver: MetricsReceiver) extends FieldMetadata {
  val differenceCounter = receiver.get("differences").counter
  val siblingsCounter = receiver.get("siblings").counter

  def differences = differenceCounter.count().toInt
  // The total # of siblings that saw differences when this field saw a difference
  def weight = siblingsCounter.count().toInt

  def apply(diffs: Map[String, Difference]) = {
    differenceCounter.increment()
    siblingsCounter.increment(diffs.size)
  }
}

class InMemoryEndpointMetadata(receiver: MetricsReceiver) extends EndpointMetadata {
  val totalCounter = receiver.get("all").counter
  val differenceCounter = receiver.get("different").counter

  // These need to be read from prometheus where they are being aggregated across an entire horizontal diffy cluster
  def total = totalCounter.count().toInt
  def differences = differenceCounter.count().toInt

  private[this] val _fields = new mutable.HashMap[String, InMemoryFieldMetadata]

  def getMetadata(field: String): InMemoryFieldMetadata = {
    if (!_fields.contains(field)) {
      _fields += (field -> new InMemoryFieldMetadata(receiver.get(field)))
    }
    _fields(field)
  }

  def fields: Map[String, InMemoryFieldMetadata] = _fields.toMap

  def add(diffs: Map[String, Difference]): Unit = {
    totalCounter.increment()
    if (diffs.nonEmpty) {
      differenceCounter.increment()
    }
    diffs foreach { case (fieldPath, _) =>
      getMetadata(fieldPath)(diffs)
    }
  }
}

object InMemoryDifferenceCollector {
  val DifferenceResultNotFoundException = new Exception("Difference result not found")
}

class InMemoryDifferenceCollector {
  import InMemoryDifferenceCollector._

  val requestsPerField: Int = 5
  val fields = mutable.Map.empty[Field, mutable.Queue[DifferenceResult]]

  private[this] def sanitizePath(p: String) = p.stripSuffix("/").stripPrefix("/")

  def create(dr: DifferenceResult): Unit = {
    dr.differences foreach { case (path, _) =>
      val queue =
        fields.getOrElseUpdate(
          Field(dr.endpoint, sanitizePath(path)),
          mutable.Queue.empty[DifferenceResult]
        )

      if (queue.size < requestsPerField) {
        queue.enqueue(dr)
      }
    }
  }

  def prefix(field: Field): Iterable[DifferenceResult] =
    (fields.toSeq flatMap {
      case (Field(endpoint, path), value)
        if endpoint == field.endpoint && path.startsWith(field.prefix) => value
      case _ => Nil
    }).toSeq.distinct

  def apply(id: Long): DifferenceResult =
    // Collect first instance of this difference showing up in all the fields
    fields.toStream map { case (field, queue) =>
      queue.find { _.id == id }
    } collectFirst {
      case Some(dr) => dr
    } match {
      case Some(dr) => dr
      case None => throw DifferenceResultNotFoundException
    }

  def clear() = fields.clear()
}