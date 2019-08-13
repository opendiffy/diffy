package ai.diffy.proxy

import java.net.InetSocketAddress

import com.twitter.util.Duration

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
  hostname: String = java.net.InetAddress.getLocalHost.toString,
  user: String = sys.env("USER"))