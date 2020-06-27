package ai.diffy.proxy

import java.net.SocketAddress

import ai.diffy.analysis.{DifferenceAnalyzer, InMemoryDifferenceCollector, JoinedDifferences}
import ai.diffy.lifter.{HttpLifter, Message}
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.{Filter, Http}
import com.twitter.util.{Future, StorageUnit, Try}

object HttpDifferenceProxy {
  def requestHostHeaderFilter(host: String) =
    Filter.mk[Request, Response, Request, Response] { (req, svc) =>
      req.host(host)
      svc(req)
    }
}

trait HttpDifferenceProxy extends DifferenceProxy {
  import HttpDifferenceProxy._
  val servicePort: SocketAddress
  val lifter = new HttpLifter(settings.excludeHttpHeadersComparison, settings.resourceMatcher)

  override type Req = Request
  override type Rep = Response
  override type Srv = HttpService

  override def serviceFactory(serverset: String, label: String) =
    HttpService(requestHostHeaderFilter(serverset) andThen
      Http.client
        .withMaxResponseSize(settings.maxResponseSize)
        .withMaxHeaderSize(settings.maxHeaderSize)
        .newService(serverset, label))

  override lazy val server =
    Http.serve(
      servicePort,
      proxy
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
