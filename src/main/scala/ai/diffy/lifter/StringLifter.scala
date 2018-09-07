package ai.diffy.lifter

import com.twitter.util.Try

object StringLifter {
  val htmlRegexPattern = """<("[^"]*"|'[^']*'|[^'">])*>""".r

  def lift(string: String): Any = {
    Try(FieldMap(Map("type" -> "json", "value" -> JsonLifter.lift(JsonLifter.decode(string))))).getOrElse {
      if(htmlRegexPattern.findFirstIn(string).isDefined)
        FieldMap(Map("type" -> "html", "value" -> HtmlLifter.lift(HtmlLifter.decode(string))))
      else string
    }
  }
}
