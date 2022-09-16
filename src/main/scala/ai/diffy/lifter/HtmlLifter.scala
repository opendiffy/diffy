package ai.diffy.lifter

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import scala.collection.JavaConverters._
import scala.language.postfixOps

object HtmlLifter {
  def lift(node: Element): FieldMap = node match {
    case doc: Document =>
      new FieldMap(
        Map(
          "head" -> lift(doc.head),
          "body" -> lift(doc.body)
        )
      )
    case doc: Element => {
      val children: Elements = doc.children
      val attributes =
        new FieldMap(
          doc.attributes.asList.asScala map { attribute =>
            attribute.getKey -> attribute.getValue
          } toMap
        )

      new FieldMap(
        Map(
          "tag"         -> doc.tagName,
          "text"        -> doc.ownText,
          "attributes"  -> attributes,
          "children"    -> children.asScala.map(element => lift(element))
        )
      )
    }
  }

  def decode(html: String): Document = Jsoup.parse(html)
}