package ai.diffy.util

import scala.util.matching.Regex

object PathPattern {

  type PathMatcher = (String, String) => Boolean

  val http: PathMatcher = matches(""":\w+""".r, """\*+""".r)

  private def matches(tokenPattern: Regex, wildcardPattern: Regex)(
      path: String,
      pattern: String): Boolean = {
    path.matches(
      pattern
        .replaceAll(tokenPattern.regex, "\\\\w+")
        .replaceAll(wildcardPattern.regex, ".*"))
  }
}
