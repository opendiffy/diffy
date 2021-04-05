package ai.diffy.proxy

import com.twitter.finagle.Service
import com.twitter.util.{Future, Try}
import com.twitter.finagle.http.Request

case class Server(classifier: Int)

class TimedMulticastService[-A, +B, C](services: Seq[Service[A, B]], apiRoots : Seq[C])
  extends Service[A, Seq[(Try[B], Long, Long)]]
{
  var requestCount = 0
  var dest = ""

  def addApiRoot(server: Server, request: Request): Unit = {
    val apiRoot = server match {
      case Server(0) => apiRoots(0).toString
      case Server(1) => apiRoots(1).toString
      case Server(2) => apiRoots(2).toString
    }
    request.uri(dest + apiRoot)
  }

  def apply(request: A): Future[Seq[(Try[B], Long, Long)]] =
    services.foldLeft[Future[Seq[(Try[B], Long, Long)]]](Future.Nil){ case (acc, service) =>
      acc flatMap { responseTriesWithTiming =>
        if (request.isInstanceOf[Request]) {
          if (dest.equals(""))
            dest = request.asInstanceOf[Request].uri
          addApiRoot(Server(requestCount), request.asInstanceOf[Request])
          requestCount += 1
          if (requestCount == 3)
            requestCount = 0
        }

        val start = System.currentTimeMillis()
        val nextResponse: Future[Try[B]] = service(request).liftToTry
        nextResponse map { responseTry: Try[B] => {
          val end  = System.currentTimeMillis()
          responseTriesWithTiming ++ Seq((responseTry, start, end))
        }}
      }
    }
}
