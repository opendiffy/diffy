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
    val headers = req.getHeaders.asScala.toMap

    val canonicalResource: Option[String] = headers
      .get("Canonical-Resource")
      .orElse(resourceMatcher.flatMap(_.resourceName(req.getPath)))
      .orElse(Some(s"${req.getMethod}:${req.getPath}"))

    val params = req.getParams
    val body = StringLifter.lift(req.getBody)
      Message(
        canonicalResource,
        new FieldMap(
          Map(
            "method" -> req.getMethod,
            "path" -> req.getPath,
            "uri" -> req.getUri,
            "headers" -> headers,
            "params" -> params,
            "body" -> body
          )
        )
      )
  }

  def liftResponse(r: HttpResponse): Message = {
    val responseMap = Map(r.getStatus -> StringLifter.lift(r.getBody())) ++ headersMap(r)
    Message(None, new FieldMap(responseMap))
  }
}
