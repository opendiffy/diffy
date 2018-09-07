package ai.diffy.lifter

import ai.diffy.ParentSpec
import ai.diffy.lifter.HttpLifter.MalformedJsonContentException
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.net.MediaType
import com.twitter.finagle.http._
import com.twitter.util.{Await, Throw, Try}
import org.jboss.netty.handler.codec.http._
import org.junit.runner.RunWith
import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[JUnitRunner])
class HttpLifterSpec extends ParentSpec {
  object Fixture {
    val reqUri = "http://localhost:0/0/accounts"

    val jsonContentType = MediaType.JSON_UTF_8.toString
    val textContentType = MediaType.PLAIN_TEXT_UTF_8.toString
    val htmlContentType = MediaType.HTML_UTF_8.toString

    val controllerEndpoint = "account/index"

    val validJsonBody =
      "{" +
        "\"data_type\": \"account\"," +
        "\"data\": [" +
          "{" +
            "\"name\": \"Account 1\"," +
            "\"deleted\": false" +
          "}," +
          "{" +
            "\"name\": \"Account 2\"," +
            "\"deleted\": true" +
          "}" +
        "]," +
        "\"total_count\": 2," +
        "\"next_cursor\": null" +
      "}"

    val invalidJsonBody = "invalid"

    val validHtmlBody = """<html><head><title>Sample HTML</title></head><body><div class="header"><h1 class="box">Hello World</h1></div><p>Lorem ipsum dolor sit amet.</p></body></html>"""

    val testException = new Exception("test exception")

    def request(method: Method, uri: String, body: Option[String] = None): Request = {
      val req = Request(Version.Http11, method, uri)
      body foreach { req.setContentString }
      req
    }

    def response(status: HttpResponseStatus, body: String): Response = {

      val resp = Response()
      resp.setStatusCode(status.getCode)
      resp.headerMap
        .add(HttpHeaders.Names.CONTENT_LENGTH, body.length.toString)
        .add(HttpHeaders.Names.CONTENT_TYPE, jsonContentType)
        .add(HttpLifter.ControllerEndpointHeaderName, controllerEndpoint)
      resp.setContentString(body)
      resp
    }
  }

  describe("HttpLifter") {
    import Fixture._
    describe("LiftRequest") {
      it("lift simple Get request") {
        val lifter = new HttpLifter(false)
        val req = request(Method.Get, reqUri)
        req.headerMap.add("Canonical-Resource", "endpoint")

        val msg = Await.result(lifter.liftRequest(req))
        val resultFieldMap = msg.result.asInstanceOf[FieldMap[String]]

        msg.endpoint.get should equal ("endpoint")
        resultFieldMap.get("request").get should equal (req.toString)
      }

      it("lift simple Post request") {
        val lifter = new HttpLifter(false)
        val requestBody = "request_body"
        val req = request(Method.Post, reqUri, Some(requestBody))
        req.headerMap.add("Canonical-Resource", "endpoint")

        val msg = Await.result(lifter.liftRequest(req))
        val resultFieldMap = msg.result.asInstanceOf[FieldMap[String]]

        msg.endpoint.get should equal ("endpoint")
        resultFieldMap.get("request").get should equal (req.toString)
        resultFieldMap.get("body").get should equal (requestBody)
      }
    }

    describe("LiftResponse") {

      it("exclude header in response map if excludeHttpHeadersComparison flag is off") {
        val lifter = new HttpLifter(true)
        val resp = response(HttpResponseStatus.OK, validJsonBody)

        val msg = Await.result(lifter.liftResponse(Try(resp)))
        val resultFieldMap = msg.result

        resultFieldMap.get("headers") should be (None)
      }


      it("return None as controller endpoint when action header was not set") {
        val lifter = new HttpLifter(false)
        val resp = response(HttpResponseStatus.OK, validJsonBody)
        resp.headerMap.remove(HttpLifter.ControllerEndpointHeaderName)
        val msg = Await.result(lifter.liftResponse(Try(resp)))

        msg.endpoint should equal (None)
      }

      it("propagate exception if response try failed") {
        val lifter = new HttpLifter(false)
        val thrown = the [Exception] thrownBy {
          Await.result(lifter.liftResponse(Throw(testException)))
        }

        thrown should be (testException)
      }
    }
  }
}
