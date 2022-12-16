package ai.diffy

import ai.diffy.functional.functions.Try
import ai.diffy.util.{ResourceMatcher, ResponseMode}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import java.net.URL

@Component
class Settings(
                @Value("${proxy.port}") val servicePort: Int,
                @Value("${candidate}") candidateAddress: String,
                @Value("${master.primary}") primaryAddress: String,
                @Value("${master.secondary}") secondaryAddress: String,
                @Value("${service.protocol}") val protocol: String,
                @Value("${serviceName}") val serviceName: String,
                @Value("${apiRoot:}") val apiRoot: String = "",
                @Value("${threshold.relative:20.0}") val relativeThreshold: Double = 20.0,
                @Value("${threshold.absolute:0.03}") val absoluteThreshold: Double = 0.03,
                @Value("${rootUrl:}") val rootUrl: String = "",
                @Value("${allowHttpSideEffects:false}") val allowHttpSideEffects: Boolean = false,
                @Value("${excludeHttpHeadersComparison:false}") val excludeHttpHeadersComparison: Boolean = false,
                @Value("${resource.mapping:}") resourceMappings: String = "",
                @Value("${responseMode:primary}") mode: String = ResponseMode.primary.name(),
                @Value("${dockerComposeLocal:false}") val dockerComposeLocal: Boolean = false)
{
  private[this] val log = LoggerFactory.getLogger(classOf[Settings])
  val candidate = Downstream(candidateAddress)
  val primary = Downstream(primaryAddress)
  val secondary = Downstream(secondaryAddress)
  val resourceMatcher: Option[ResourceMatcher] = Option(resourceMappings).map(
    _.split(",")
      .map(_.split(";"))
      .filter { x =>
        val wellFormed = x.length == 2
        if (!wellFormed) log.warn(s"Malformed resource mapping: $x. Should be <pattern>;<resource-name>")
        wellFormed
      }
      .map(x => (x(0), x(1)))
      .toList)
    .map(new ResourceMatcher(_))

  val responseMode = ResponseMode.valueOf(mode);
}

object Downstream {
  def apply(address: String): Downstream = {
    if (Try.of(() => new URL(address)).isNormal)
      BaseUrl(address)
    else
      HostPort(address.split(":")(0), address.split(":")(1).toInt)
  }
}
sealed trait Downstream
case class HostPort(host: String, port: Int) extends Downstream{
  override def toString: String = s"${host}:${port}"
}
case class BaseUrl(baseUrl: String) extends Downstream {
  override def toString: String = baseUrl
}