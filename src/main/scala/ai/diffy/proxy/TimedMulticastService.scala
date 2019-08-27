package ai.diffy.proxy

import com.twitter.finagle.Service
import com.twitter.util.{Future, Try}

class TimedMulticastService[-A, +B](services: Seq[Service[A, B]])
  extends Service[A, Seq[(Try[B], Long, Long)]]
{
  def apply(request: A): Future[Seq[(Try[B], Long, Long)]] =
    services.foldLeft[Future[Seq[(Try[B], Long, Long)]]](Future.Nil){ case (acc, service) =>
      acc flatMap { responseTriesWithTiming =>
        val start = System.currentTimeMillis()
        val nextResponse: Future[Try[B]] = service(request).liftToTry
        nextResponse map { responseTry: Try[B] => {
          val end  = System.currentTimeMillis()
          responseTriesWithTiming ++ Seq((responseTry, start, end))
        }}
      }
    }
}
