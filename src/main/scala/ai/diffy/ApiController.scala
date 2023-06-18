package ai.diffy

import ai.diffy.analysis.{DifferencesFilterFactory, DynamicAnalyzer, JoinedEndpoint}
import ai.diffy.repository.{DifferenceResultRepository, NoiseRepository}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, PostMapping, RequestParam, RestController}

import java.util.Date
import scala.jdk.CollectionConverters._

@RestController
class ApiController(
    @Autowired noise: NoiseRepository,
    @Autowired repository: DifferenceResultRepository,
    @Autowired settings: Settings)
{
  val dynamicAnalyzer = new DynamicAnalyzer(repository)
  def proxy(start: Long, end: Long) = dynamicAnalyzer.filter(start, end)
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
            val noisyFields = noise.findById(ep).map(_.noisyfields.asScala.toSeq).orElse(Nil)
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
     @RequestParam(name = "include_weights", defaultValue = "false") includeWeights: Boolean,
     @RequestParam(name = "start", defaultValue = "0") start: Long,
     @RequestParam(name = "end", defaultValue = "1701001001000") end: Long
 ) : Map[String, Any] = {
    proxy(start, end).joinedDifferences.endpoints match { case eps =>
      eps.map { case (endpoint, diffs) =>
        endpoint -> endpointMap(endpoint, diffs, includeWeights, excludeNoise)
      }
    }
  }

  @GetMapping(path = Array("/api/1/endpoints"))
  def getEndpoints(
    @RequestParam(name = "exclude_noise", defaultValue = "false") excludeNoise: Boolean,
    @RequestParam(name = "start", defaultValue = "0") start: Long,
    @RequestParam(name = "end", defaultValue = "1701001001000") end: Long
  ): Map[String, Any] = {
    Renderer.endpoints(
      proxy(start, end).joinedDifferences.raw.counter.endpoints filterNot  { case (ep, meta) => {
        excludeNoise &&
          getStats(ep, excludeNoise, false, start, end)
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
    @RequestParam(name = "include_weights", defaultValue = "false") includeWeights: Boolean,
    @RequestParam(name = "start", defaultValue = "0") start: Long,
    @RequestParam(name = "end", defaultValue = "1701001001000") end: Long
  ) = {
    if (endpoint.isEmpty) {
      MissingEndpointException
    } else {
      try {
        proxy(start, end).joinedDifferences.endpoint(endpoint) match { case joinedEndpoint =>
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
    @RequestParam(name = "include_request", defaultValue = "false") include_request: Boolean,
    @RequestParam(name = "start", defaultValue = "0") start: Long,
    @RequestParam(name = "end", defaultValue = "1701001001000") end: Long
                ) = {
    if(endpoint.isEmpty || path.isEmpty) {
      MissingEndpointPathException
    } else {
      val drs = proxy(start, end).collector.prefix(analysis.Field(endpoint, path))
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
    @RequestParam(name = "include_request", defaultValue = "true") includeRequest: Boolean,
    @RequestParam(name = "start", defaultValue = "0") start: Long,
    @RequestParam(name = "end", defaultValue = "1701001001000") end: Long
  ) = {
    if (endpoint.isEmpty || path.isEmpty) {
      IndexOutOfBoundsException
    } else {
        proxy(start, end).collector.prefix(analysis.Field(endpoint, path)).toSeq.lift(index) match {
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
    @RequestParam(name = "include_request", defaultValue = "true") includeRequest: Boolean,
    @RequestParam(name = "start", defaultValue = "0") start: Long,
    @RequestParam(name = "end", defaultValue = "1701001001000") end: Long
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
//    proxy.clear()
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
        "protocol" -> "http")


  private[this] def httpServiceToMap(target: String) = Map("target" -> target)
}


