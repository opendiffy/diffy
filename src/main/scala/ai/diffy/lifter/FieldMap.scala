package ai.diffy.lifter

class FieldMap(val value: Map[String, _]){
  override def toString: String = {
    value.toSeq.sortBy { case (k, _) => k }.toString
  }
}