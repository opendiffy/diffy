package ai.diffy.lifter

import com.fasterxml.jackson.core.{JsonGenerator, JsonToken}
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{JsonNode, JsonSerializer, ObjectMapper, SerializerProvider}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.control.NoStackTrace

object JsonLifter {
  val log  = LoggerFactory.getLogger(JsonLifter.getClass)

  @JsonSerialize(using = classOf[JsonNullSerializer])
  object JsonNull
  object JsonParseError extends Exception with NoStackTrace

  val Mapper = new ObjectMapper
  Mapper.registerModule(DefaultScalaModule)

  class fieldMapSerializer extends JsonSerializer[FieldMap]() {
    override def serialize(t: FieldMap, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
      jsonGenerator.writeObject(t.value)
    }
  }
  val module = new SimpleModule()
  module.addSerializer(classOf[FieldMap], new fieldMapSerializer)
  Mapper.registerModule(module)

  def apply(obj: Any): JsonNode = Mapper.valueToTree(obj)

  def lift(node: JsonNode): Any = node.asToken match {
    case JsonToken.START_ARRAY        =>
      node.elements.asScala.toSeq.map {
        element => lift(element)
      }
    case JsonToken.START_OBJECT       => {
      val fields = node.fieldNames.asScala.toSet
      if (areMapInsteadofObjectKeys(fields)) {
        node.fields.asScala map {field => (field.getKey -> lift(field.getValue))} toMap
      } else {
        new FieldMap(
          node.fields.asScala map {field => (field.getKey -> lift(field.getValue))} toMap
        )
      }
    }
    case JsonToken.VALUE_FALSE        => false
    case JsonToken.VALUE_NULL         => JsonNull
    case JsonToken.VALUE_NUMBER_FLOAT => node.asDouble
    case JsonToken.VALUE_NUMBER_INT   => node.asLong
    case JsonToken.VALUE_TRUE         => true
    case JsonToken.VALUE_STRING       => node.textValue
    case _                            => throw JsonParseError
  }

  def decode(json: String): JsonNode = Mapper.readTree(json)
  def decode[T](json: String, clss: Class[T]) = Mapper.readValue(json, clss)
  def encode(item: Any): String = Mapper.writer.writeValueAsString(item)

  def areMapInsteadofObjectKeys(fields: Set[String]): Boolean =
    fields.size > 50 || fields.exists{ field =>
      field.length > 100 ||
      field.matches("[0-9].*") || // starts with a digit
      !field.matches("[_a-zA-Z0-9]*") // contains non-alphanumeric characters
    }

}

class JsonNullSerializer(clazz: Class[Any]) extends StdSerializer[Any](clazz) {
  def this() {
    this(null)
  }

  override def serialize(t: Any, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeNull()
  }
}