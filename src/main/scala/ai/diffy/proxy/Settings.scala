package ai.diffy.proxy

import java.net.InetSocketAddress

import ai.diffy.util.ResourceMatcher
import ai.diffy.util.ServiceInstance
import com.twitter.util.{Duration, StorageUnit, Try}

case class Settings(
    datacenter: String,
    servicePort:InetSocketAddress,
    candidate: String,
    primary: String,
    secondary: String,
    protocol: String,
    clientId: String,
    pathToThriftJar: String,
    serviceClass: String,
    serviceName: String,
    apiRoot: String,
    enableThriftMux: Boolean,
    relativeThreshold: Double,
    absoluteThreshold: Double,
    teamEmail: String,
    emailDelay: Duration,
    rootUrl: String,
    allowHttpSideEffects: Boolean,
    excludeHttpHeadersComparison: Boolean,
    skipEmailsWhenNoErrors: Boolean,
    httpsPort: String,
    useFramedThriftTransport: Boolean,
    hostname: String = Try(java.lang.management.ManagementFactory.getRuntimeMXBean.getName.split("@")(1)).getOrElse("unknown"),
    user: String = Try(sys.env("USER")).getOrElse("unknown"),
    resourceMatcher: Option[ResourceMatcher] = None,
    responseMode: ServiceInstance = ServiceInstance.Primary,
    maxResponseSize: StorageUnit,
    maxHeaderSize: StorageUnit)