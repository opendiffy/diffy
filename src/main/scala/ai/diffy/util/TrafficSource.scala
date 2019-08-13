package ai.diffy.util

sealed trait TrafficSource {
  val isProduction: Boolean
}

object TrafficSource {
  object Primary extends TrafficSource {
    override val isProduction: Boolean = true
  }
  object Secondary extends TrafficSource {
    override val isProduction: Boolean = false
  }
  object Candidate extends TrafficSource {
    override val isProduction: Boolean = false
  }

  val all = Seq(Primary, Secondary, Candidate)
}