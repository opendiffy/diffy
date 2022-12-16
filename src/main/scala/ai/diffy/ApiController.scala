package ai.diffy

import ai.diffy.analysis.{DifferencesFilterFactory, JoinedEndpoint}
import ai.diffy.proxy.ReactorHttpDifferenceProxy
import ai.diffy.repository.{DifferenceResultRepository, NoiseRepository}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, RequestParam, RestController}

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

@RestController
class ApiController(
    @Autowired noise: NoiseRepository,
    @Autowired repository: DifferenceResultRepository,
    @Autowired proxy: ReactorHttpDifferenceProxy,
    @Autowired settings: Settings)
{
  val MissingEndpointException = Renderer.error("Specify an endpoint")
  val MissingEndpointPathException = Renderer.error("Specify an endpoint and path")
  val RequestPurgedException = Renderer.error("Request purged")
  val IndexOutOfBoundsException = Renderer.error("Request index out of bounds")
  val thresholdFilter =
    DifferencesFilterFactory(settings.relativeThreshold, settings.absoluteThreshold)

  def endpointMap(
    ep: String,
    joinedEndpoint: JoinedEndpoint,
    includeWeights: Boolean,
    excludeNoise: Boolean
  ) =
    Map(
      "endpoint" -> Renderer.endpoint(joinedEndpoint.endpoint),
      "fields" ->
        Renderer.fields(
          // Only show fields that are above the thresholds
          if (excludeNoise) {
            val noisyFields = noise.findById(ep).map(_.noisyfields.toSeq).orElse(Nil)
            joinedEndpoint.fields.filter { case ((path, field)) =>
              thresholdFilter(field) && !noisyFields.exists(path.startsWith)
            }
          } else {
            joinedEndpoint.fields
          },
          includeWeights
        )
    )

  @GetMapping(path = Array("/api/1/overview"))
  def getOverview(
     @RequestParam(name = "exclude_noise", defaultValue = "true") excludeNoise: Boolean,
     @RequestParam(name = "include_weights", defaultValue = "false") includeWeights: Boolean
 ) : Map[String, Any] = {
    proxy.joinedDifferences.endpoints match { case eps =>
      eps.map { case (endpoint, diffs) =>
        endpoint -> endpointMap(endpoint, diffs, includeWeights, excludeNoise)
      }
    }
  }

  @GetMapping(path = Array("/api/1/endpoints"))
  def getEndpoints(
    @RequestParam(name = "exclude_noise", defaultValue = "false") excludeNoise: Boolean
  ): Map[String, Any] = {
    Renderer.endpoints(
      proxy.joinedDifferences.raw.counter.endpoints filterNot  { case (ep, meta) => {
        excludeNoise &&
          getStats(ep, excludeNoise, false)
            .getOrElse("fields", Map.empty)
            .asInstanceOf[Map[String, _]]
            .isEmpty
      }}
    )
  }

  @GetMapping(path = Array("/api/1/endpoints/{endpoint}/stats"))
  def getStats(
    @PathVariable(name = "endpoint") endpoint: String,
    @RequestParam(name = "exclude_noise", defaultValue = "true") excludeNoise: Boolean,
    @RequestParam(name = "include_weights", defaultValue = "false") includeWeights: Boolean
  ) = {
    if (endpoint.isEmpty) {
      MissingEndpointException
    } else {
      try {
        proxy.joinedDifferences.endpoint(endpoint) match { case joinedEndpoint =>
          endpointMap(endpoint, joinedEndpoint, includeWeights, excludeNoise)
        }
      } catch {
        case t: Throwable => Renderer.error(t.getMessage())
      }
    }
  }

  @GetMapping(path = Array("/api/1/endpoints/{endpoint}/fields/{path}/results"))
  def getResults(
    @PathVariable("endpoint") endpoint: String,
    @PathVariable("path") path: String,
    @RequestParam(name = "include_request", defaultValue = "false") include_request: Boolean) = {
    if(endpoint.isEmpty || path.isEmpty) {
      MissingEndpointPathException
    } else {
      val drs = proxy.collector.prefix(analysis.Field(endpoint, path))
      Map(
        "endpoint" -> endpoint,
        "path" -> path,
        "requests" ->
          Renderer.differenceResults(
            drs,
            include_request
          )
      )
    }
  }

  @GetMapping(path = Array("/api/1/endpoints/{endpoint}/fields/{path}/results/{index}"))
  def getIndex(
    @PathVariable("endpoint") endpoint: String,
    @PathVariable("path") path: String,
    @PathVariable("index") index: Int,
    @RequestParam(name = "include_request", defaultValue = "true") includeRequest: Boolean
  ) = {
    if (endpoint.isEmpty || path.isEmpty) {
      IndexOutOfBoundsException
    } else {
        proxy.collector.prefix(analysis.Field(endpoint, path)).toSeq.lift(index) match {
          case Some(dr) =>
            Renderer.differenceResult(
              dr,
              includeRequest
            )

          case None => IndexOutOfBoundsException
        }
    }
  }

  @GetMapping(path = Array("/api/1/requests/{id}"))
  def getRequest(
    @PathVariable("id") id: String,
    @RequestParam(name = "include_request", defaultValue = "true") includeRequest: Boolean
  ) = {
    if(id.isEmpty) {
      RequestPurgedException
    } else {
      repository.findById(id) map { case dr =>
        Renderer.differenceResult(
          dr,
          includeRequest
        )
      } orElse(RequestPurgedException)
    }
  }

  @GetMapping(path = Array("/api/1/clear"))
  def clear() = {
    proxy.clear()
    Renderer.success("Diffs cleared")
  }

  @GetMapping(path = Array("/api/1/info"))
  def getInfo(): Map[String, _] = Map(
        "name" -> settings.serviceName,
        "candidate" -> httpServiceToMap(settings.candidate.toString),
        "primary" -> httpServiceToMap(settings.primary.toString),
        "secondary" -> httpServiceToMap(settings.secondary.toString),
        "relativeThreshold" -> settings.relativeThreshold,
        "absoluteThreshold" -> settings.absoluteThreshold,
        "protocol" -> "http",
        "last_reset" -> proxy.lastReset.getTime)


  private[this] def httpServiceToMap(target: String) = Map("target" -> target)
}


