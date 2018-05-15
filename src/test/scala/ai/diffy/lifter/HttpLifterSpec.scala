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
      it("lift simple Json response") {
        checkJsonContentTypeIsLifted(MediaType.JSON_UTF_8.toString)
      }

      it("lift simple Json response when the charset is not set") {
        checkJsonContentTypeIsLifted(MediaType.JSON_UTF_8.withoutParameters().toString)
      }

      it("exclude header in response map if excludeHttpHeadersComparison flag is off") {
        val lifter = new HttpLifter(true)
        val resp = response(HttpResponseStatus.OK, validJsonBody)

        val msg = Await.result(lifter.liftResponse(Try(resp)))
        val resultFieldMap = msg.result

        resultFieldMap.get("headers") should be (None)
      }

      it("throw MalformedJsonContentException when json body is malformed") {
        val lifter = new HttpLifter(false)
        val resp = response(HttpResponseStatus.OK, invalidJsonBody)
        val thrown = the [MalformedJsonContentException] thrownBy {
          Await.result(lifter.liftResponse(Try(resp)))
        }

        thrown.getCause should not be (null)
      }

      it("only compare headers when ContentType header was not set") {
        val lifter = new HttpLifter(false)
        val resp = response(HttpResponseStatus.OK, validJsonBody)
        resp.headerMap.remove(HttpHeaders.Names.CONTENT_TYPE)

        val msg = Await.result(lifter.liftResponse(Try(resp)))
        val resultFieldMap = msg.result

        resultFieldMap.get("headers") should not be (None)
      }

      it("returns FieldMap when ContentType header is Html") {
        val lifter = new HttpLifter(false)
        val resp = response(HttpResponseStatus.OK, validHtmlBody)
        resp.headerMap.set(HttpHeaders.Names.CONTENT_TYPE, MediaType.HTML_UTF_8.toString)

        val msg = Await.result(lifter.liftResponse(Try(resp)))
        val resultFieldMap = msg.result

        resultFieldMap shouldBe a [FieldMap[_]]
      }

      it("throw ContentTypeNotSupportedException when ContentType header is not Json or Html") {
        val lifter = new HttpLifter(false)
        val resp = response(HttpResponseStatus.OK, validJsonBody)
        resp.headerMap.update(HttpHeaders.Names.CONTENT_TYPE, textContentType)

        val thrown = the [Exception] thrownBy {
          Await.result(lifter.liftResponse(Try(resp)))
        }

        thrown.getMessage should be (HttpLifter.contentTypeNotSupportedException(MediaType.PLAIN_TEXT_UTF_8.toString).getMessage)
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

      it("only compares header when Content-Length is zero") {
        val lifter = new HttpLifter(false)
        val resp = response(HttpResponseStatus.OK, "")
        resp.headerMap.set(HttpHeaders.Names.CONTENT_TYPE, MediaType.GIF.toString)

        val msg = Await.result(lifter.liftResponse(Try(resp)))
        val resultFieldMap = msg.result
        resultFieldMap.get("headers") should not be (None)
      }

      def checkJsonContentTypeIsLifted(contentType: String): Unit = {
        val lifter = new HttpLifter(false)
        val resp = response(HttpResponseStatus.OK, validJsonBody)
        resp.headerMap.set(HttpHeaders.Names.CONTENT_TYPE, contentType)

        val msg = Await.result(lifter.liftResponse(Try(resp)))
        val resultFieldMap = msg.result.asInstanceOf[FieldMap[Map[String, Any]]]
        val status = resultFieldMap.keySet.headOption.value
        val headers = resultFieldMap.get(status).value
          .get("headers").value.asInstanceOf[FieldMap[Any]]
        val content = resultFieldMap.get(status).value.get("content").value.asInstanceOf[JsonNode]

        msg.endpoint.get should equal(controllerEndpoint)
        status should equal(HttpResponseStatus.OK.getCode.toString)
        headers.get(HttpLifter.ControllerEndpointHeaderName).get should equal(
          ArrayBuffer(controllerEndpoint))
        headers.get(HttpHeaders.Names.CONTENT_TYPE).get should equal(
          ArrayBuffer(contentType))
        headers.get(HttpHeaders.Names.CONTENT_LENGTH).get should equal(
          ArrayBuffer(validJsonBody.length.toString))
        content.get("data_type").asText should equal("account")
        content.get("total_count").asInt should equal(2)
        content.get("next_cursor").isNull should be(true)
        val data = content.get("data")
        data should have size (2)
        data.get(0).get("name").asText should equal("Account 1")
        data.get(0).get("deleted").asBoolean should equal(false)
        data.get(1).get("name").asText should equal("Account 2")
        data.get(1).get("deleted").asBoolean should equal(true)
      }
    }
  }
}
