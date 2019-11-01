package ai.diffy.util

import ai.diffy.ParentSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ResourceMatcherSpec extends ParentSpec {

  describe("The HTTP PathMatcher") {

    it("should support parameter placeholders") {
      new ResourceMatcher(List("/path1/:param1/path2/:param2" -> "p1"))
        .resourceName("/path1/param1/path2/param2") mustBe Some("p1")
    }

    it("should support wildcards, matching everything after the wildcard") {
      new ResourceMatcher(List("/path1/*" -> "p2"))
        .resourceName("/path1/*") mustBe Some("p2")
    }

    it("should support wildcards, matching wildcards in the middle of the path") {
      new ResourceMatcher(List("/path1/*/path/param2" -> "p3"))
        .resourceName("/path1/param1/path/param2") mustBe Some("p3")
    }
  }
}
