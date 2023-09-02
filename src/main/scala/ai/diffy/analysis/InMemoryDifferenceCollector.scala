package ai.diffy.analysis

import ai.diffy.compare.{Difference, NoDifference}
import ai.diffy.metrics.MetricsReceiver
import io.micrometer.core.instrument.{Counter, Metrics}

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Try

class InMemoryDifferenceCounter(name: String) extends DifferenceCounter {
  lazy val receiver = MetricsReceiver.root.withNameToken(name)

  val endpointsMap: mutable.Map[String, InMemoryEndpointMetadata] = mutable.Map.empty

  protected[this] def endpointCollector(ep: String) =
    endpointsMap.getOrElseUpdate(ep, new InMemoryEndpointMetadata(receiver.withAdditionalTags(Map("endpoint" -> ep))))

  override def endpoints: Map[String, EndpointMetadata] =
    endpointsMap.toMap filter { _._2.total > 0 }

  override def clear(): Unit = endpointsMap.clear()

  override def fields(ep: String): Map[String, FieldMetadata] = endpointCollector(ep).fields

  override def count(endpoint: String, diffs: Map[String, Difference]): Unit =
    endpointCollector(endpoint).add(diffs)
}

class InMemoryFieldMetadata(receiver: MetricsReceiver) extends FieldMetadata {
  val differenceCounter = receiver.withNameToken("differences").counter
  val siblingsCounter = receiver.withNameToken("siblings").counter

  val Seq(diffsAtomic,sibsAtomic) = Seq.fill(2)(new AtomicInteger(0))
  def differences = diffsAtomic.get()//differenceCounter.count().toInt
  // The total # of siblings that saw differences when this field saw a difference
  def weight = sibsAtomic.get()//siblingsCounter.count().toInt

  def apply(diffs: Map[String, Difference]) = {
    differenceCounter.increment()
    siblingsCounter.increment(diffs.size)
    diffsAtomic.incrementAndGet()
    sibsAtomic.addAndGet(diffs.size)
  }
}

class InMemoryEndpointMetadata(receiver: MetricsReceiver) extends EndpointMetadata {
  val totalCounter = receiver.withNameToken("all").counter
  val differenceCounter = receiver.withNameToken("different").counter

  val totalAtomic = new AtomicInteger(0)
  val differenceAtomic = new AtomicInteger(0)
  // These need to be read from prometheus where they are being aggregated across an entire horizontal diffy cluster
  def total = totalAtomic.get()//totalCounter.count().toInt
  def differences = differenceAtomic.get()//differenceCounter.count().toInt

  private[this] val _fields = new mutable.HashMap[String, InMemoryFieldMetadata]

  def getMetadata(field: String): InMemoryFieldMetadata = {
    if (!_fields.contains(field)) {
      _fields += (field -> new InMemoryFieldMetadata(receiver.withAdditionalTags(Map("field" -> field))))
    }
    _fields(field)
  }

  def fields: Map[String, InMemoryFieldMetadata] = _fields.toMap

  def add(diffs: Map[String, Difference]): Unit = {
    Try(totalCounter.increment())
    totalAtomic.incrementAndGet()
    if (diffs.filterNot{case (_, diff) => diff.isInstanceOf[NoDifference[_]]}.nonEmpty) {
      differenceCounter.increment()
      differenceAtomic.incrementAndGet()
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
    dr.differences.asScala foreach { case fieldDifference: FieldDifference =>
      val path = fieldDifference.field
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