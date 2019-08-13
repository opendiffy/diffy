package ai.diffy.lifter

import com.google.common.net.{HttpHeaders, MediaType}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.logging.Logger
import com.twitter.util.{Future, Try}

object HttpLifter {
  val ControllerEndpointHeaderName = "X-Action-Name"

  def contentTypeNotSupportedException(contentType: String) = new Exception(s"Content type: $contentType is not supported")
  def contentTypeNotSupportedExceptionFuture(contentType: String) = Future.exception(contentTypeNotSupportedException(contentType))

  case class MalformedJsonContentException(cause: Throwable)
    extends Exception("Malformed Json content")
  {
    initCause(cause)
  }
}

class HttpLifter(excludeHttpHeadersComparison: Boolean) {
  import HttpLifter._

  private[this] val log = Logger(classOf[HttpLifter])
  private[this] def headersMap(response: Response): Map[String, Any] = {
    if(!excludeHttpHeadersComparison) {
      val rawHeaders = response.headerMap.toSeq

      val headers = rawHeaders groupBy { case (name, _) => name } map { case (name, values) =>
        name -> (values map { case (_, value) => value } sorted)
      }

      Map( "headers" -> FieldMap(headers))
    } else Map.empty
  }

  def liftRequest(req: Request): Future[Message] = {
    val headers = req.headerMap
    val canonicalResource = headers.get("Canonical-Resource")
    val params = req.getParams()
    val body = StringLifter.lift(req.getContentString())
    Future.value(
      Message(
        canonicalResource,
        FieldMap(
          Map(
            "request"-> req.toString,
            "headers" -> headers,
            "params" -> params,
            "body" -> body
          )
        )
      )
    )
  }

  def liftResponse(resp: Try[Response]): Future[Message] = {
    log.debug(s"$resp")
    Future.const(resp) flatMap { r: Response =>

      /** header supplied by macaw, indicating the controller reached **/
      val controllerEndpoint = r.headerMap.get(ControllerEndpointHeaderName)

      val stringContentTry = Try {
        StringLifter.lift(r.getContentString())
      }

      Future.const(stringContentTry map { stringContent =>
        val responseMap = Map(
          r.statusCode.toString -> (Map(
            "content" -> stringContent,
            "chunked" -> r.isChunked
          ) ++ headersMap(r))
        )

        Message(controllerEndpoint, FieldMap(responseMap))
      })

    }
  }
}
