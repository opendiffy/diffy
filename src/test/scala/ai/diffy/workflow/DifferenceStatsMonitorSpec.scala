package ai.diffy.workflow

import ai.diffy.ParentSpec
import ai.diffy.analysis.{DifferenceCounter, EndpointMetadata, RawDifferenceCounter}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.util.{Duration, Future, MockTimer, Time}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DifferenceStatsMonitorSpec extends ParentSpec {
  describe("DifferenceStatsMonitor"){
    val diffCounter = mock[DifferenceCounter]
    val metadata =
      new EndpointMetadata {
        override val differences = 0
        override val total = 0
      }

    val endpoints = Map("endpointName" -> metadata)
    when(diffCounter.endpoints) thenReturn Future.value(endpoints)

    val stats = new InMemoryStatsReceiver
    val timer = new MockTimer
    val monitor = new DifferenceStatsMonitor(RawDifferenceCounter(diffCounter), stats, timer)

    it("must add gauges after waiting a minute"){
      Time.withCurrentTimeFrozen { tc =>
        monitor.schedule()
        timer.tasks.size must be(1)
        stats.gauges.size must be(0)
        tc.advance(Duration.fromMinutes(1))
        timer.tick()
        timer.tasks.size must be(1)
        stats.gauges.size must be(2)
        stats.gauges.keySet map { _.takeRight(2) } must be(Set(Seq("endpointName", "total"), Seq("endpointName", "differences")))
      }
    }
  }
}
