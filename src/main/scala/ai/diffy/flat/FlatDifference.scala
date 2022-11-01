package ai.diffy.flat

import ai.diffy.util.Memoize

import scala.language.postfixOps

object Marker {

  val Left = FlatPrimitive("sn126left")

  val Right = FlatPrimitive("sn126right")

  val Value = FlatPrimitive("sn126value")

  val Field = FlatPrimitive("sn126field")

  val Missing = FlatPrimitive("sn126missing")

  val Extra = FlatPrimitive("sn126extra")

  val Key = FlatPrimitive("sn126key")

  val all = Set(Left, Right, Value, Field, Missing, Extra, Key)

  val leafMarkers = Set(Left, Right, Missing, Extra)
  val containerMarkers = Set(Key, Value)

  def isOne(flatObject: FlatObject): Boolean = all.exists(_ == flatObject)
  def isLeaf(flatObject: FlatObject): Boolean = leafMarkers.exists(_ == flatObject)
  def isContainer(flatObject: FlatObject): Boolean = containerMarkers.exists(_ == flatObject)

}

import Marker._

trait FlatDifference extends FlatObject
trait TerminalFlatDifference extends FlatDifference
case class NoFlatDifference[A](value: A) extends TerminalFlatDifference {
  override def normalized: Map[FlatObject, FlatObject] = Map.empty
}

case class TypeFlatDifference[A, B](left: A, right: B)
  extends TerminalFlatDifference
{
  private[this] def toMessage(obj: Any) = FlatPrimitive(obj.getClass.getSimpleName)
  override def normalized: Map[FlatObject, FlatObject] =
    Map(Left -> toMessage(left), Right -> toMessage(right))
}

case class PrimitiveFlatDifference[A](left: FlatPrimitive[A], right: FlatPrimitive[A]) extends TerminalFlatDifference {
  override def normalized = Map(Left -> left, Right -> right)
}

case object MissingField extends TerminalFlatDifference {
  override def normalized: Map[FlatObject, FlatObject] = Map(Field -> Missing)
}

case object ExtraField extends TerminalFlatDifference {
  override def normalized: Map[FlatObject, FlatObject] = Map(Field -> Extra)
}

trait SeqFlatDifference extends FlatDifference

case class OrderingFlatDifference(leftPattern: Seq[Int], rightPattern: Seq[Int])
  extends TerminalFlatDifference
    with SeqFlatDifference
{
  override def normalized: Map[FlatObject, FlatObject] =
    Map(Left -> FlatObject.lift(leftPattern), Right -> FlatObject.lift(rightPattern))
}

case class SeqSizeFlatDifference[A](leftNotRight: Seq[A], rightNotLeft: Seq[A])
  extends TerminalFlatDifference
    with SeqFlatDifference
{
  override def normalized: Map[FlatObject, FlatObject] =
    Map(Left -> FlatObject.lift(leftNotRight), Right -> FlatObject.lift(rightNotLeft))
}

case class IndexedFlatDifference(indexedDiffs: Seq[FlatDifference]) extends SeqFlatDifference {
  override def normalized: Map[FlatObject, FlatObject] =
    indexedDiffs flatMap { _.normalized } toMap
}

case class SetFlatDifference[A](leftNotRight: Set[A], rightNotLeft: Set[A])
  extends TerminalFlatDifference
{
  override def normalized: Map[FlatObject, FlatObject] =
    Map(Left -> FlatObject.lift(leftNotRight), Right -> FlatObject.lift(rightNotLeft))
}

case class MapFlatDifference[A](keys: TerminalFlatDifference, values: Map[A, FlatDifference])
  extends TerminalFlatDifference
{
  override def normalized: Map[FlatObject, FlatObject] =
    Map(Key -> keys, Value -> FlatObject.lift(values))
}

case class ObjectFlatDifference(mapDiff: MapFlatDifference[FlatObject]) extends FlatDifference {
  override def normalized: Map[FlatObject, FlatObject] = mapDiff.normalized
}

object FlatDifference {
  def apply[A](left: FlatObject, right: FlatObject): FlatDifference =
    (left, right) match {
      case (l, r) if l == r => NoFlatDifference(l)
      case (l @ FlatPrimitive(_), r @ FlatPrimitive(_)) => PrimitiveFlatDifference(l,r)
      case (ls: FlatSeq[FlatObject], rs: FlatSeq[FlatObject]) => diffSeq(ls, rs)
      case (ls: FlatSet[FlatObject], rs: FlatSet[FlatObject]) => diffSet(ls, rs)
      case (lm: FlatStruct, rm: FlatStruct) => diffObjectMap(lm, rm)
      case (lm: FlatMap[FlatObject, FlatObject], rm: FlatMap[FlatObject, FlatObject]) => diffMap(lm, rm)
      case (l, r) if l.getClass != r.getClass => TypeFlatDifference(l, r)
    }

  def diffSet(left: FlatSet[FlatObject], right: FlatSet[FlatObject]): TerminalFlatDifference =
    if(left == right) NoFlatDifference(left) else SetFlatDifference(left.value -- right.value, right.value -- left.value)

  def diffSeq(left: FlatSeq[FlatObject], right: FlatSeq[FlatObject]): SeqFlatDifference = {
    val leftNotRight = left.value diff right.value
    val rightNotLeft = right.value diff left.value

    if (leftNotRight ++ rightNotLeft == Nil) {
      def seqPattern(s: Seq[FlatObject]) = s map { left.value.indexOf(_) }
      OrderingFlatDifference(seqPattern(left.value), seqPattern(right.value))
    } else if (left.value.length == right.value.length) {
      IndexedFlatDifference((left.value zip right.value) map { case (le, re) => apply(FlatObject.lift(le), FlatObject.lift(re)) })
    } else {
      SeqSizeFlatDifference(leftNotRight, rightNotLeft)
    }
  }

  def diffMap(lm: FlatMap[FlatObject, FlatObject], rm: FlatMap[FlatObject, FlatObject]): MapFlatDifference[FlatObject] =
    MapFlatDifference (
      diffSet(FlatSet(lm.value.keySet), FlatSet(rm.value.keySet)),
      (lm.value.keySet intersect  rm.value.keySet) map { key =>
        key -> apply(lm.value(key), rm.value(key))
      } toMap
    )

  def diffObjectMap(lm: FlatStruct, rm: FlatStruct): ObjectFlatDifference =
    ObjectFlatDifference(diffMap(
        lm.value.asInstanceOf[FlatMap[FlatObject, FlatObject]],
        rm.value.asInstanceOf[FlatMap[FlatObject, FlatObject]]
    ))
}