package ai.diffy.metrics

import ai.diffy.util.Memoize
import io.micrometer.core.instrument.{Counter, Metrics}

object MetricsReceiver {
  private class MemoizedMetricsReceiver(val tokens: Seq[String]) extends  MetricsReceiver {
    val name = tokens reduce {
      _ + "." + _
    }

    override lazy val counter = Metrics.globalRegistry.counter(name)

    override def get(name: String): MetricsReceiver = new MemoizedMetricsReceiver(tokens :+ name)
  }

  private def apply: Seq[String] => MetricsReceiver = Memoize { new MemoizedMetricsReceiver(_) }
  def root: MetricsReceiver = apply(Seq("diffy"))
}

trait MetricsReceiver {
  def get(name: String): MetricsReceiver
  def counter: Counter
}