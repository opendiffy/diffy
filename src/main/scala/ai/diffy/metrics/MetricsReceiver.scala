package ai.diffy.metrics

import ai.diffy.util.Memoize
import io.micrometer.core.instrument.{Counter, Metrics, Tag, Tags}

import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.language.postfixOps

object MetricsReceiver {
  private class MemoizedMetricsReceiver(val name: String, val tags: Map[String, String]) extends  MetricsReceiver {

    override lazy val counter = Metrics.globalRegistry.counter(name, Tags.of(tags map {case (k, v) => Tag.of(k,v) } asJava))

    override def withNameToken(token: String): MetricsReceiver =
      new MemoizedMetricsReceiver(name + "_" + token, tags)

    override def withAdditionalTags(additionalTags: Map[String, String]): MetricsReceiver =
      new MemoizedMetricsReceiver(name, tags ++ additionalTags)
  }

  type MetricsConstructionArgs = (String, Map[String, String])
  private def apply: MetricsConstructionArgs => MetricsReceiver = Memoize { args => new MemoizedMetricsReceiver(args._1, args._2) }
  def root: MetricsReceiver = apply("diffy", Map.empty[String, String])
}

trait MetricsReceiver {
  def withNameToken(name: String): MetricsReceiver
  def withAdditionalTags(tags: Map[String, String]): MetricsReceiver
  def counter: Counter
}