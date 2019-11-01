package ai.diffy.util

import ai.diffy.util.ResourceMatcher.ResourceMapping

import scala.util.matching.Regex

object ResourceMatcher {

  /**
   * _1 - The pattern for the resource
   * _2 - The name of the resource
   * _3 - A function that returns true if a string matches the pattern
   */
  type ResourceMapping = (String, String)

  type PathMatcher = (String, String) => Boolean
}

class ResourceMatcher private(tokenPattern: Regex, wildcardPattern: Regex, mappings: List[ResourceMapping]) {

  def this(mappings: List[ResourceMapping]) = this(""":\w+""".r, """\*+""".r, mappings)

  private val patterns = mappings
    .map { case (pattern, name) => (pattern
      .replaceAll(tokenPattern.regex, "\\\\w+")
      .replaceAll(wildcardPattern.regex, ".*"), name)
    }

  def resourceName(path: String): Option[String] = {
    patterns.find { case (regex, _) => path.matches(regex) }.map(_._2)
  }
}
