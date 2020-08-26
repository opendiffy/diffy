package ai.diffy.lifter

import java.util.AbstractMap

import ai.diffy.lifter.StringLifter.htmlRegexPattern
import ai.diffy.util.ResourceMatcher
import com.twitter.finagle.http.{ParamMap, Request, Response}
import com.twitter.logging.Logger
import com.twitter.util.{Future, Try}
import java.util.{AbstractMap, Map => JMap, List => JList}
import scala.collection.JavaConverters._

import scala.util.control.NoStackTrace


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

class HttpLifter(excludeHttpHeadersComparison: Boolean, resourceMatcher: Option[ResourceMatcher] = None, sensitiveParameters: Option[Set[String]] = None) {

  import HttpLifter._

  private[this] val log = Logger(classOf[HttpLifter])

  private[this] def headersMap(response: Response): Map[String, Any] = {
    if (!excludeHttpHeadersComparison) {
      val rawHeaders = response.headerMap.toSeq

      val headers = rawHeaders map { case (name, value) =>
        (name.toLowerCase, value)
      } groupBy {
        _._1
      } map { case (name, pairs) =>
        name -> (pairs map {
          _._2
        } sorted)
      }

      Map("headers" -> FieldMap(headers))
    } else Map.empty
  }

  def liftSensitiveParams(sensitive: Set[String], req: Request) : (JList[JMap.Entry[String, String]], String) =
  {
    val splitBit = req.uri.split("\\?", 2)
    val redactedURI = splitBit(0) + (if (splitBit.length == 1) "" else "?" +
      redactContainer(splitBit(1).split('&').map(bit => {
        val stuff = bit.split("=", 2)
        if (stuff.size > 1) (stuff(0), stuff(1)) else (stuff(0), "")
      }), sensitive).map { case (key: String, value: String) => key + '=' + value }.mkString("&"))
    val redactedParameters = redactContainer(req.params, sensitive).toList.map {
      case (k, v) => new AbstractMap.SimpleImmutableEntry(k, v).asInstanceOf[JMap.Entry[String, String]]
    }
    (redactedParameters.asJava, redactedURI)
  }

  def liftRequest(req: Request): Future[Message] = {
    val headers = req.headerMap

    val canonicalResource = headers
      .get("Canonical-Resource")
      .orElse(resourceMatcher.flatMap(_.resourceName(req.path)))

    val (params: JList[JMap.Entry[String, String]], uri) = (sensitiveParameters match {
      case None => (req.getParams(), req.uri)
      case Some(sensitive) => liftSensitiveParams(sensitive, req)
    })

    val body = liftBody(req.getContentString())
    Future.value(
      Message(
        canonicalResource,
        FieldMap(
          Map(
            "uri" -> uri,
            "method" -> req.method,
            "headers" -> headers,
            "params" -> params,
            "body" -> body
          )
        )
      )
    )
  }


  object KeyValParseError extends Exception with NoStackTrace

  // Converts a key = value representation to something Diffy can understand
  def liftText(string: String): Any = {
    val asMap = string.split('\n').foldLeft( Map.empty[String,Any] )(
      (map, line) =>
      {
        val index = line indexOf '='
        if (index == -1)
          throw KeyValParseError
        val startString: String = line.substring(0, index).trim()
        val endString: String  = line.substring(index + 1).trim()
        val endBit = Try(JsonLifter.lift(JsonLifter.decode(endString))).getOrElse(endString)
        map + (startString -> (map.get(startString) match {
          case Some(a: Seq[Any]) => (a :+ endBit)
          case Some(a: Any) => Seq(a, endBit)
          case None => endBit
        }))
      })
    FieldMap(asMap)
  }

  def redactContainer[T >: String](traversable: TraversableOnce[(String, T)], sensitive: Set[String]): TraversableOnce[(String, T)] =
  {
    traversable.map { case (key: String, value) =>
      (key,
        if (sensitive.contains(key))
          value match {
            case strVal: String => strVal.map(_ => 'x')
            case _ => "redacted"
          }
        else
          value)
    }
  }

  def redactBody(unredacted: Any): Any = {
    (sensitiveParameters, unredacted) match {
      case (Some(sensitive), fieldMap: FieldMap[Any])  => FieldMap(redactContainer(fieldMap, sensitive).toMap)
      case _ => unredacted
    }
  }

  def liftBody(string: String): Any = {
    Try(FieldMap(Map("type" -> "json", "value" -> redactBody(JsonLifter.lift(JsonLifter.decode(string)))))).getOrElse {
      Try(FieldMap(Map("type" -> "text", "value" -> redactBody(liftText(string))))).getOrElse {
        if (htmlRegexPattern.findFirstIn(string).isDefined)
          FieldMap(Map("type" -> "html", "value" -> HtmlLifter.lift(HtmlLifter.decode(string))))
        else string
      }
    }
  }

  def liftResponse(resp: Try[Response]): Future[Message] = {
    log.debug(s"$resp")
    Future.const(resp) flatMap { r: Response =>

      /** header supplied by macaw, indicating the controller reached **/
      val controllerEndpoint = r.headerMap.get(ControllerEndpointHeaderName)

      val stringContentTry = Try {
        liftBody(r.getContentString())
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
