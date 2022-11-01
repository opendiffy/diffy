package ai.diffy.flat

import java.nio.ByteBuffer

import com.fasterxml.jackson.databind.JsonNode

import ai.diffy.lifter.{FieldMap, JsonLifter, StringLifter}
import ai.diffy.util.Memoize
import scala.collection.mutable
import scala.language.postfixOps


object FlatObject {
  val isPrimitive: Any => Boolean = {
    case _: Unit => true
    case _: Boolean => true
    case _: Byte => true
    case _: Char => true
    case _: Short => true
    case _: Int => true
    case _: Long => true
    case _: Float => true
    case _: Double => true
    case _: String => true
    case _ => false
  }

  private[this] def mkMap(obj: Any): Map[String, Any] = mapMaker(obj.getClass)(obj)

  /**
    * Assume that a class definition will not change and only use reflection
    * once per class to discover it's fields
    */
  private[this] val mapMaker: Class[_] => (Any => Map[String, Any]) = Memoize { c =>
    val fields = c.getDeclaredFields filterNot { _.getName.contains('$') }
    fields foreach { _.setAccessible(true) }
    { obj: Any =>
      fields map { field =>
        field.getName -> field.get(obj)
      } toMap
    }
  }

  private[this] def liftClass[T](c: Class[T]): (T => FlatStruct) = {
    val fields = c.getDeclaredFields filterNot { _.getName.contains('$') }
    fields foreach { _.setAccessible(true) }
    { obj: Any =>
      FlatStruct(FlatMap(
        fields map { field =>
          FlatPrimitive(field.getName) -> lift(field.get(obj))
        } toMap
      ))
    }
  }

  def lift(a: Any): FlatObject = a match {
    case flatObject: FlatObject => flatObject
    case _ if isPrimitive(a) => FlatPrimitive(a)
    case as: Seq[_] => FlatSeq(as map {lift})
    case as: Set[_] => FlatSet(as map {lift})
    case as: Map[_, _] => FlatMap(as map {case (k,v) => lift(k) -> lift(v)})
    case array: Array[_] => lift(array.toSeq)
    case byteBuffer: ByteBuffer => lift(new String(byteBuffer.asReadOnlyBuffer().array))
    case jsonNode: JsonNode => lift(JsonLifter.lift(jsonNode))
    case null => FlatNull
    case fm: FieldMap => lift(fm.value)
    case _ => FlatStruct(FlatMap(mkMap(a) map {case (k,v) => FlatPrimitive(k) -> lift(v)}))
  }
}

trait FlatObject {
  def normalized: Map[FlatObject, FlatObject]
  def flatMap(f: (FlatObject, FlatObject) => FlatObject): FlatObject = {
    FlatMap(normalized flatMap {
      case (k,v) => f(k, v).normalized
    })
  }
  def get(key: FlatObject): Seq[FlatObject] = key match {
    case FlatNull => normalized.values.toSeq
    case _ => normalized.get(key).toSeq
  }

  def get(path: Seq[FlatObject]): Seq[FlatObject] =
    path match {
      case Nil => Seq(this)
      case keys => get(keys.head) flatMap { _.get(keys.tail) }
    }

  def getMatching(tokenizedPath: Seq[TerminalFlatObject]): Seq[FlatObject] =
    tokenizedPath match {
      case Nil => Seq(this)
      case Seq(FlatNull, tail @ _*) => normalized.values.toSeq flatMap { _.getMatching(tail) }
      case keys => get(keys.head) flatMap { _.getMatching(keys.tail) }
    }

  def flatTable: Map[Seq[FlatObject], TerminalFlatObject] =
    for {
      (pkey, value) <- normalized
      (lKeys, terminal) <- value.flatTable
    } yield {
      (Seq(pkey) ++ lKeys) -> terminal
    }

  lazy val paths =
    flatTable.keys.toSeq map { _ map {
        case FlatPrimitive(v:String) => v
        case _ => "$"
      } mkString "."
    }

  lazy val tokenizedPaths: Map[Seq[TerminalFlatObject], Seq[TerminalFlatObject]] =
    flatTable.foldLeft(
      mutable.Map.empty[Seq[TerminalFlatObject], mutable.Buffer[TerminalFlatObject]]
    ) {
      case (acc, (k, v)) =>
        val tokenizedPath =
          k map {
            case p @ FlatPrimitive(_: String) => p
            case _ => FlatNull
          }
        acc.getOrElseUpdate(tokenizedPath, mutable.Buffer.empty[TerminalFlatObject]).append(v)
        acc
    }.toMap map { case (k, v) => k -> v.toSeq }

  lazy val rendered: Seq[FlatEntry] =
    flatTable.toSeq map {
      case (k, value) =>
        val key = k map {
          case FlatPrimitive(v) => v
          case _ => "$"
        } mkString "."
        FlatEntry(key, value.token)
    }
}

case class FlatEntry(key: String, value: String)

trait TerminalFlatObject extends FlatObject {
  override val normalized = Map.empty[FlatObject, FlatObject]
  override val flatTable = Map(Seq.empty[FlatObject] -> this)
  val token = "$"
}

case object FlatNull extends TerminalFlatObject
case class FlatPrimitive[T](value: T) extends TerminalFlatObject{
  override val token = value.toString
}

abstract class FlatCollection[T <: FlatObject](value: Iterable[T]) extends FlatObject {
  override lazy val normalized: Map[FlatObject, FlatObject] =
    value.zipWithIndex map { case (v, i) => FlatPrimitive(i) -> v} toMap
}
case class FlatSeq[T <: FlatObject](value: Seq[T]) extends FlatCollection(value)
case class FlatSet[T <: FlatObject](value: Set[T]) extends FlatCollection(value.toSeq.sortBy(_.toString))

case class FlatMap[K <: FlatObject, V <: FlatObject](value: Map[K, V]) extends FlatObject {
  override lazy val normalized: Map[FlatObject, FlatObject] = value.asInstanceOf[Map[FlatObject, FlatObject]]
}
case class FlatStruct(value: FlatMap[FlatPrimitive[String], FlatObject]) extends FlatObject {
  override lazy val normalized: Map[FlatObject, FlatObject] = value.value.asInstanceOf[Map[FlatObject, FlatObject]]
}