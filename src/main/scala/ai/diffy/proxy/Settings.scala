package ai.diffy.proxy

import java.net.InetSocketAddress

import ai.diffy.util.ResourceMatcher
import com.twitter.util.{Duration, Try}

case class Settings(
                     datacenter: String,
                     servicePort:InetSocketAddress,
                     candidate: String,
                     primary: String,
                     secondary: String,
                     candidateApiRoot: String,
                     primaryApiRoot: String,
                     secondaryApiRoot: String,
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
                     resourceMatcher: Option[ResourceMatcher] = None)
