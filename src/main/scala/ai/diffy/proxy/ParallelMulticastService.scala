package ai.diffy.proxy

import com.twitter.finagle.Service
import com.twitter.util.{Future, Try}

class ParallelMulticastService[-A, +B](
    services: Seq[Service[A, B]])
  extends Service[A, Seq[(Try[B], Long, Long)]]
{
  def apply(request: A): Future[Seq[(Try[B], Long, Long)]] = {
    Future.collect(services map {
      srv => {
        val start = System.currentTimeMillis()
        srv(request).liftToTry map { res =>
          val end = System.currentTimeMillis()
          (res, start, end)
        }
      }
    })
  }
}