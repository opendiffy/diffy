package ai.diffy.analysis

import ai.diffy.compare.Difference
import org.springframework.stereotype.Component

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable

class InMemoryDifferenceCounter extends DifferenceCounter {
  val endpointsMap: mutable.Map[String, InMemoryEndpointMetadata] = mutable.Map.empty

  protected[this] def endpointCollector(ep: String) = {
    if (!endpointsMap.contains(ep)) {
      endpointsMap += ep -> new InMemoryEndpointMetadata()
    }
    endpointsMap(ep)
  }

  override def endpoints: Map[String, EndpointMetadata] =
    endpointsMap.toMap filter { _._2.total > 0 }

  override def clear(): Unit = endpointsMap.clear()

  override def fields(ep: String): Map[String, FieldMetadata] = endpointCollector(ep).fields

  override def count(endpoint: String, diffs: Map[String, Difference]): Unit =
    endpointCollector(endpoint).add(diffs)
}

class InMemoryFieldMetadata extends FieldMetadata {
  val atomicCount = new AtomicInteger(0)
  val atomicSiblingsCount = new AtomicInteger(0)

  def differences = atomicCount.get
  // The total # of siblings that saw differences when this field saw a difference
  def weight = atomicSiblingsCount.get

  def apply(diffs: Map[String, Difference]) = {
    atomicCount.incrementAndGet()
    atomicSiblingsCount.addAndGet(diffs.size)
  }
}

class InMemoryEndpointMetadata extends EndpointMetadata {
  val atomicTotalCount = new AtomicInteger(0)
  val atomicDifferencesCount = new AtomicInteger(0)

  def total = atomicTotalCount.get
  def differences = atomicDifferencesCount.get

  private[this] val _fields = new mutable.HashMap[String, InMemoryFieldMetadata]

  def allResults = _fields.values

  def getMetadata(field: String): InMemoryFieldMetadata = {
    if (!_fields.contains(field)) {
      _fields += (field -> new InMemoryFieldMetadata)
    }
    _fields(field)
  }

  def fields: Map[String, InMemoryFieldMetadata] = _fields.toMap

  def add(diffs: Map[String, Difference]): Unit = {
    atomicTotalCount.incrementAndGet()
    if (diffs.size > 0) {
      atomicDifferencesCount.incrementAndGet()
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