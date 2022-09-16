package ai.diffy.lifter

import ai.diffy.Settings
import ai.diffy.proxy.{HttpMessage, HttpRequest, HttpResponse}
import ai.diffy.util.ResourceMatcher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.collection.JavaConverters._

object HttpLifter {


  val ControllerEndpointHeaderName = "X-Action-Name"

  def contentTypeNotSupportedException(contentType: String) = new Exception(s"Content type: $contentType is not supported")

  case class MalformedJsonContentException(cause: Throwable)
    extends Exception("Malformed Json content")
  {
    initCause(cause)
  }
}

class HttpLifter(settings: Settings) {
  val excludeHttpHeadersComparison: Boolean = settings.excludeHttpHeadersComparison
  val resourceMatcher: Option[ResourceMatcher] = settings.resourceMatcher

  import HttpLifter._

  private[this] def headersMap(response: HttpMessage): Map[String, Any] = {
    if(!excludeHttpHeadersComparison) {
      Map( "headers" -> new FieldMap(response.getHeaders.asScala.toMap))
    } else Map.empty
  }

  def liftRequest(req: HttpRequest): Message = {
    val headers = req.getMessage.getHeaders.asScala.toMap

    val canonicalResource: Option[String] = headers
      .get("Canonical-Resource")
      .orElse(resourceMatcher.flatMap(_.resourceName(req.getPath)))

    val params = req.getParams
    val body = StringLifter.lift(req.getMessage.getBody)
      Message(
        canonicalResource,
        new FieldMap(
          Map(
            "uri" -> req.getUri,
            "method" -> req.getMethod,
            "headers" -> headers,
            "params" -> params,
            "body" -> body
          )
        )
      )
  }

  def liftResponse(r: HttpResponse): Message = {
    /** header supplied by macaw, indicating the controller reached **/
    val controllerEndpoint = r.getMessage.getHeaders.asScala.toMap.get(ControllerEndpointHeaderName)
    val responseMap = Map(r.getStatus -> StringLifter.lift(r.getMessage.getBody())) ++ headersMap(r.getMessage)
    Message(controllerEndpoint, new FieldMap(responseMap))
  }
}
