package ai.diffy.util

sealed trait ServiceInstance {
  val name: String
  val isProduction: Boolean
}

object ServiceInstance {
  object Primary extends ServiceInstance {
    override val name: String = "primary"
    override val isProduction: Boolean = true
  }
  object Candidate extends ServiceInstance {
    override val name: String = "candidate"
    override val isProduction: Boolean = false
  }
  object Secondary extends ServiceInstance {
    override val name: String = "secondary"
    override val isProduction: Boolean = false
  }

  val all = Seq(Primary, Candidate, Secondary)
  def from(name: String): Option[ServiceInstance] = all.find(_.name == name)
}