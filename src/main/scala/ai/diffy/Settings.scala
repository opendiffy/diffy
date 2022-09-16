package ai.diffy

import ai.diffy.util.{ResourceMatcher, ServiceInstance}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Settings(
     @Value("${proxy.port}") val servicePort: Int,
     @Value("${candidate}") candidate: String,
     @Value("${master.primary}") primary: String,
     @Value("${master.secondary}") secondary: String,
     @Value("${service.protocol}") val protocol: String,
     @Value("${serviceName}") val serviceName: String,
     @Value("${apiRoot:}") val apiRoot: String = "",
     @Value("${threshold.relative:20.0}") val relativeThreshold: Double = 20.0,
     @Value("${threshold.absolute:0.03}") val absoluteThreshold: Double = 0.03,
     @Value("${rootUrl:}") val rootUrl: String = "",
     @Value("${allowHttpSideEffects:false}") val allowHttpSideEffects: Boolean = false,
     @Value("${excludeHttpHeadersComparison:false}") val excludeHttpHeadersComparison: Boolean = false,
     @Value("${resource.mapping:}") resourceMappings: String = "",
     @Value("${responseMode:primary}") mode: String = ServiceInstance.Primary.name)
{
  private[this] val log = LoggerFactory.getLogger(classOf[Settings])
  val candidateHost: String = candidate.split(":")(0)
  val candidatePort: Int = candidate.split(":")(1).toInt
  val primaryHost: String = primary.split(":")(0)
  val primaryPort: Int = primary.split(":")(1).toInt
  val secondaryHost: String = secondary.split(":")(0)
  val secondaryPort: Int = secondary.split(":")(1).toInt
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

  val responseMode = ServiceInstance.from(mode).getOrElse(ServiceInstance.Primary)
}