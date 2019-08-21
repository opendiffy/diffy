package ai.diffy

import javax.inject.Inject
import ai.diffy.analysis.{DifferencesFilterFactory, JoinedEndpoint}
import ai.diffy.proxy._
import ai.diffy.util.DiffyProject
import ai.diffy.workflow.{FunctionalReport, ReportGenerator}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.Controller
import com.twitter.util._

object ApiController {
  val MissingEndpointException = Future.value(Renderer.error("Specify an endpoint"))
  val MissingEndpointPathException = Future.value(Renderer.error("Specify an endpoint and path"))
  val RequestPurgedException = Future.value(Renderer.error("Request purged"))
  val IndexOutOfBoundsException = Future.value(Renderer.error("Request index out of bounds"))
}

class ApiController @Inject()(
    proxy: DifferenceProxy,
    settings: Settings,
    workflow: FunctionalReport,
    reportGenerator: ReportGenerator)
  extends Controller
{
  import ApiController._
  DiffyProject.settings(settings)
  val thresholdFilter =
    DifferencesFilterFactory(settings.relativeThreshold, settings.absoluteThreshold)

  def endpointMap(
    JoinedEndpoint: JoinedEndpoint,
    includeWeights: Boolean,
    excludeNoise: Boolean
  ) =
    Map(
      "endpoint" -> Renderer.endpoint(JoinedEndpoint.endpoint),
      "fields" ->
        Renderer.fields(
          // Only show fields that are above the thresholds
          if (excludeNoise) {
            JoinedEndpoint.fields.filter { case ((_, field)) =>
              thresholdFilter(field)
            }
          } else {
            JoinedEndpoint.fields
          },
          includeWeights
        )
    )

  get("/api/1/overview") { req: Request =>
    DiffyProject.log(req.uri)
    val excludeNoise = req.params.getBooleanOrElse("exclude_noise", true)
    val includeWeights = req.params.getBooleanOrElse("include_weights", false)

    proxy.joinedDifferences.endpoints map { eps =>
      eps.map { case (endpoint, diffs) =>
        endpoint -> endpointMap(diffs, includeWeights, excludeNoise)
      }
    }
  }

  get("/api/1/report") { req: Request =>
    DiffyProject.log(req.uri)
    reportGenerator.extractReportFromDifferences
  }

  get("/api/1/endpoints") { req: Request =>
    DiffyProject.log(req.uri)
    proxy.joinedDifferences.raw.counter.endpoints map Renderer.endpoints
  }

  get("/api/1/endpoints/:endpoint/stats") { req: Request =>
    DiffyProject.log(req.uri)
    val excludeNoise = req.params.getBooleanOrElse("exclude_noise", true)
    val includeWeights = req.params.getBooleanOrElse("include_weights", false)

    req.params.get("endpoint") match {
      case Some(endpoint) =>
        proxy.joinedDifferences.endpoint(endpoint) map { case joinedEndpoint =>
          endpointMap(joinedEndpoint, includeWeights, excludeNoise)
        } handle {
          case t: Throwable => Renderer.error(t.getMessage())
        }

      case None => MissingEndpointException
    }
  }

  get("/api/1/endpoints/:endpoint/fields/:path/results") { req: Request =>
    DiffyProject.log(req.uri)
    (
      req.params.get("endpoint"),
      req.params.get("path")
      ) match {
      case (Some(endpoint), Some(path)) =>
        proxy.collector.prefix(analysis.Field(endpoint, path)) map { drs =>
          Map(
            "endpoint" -> endpoint,
            "path" -> path,
            "requests" ->
              Renderer.differenceResults(
                drs,
                req.params.getBooleanOrElse("include_request", false)
              )
          )
        }

      case _ => MissingEndpointPathException
    }
  }

  get("/api/1/endpoints/:endpoint/fields/:path/results/:index") { req: Request =>
    DiffyProject.log(req.uri)
    (
      req.params.get("endpoint"),
      req.params.get("path"),
      req.params.getInt("index")
      ) match {
      case (Some(endpoint), Some(path), Some(index)) =>
        proxy.collector.prefix(analysis.Field(endpoint, path)) map {
          _.toSeq.lift(index)
        } map {
          case Some(dr) =>
            Renderer.differenceResult(
              dr,
              req.params.getBooleanOrElse("include_request", true)
            )

          case None => IndexOutOfBoundsException
        }

      case _ => MissingEndpointPathException
    }
  }

  get("/api/1/requests/:id") { req: Request =>
    DiffyProject.log(req.uri)
    req.params.get("id") map { id =>
      proxy.collector(id.toLong) map { dr =>
        Renderer.differenceResult(
          dr,
          req.params.getBooleanOrElse("include_request", true)
        )
      }
    } match {
      case Some(request) => request
      case None => RequestPurgedException
    }
  }

  get("/api/1/clear") { req: Request =>
    DiffyProject.log(req.uri)
    proxy.clear() map { _ =>
      workflow.schedule()
      Renderer.success("Diffs cleared")
    }
  }

  post("/api/1/email") { req: Request =>
    DiffyProject.log(req.uri)
    reportGenerator.sendEmail
    Renderer.success("Sent report e-mail.")
  }

  get("/api/1/info") { req: Request =>
    DiffyProject.log(req.uri)
    (proxy match {
      case thrift: ThriftDifferenceProxy => Map(
        "candidate" -> thriftServiceToMap(settings.candidate, thrift.candidate),
        "primary" -> thriftServiceToMap(settings.primary, thrift.primary),
        "secondary" -> thriftServiceToMap(settings.secondary, thrift.secondary),
        "thrift_jar" -> settings.pathToThriftJar,
        "service_class" -> settings.serviceClass,
        "service_name" -> settings.serviceName,
        "client_id" -> settings.clientId,
        "threshold_relative" -> settings.relativeThreshold,
        "threshold_absolute" -> settings.absoluteThreshold,
        "protocol" -> "thrift"
      )

      case http: HttpDifferenceProxy => Map(
        "candidate" -> httpServiceToMap(settings.candidate, http.candidate),
        "primary" -> httpServiceToMap(settings.primary, http.primary),
        "secondary" -> httpServiceToMap(settings.secondary, http.secondary),
        "protocol" -> "http"
      )
    }) ++ Map("last_reset" -> proxy.lastReset.inMillis)
  }

  private[this] def thriftServiceToMap(target: String, srv: ThriftService) =
      Map(
        "target" -> target,
        "members" -> srv.members,
        "valid" -> srv.serversetValid,
        "last_changed" -> srv.changedAt.map(_.inMillis).getOrElse(-1),
        "last_reset" -> srv.resetAt.map(_.inMillis).getOrElse(-1)
      )

  private[this] def httpServiceToMap(target: String, srv: HttpService) =
    Map(
      "target" -> target
    )
}


class AllowLocalAccess extends SimpleFilter[Request, Response] {
  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    service(request) map { response =>
      response.headerMap.set("Access-Control-Allow-Origin", "http://localhost:8888")
      response
    }
  }
}


