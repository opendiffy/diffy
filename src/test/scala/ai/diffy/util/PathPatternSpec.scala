package ai.diffy.util

import ai.diffy.ParentSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PathPatternSpec extends ParentSpec {

  describe("The HTTP PathMatcher") {

    val matcher = PathPattern.http

    it("should support parameter placeholders") {
      val matches = matcher("/path1/param1/path2/param2", "/path1/:param1/path2/:param2")
      matches mustBe true
    }

    it("should support wildcards, matching everything after the wildcard") {
      val matches = matcher("/path1/param1/path/param2", "/path1/*")
      matches mustBe true
    }

    it("should support wildcards, matching wildcards in the middle of the path") {
      val matches = matcher("/path1/param1/path/param2", "/path1/*/path/param2")
      matches mustBe true
    }
  }
}
