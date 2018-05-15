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
    val canonicalResource = req.headerMap.get("Canonical-Resource")
    val body = req.getContentString()
    Future.value(Message(canonicalResource, FieldMap(Map("request"-> req.toString, "body" -> body))))
  }

  def liftResponse(resp: Try[Response]): Future[Message] = {
    Future.const(resp) flatMap { r: Response =>
      val mediaTypeOpt: Option[MediaType] =
        r.headerMap.get(HttpHeaders.CONTENT_TYPE) map { MediaType.parse }
      
      val contentLengthOpt = r.headerMap.get(HttpHeaders.CONTENT_LENGTH)

      /** header supplied by macaw, indicating the controller reached **/
      val controllerEndpoint = r.headerMap.get(ControllerEndpointHeaderName)

      (mediaTypeOpt, contentLengthOpt) match {
        /** When Content-Length is 0, only compare headers **/
        case (_, Some(length)) if length.toInt == 0 =>
          Future.const(
            Try(Message(controllerEndpoint, FieldMap(headersMap(r))))
          )

        /** When Content-Type is set as application/json, lift as Json **/
        case (Some(mediaType), _) if mediaType.is(MediaType.JSON_UTF_8) || mediaType.toString == "application/json" => {
          val jsonContentTry = Try {
            JsonLifter.decode(r.getContentString())
          }

          Future.const(jsonContentTry map { jsonContent =>
            val responseMap = Map(
              r.getStatusCode().toString -> (Map(
                "content" -> jsonContent,
                "chunked" -> r.isChunked
              ) ++ headersMap(r))
            )

            Message(controllerEndpoint, FieldMap(responseMap))
          }).rescue { case t: Throwable =>
            Future.exception(new MalformedJsonContentException(t))
          }
        }

        /** When Content-Type is set as text/html, lift as Html **/
        case (Some(mediaType), _)
          if mediaType.is(MediaType.HTML_UTF_8) || mediaType.toString == "text/html" => {
            val htmlContentTry = Try {
              HtmlLifter.lift(HtmlLifter.decode(r.getContentString()))
            }

            Future.const(htmlContentTry map { htmlContent =>
              val responseMap = Map(
                r.getStatusCode().toString -> (Map(
                  "content" -> htmlContent,
                  "chunked" -> r.isChunked
                ) ++ headersMap(r))
              )

              Message(controllerEndpoint, FieldMap(responseMap))
            })
          }

        /** When content type is not set, only compare headers **/
        case (None, _) => {
          Future.const(Try(
            Message(controllerEndpoint, FieldMap(headersMap(r)))))
        }

        case (Some(mediaType), _) => {
          log.debug(s"Content type: $mediaType is not supported")
          contentTypeNotSupportedExceptionFuture(mediaType.toString)
        }
      }
    }
  }
}
