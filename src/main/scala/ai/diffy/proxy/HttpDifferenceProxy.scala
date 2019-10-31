package ai.diffy.proxy

import java.net.SocketAddress

import ai.diffy.analysis.{DifferenceAnalyzer, InMemoryDifferenceCollector, JoinedDifferences}
import ai.diffy.lifter.{HttpLifter, Message}
import ai.diffy.proxy.DifferenceProxy.NoResponseException
import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.finagle.{Filter, Http, Service}
import com.twitter.util.{Future, Try}

object HttpDifferenceProxy {
  val okResponse = Future.value(Response(Status.Ok))

  val noResponseExceptionFilter =
    new Filter[Request, Response, Request, Response] {
      override def apply(
        request: Request,
        service: Service[Request, Response]
      ): Future[Response] = {
        service(request).rescue[Response] { case NoResponseException =>
          okResponse
        }
      }
    }
}

trait HttpDifferenceProxy extends DifferenceProxy {
  val servicePort: SocketAddress
  val lifter = new HttpLifter(settings.excludeHttpHeadersComparison, settings.resourceMatcher)

  override type Req = Request
  override type Rep = Response
  override type Srv = HttpService

  override def serviceFactory(serverset: String, label: String) =
    HttpService(Http.newClient(serverset, label).toService)

  override lazy val server =
    Http.serve(
      servicePort,
      HttpDifferenceProxy.noResponseExceptionFilter andThen proxy
    )

  override def liftRequest(req: Request): Future[Message] =
    lifter.liftRequest(req)

  override def liftResponse(resp: Try[Response]): Future[Message] =
    lifter.liftResponse(resp)
}

object SimpleHttpDifferenceProxy {
  /**
   * Side effects can be dangerous if replayed on production backends. This
   * filter ignores all POST, PUT, and DELETE requests if the
   * "allowHttpSideEffects" flag is set to false.
   */
  lazy val httpSideEffectsFilter =
    Filter.mk[Request, Response, Request, Response] { (req, svc) =>
      val hasSideEffects =
        Set(Method.Post, Method.Put, Method.Delete).map(_.toString()).contains(req.method.toString())

      if (hasSideEffects) DifferenceProxy.NoResponseExceptionFuture else svc(req)
    }
}

/**
 * A Twitter-specific difference proxy that adds custom filters to unpickle
 * TapCompare traffic from TFE and optionally drop requests that have side
 * effects
 * @param settings    The settings needed by DifferenceProxy
 */
case class SimpleHttpDifferenceProxy (
    settings: Settings,
    collector: InMemoryDifferenceCollector,
    joinedDifferences: JoinedDifferences,
    analyzer: DifferenceAnalyzer)
  extends HttpDifferenceProxy
{
  import SimpleHttpDifferenceProxy._

  override val servicePort = settings.servicePort
  override val proxy =
    Filter.identity andThenIf
      (!settings.allowHttpSideEffects, httpSideEffectsFilter) andThen
      super.proxy
}

/**
 * Alternative to SimpleHttpDifferenceProxy allowing HTTPS requests
 */
case class SimpleHttpsDifferenceProxy (
   settings: Settings,
   collector: InMemoryDifferenceCollector,
   joinedDifferences: JoinedDifferences,
   analyzer: DifferenceAnalyzer)
  extends HttpDifferenceProxy
{
  import SimpleHttpDifferenceProxy._

  override val servicePort = settings.servicePort

  override val proxy =
    Filter.identity andThenIf
      (!settings.allowHttpSideEffects, httpSideEffectsFilter) andThen
      super.proxy

  override def serviceFactory(serverset: String, label: String) =
    HttpService(
      Http.client
      .withTls(serverset)
      .newService(serverset+":"+settings.httpsPort, label)
    )
}
