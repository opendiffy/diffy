package ai.diffy.workflow

import ai.diffy.analysis.{EndpointMetadata, RawDifferenceCounter}
import ai.diffy.util.EmailSender
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.logging.Logger
import com.twitter.util.{Duration, Memoize, Time, Timer}
import javax.inject.Inject

trait Workflow {
  val log: Logger = Logger(classOf[Workflow])
  val emailSender = new EmailSender(log)
  val delay: Duration

  val timer: Timer
  def run(start: Time): Unit

  def schedule(): Unit = {
    val start = Time.now
    log.info(s"Scheduling ${getClass.getName} at $start")

    timer.doLater(delay) {
      run(start)
    }
  }
}

class DifferenceStatsMonitor @Inject()(
     diffCounter: RawDifferenceCounter,
     stats: StatsReceiver,
     override val timer: Timer = DefaultTimer.twitter)
  extends Workflow
{
  val delay = Duration.fromMinutes(1)
  val scope = stats.scope("raw").scope("endpoints")
  private[this] val addGauges = Memoize[(String, EndpointMetadata), Unit] { case (endpoint, meta) =>
    scope.scope(endpoint).provideGauge("total"){ meta.total }
    scope.scope(endpoint).provideGauge("differences"){ meta.differences }
  }

  override def run(start: Time) = {
    diffCounter.counter.endpoints foreach { endPoints =>
      endPoints map { addGauges }
    }
  }

  override def schedule() = {
    val start = Time.now
    timer.schedule(delay) { run(start) }
  }
}