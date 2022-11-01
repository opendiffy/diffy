package ai.diffy.flat

import scala.collection.mutable

class FlatIndexedCollection {
  val reverseIndex =
    mutable.Map.empty[Seq[TerminalFlatObject], mutable.Map[FlatObject, Seq[TerminalFlatObject]]]

  def insert(o: FlatObject): Unit = {
    o.tokenizedPaths foreach { case (k, v) =>
      reverseIndex
        .getOrElseUpdate(k, mutable.Map.empty[FlatObject, Seq[TerminalFlatObject]])
        .update(o, v)
    }
  }

  def collect(
      path: Seq[TerminalFlatObject],
      predicate: TerminalFlatObject => Boolean
  ): Seq[FlatObject] =
    for {
      ovm <- reverseIndex.get(path).toSeq
      (o, vs) <- ovm.toSeq
      v <- vs
      if predicate(v)
    } yield {
      o
    }
}

sealed trait FlatCondition extends (FlatObject => Boolean)

case class equals(o: FlatObject) extends FlatCondition {
  override def apply(other: FlatObject): Boolean = o == other
}

case class matches(regex: String) extends FlatCondition {
  override def apply(other: FlatObject): Boolean =
   other match {
     case FlatPrimitive(str: String) => str.matches(regex)
     case _ => false
   }
}

//case class lt[T : { def <(o:T):Boolean }](upperBound: T) extends FlatCondition {
//  override def apply(other: FlatObject): Boolean =
//    other match {
//      case FlatPrimitive(value: T) => value < upperBound
//      case _ => false
//    }
//}
//
//case class gt[T<: { def >(o:T):Boolean }](lowerBound: T) extends FlatCondition {
//  override def apply(other: FlatObject): Boolean =
//    other match {
//      case FlatPrimitive(value: T) => value > lowerBound
//      case _ => false
//    }
//}
//
//case class lte[T<: { def <=(o:T):Boolean }](upperBound: T) extends FlatCondition {
//  override def apply(other: FlatObject): Boolean =
//    other match {
//      case FlatPrimitive(value: T) => value <= upperBound
//      case _ => false
//    }
//}
//
//case class gte[T<: { def >=(o:T):Boolean }](lowerBound: T) extends FlatCondition {
//  override def apply(other: FlatObject): Boolean =
//    other match {
//      case FlatPrimitive(value: T) => value >= lowerBound
//      case _ => false
//    }
//}

case class TerminalCondition(path: Seq[TerminalFlatObject], predicate: FlatCondition) extends FlatCondition {
  override def apply(o: FlatObject): Boolean =
    o.get(path).exists{ predicate }
}

case class MultipathCondition(
    predicate: ValueCondition,
    paths: Seq[TerminalFlatObject]*)
  extends FlatCondition {
  override def apply(o: FlatObject): Boolean =
    predicate(paths map o.get)
}

sealed trait ValueCondition extends (Seq[Seq[FlatObject]] => Boolean)

object veq extends ValueCondition {
  override def apply(values: Seq[Seq[FlatObject]]): Boolean =
    values match {
      case Nil => true
      case Seq(head) => true
      case Seq(head, tail @ _*) => tail.forall{ _ == head }
    }
}

sealed trait CompositeCondition extends FlatCondition

case class and(conditions: FlatCondition*) extends FlatCondition {
  override def apply(o:FlatObject): Boolean = conditions.forall(_(o))
}

case class or(conditions: FlatCondition*) extends FlatCondition {
  override def apply(o:FlatObject): Boolean = conditions.exists(_(o))
}