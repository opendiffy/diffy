package ai.diffy.lifter

class FieldMap(self: Map[String, _]){
  val value = self

  override def toString: String = {
    self.toSeq.sortBy { case (k, _) => k }.toString
  }
}