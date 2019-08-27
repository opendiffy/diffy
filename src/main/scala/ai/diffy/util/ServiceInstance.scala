package ai.diffy.util

sealed trait ServiceInstance {
  val name: String
  val isProduction: Boolean
}

object ServiceInstance {
  object Primary extends ServiceInstance {
    override val name: String = "Primary"
    override val isProduction: Boolean = true
  }
  object Secondary extends ServiceInstance {
    override val name: String = "Secondary"
    override val isProduction: Boolean = false
  }
  object Candidate extends ServiceInstance {
    override val name: String = "Candidate"
    override val isProduction: Boolean = false
  }

  val all = Seq(Primary, Secondary, Candidate)
}