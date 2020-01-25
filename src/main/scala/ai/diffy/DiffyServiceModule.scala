package ai.diffy

import java.net.InetSocketAddress

import ai.diffy.analysis.{InMemoryDifferenceCollector, InMemoryDifferenceCounter, NoiseDifferenceCounter, RawDifferenceCounter}
import ai.diffy.proxy.Settings
import ai.diffy.util.ResourceMatcher
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import com.twitter.util.Duration
import javax.inject.Singleton

object DiffyServiceModule extends TwitterModule {
  val datacenter =
    flag("dc", "localhost", "the datacenter where this Diffy instance is deployed")

  val servicePort =
    flag("proxy.port", new InetSocketAddress(9992), "The port where the proxy service should listen")

  val candidatePath =
    flag[String]("candidate", "candidate serverset where code that needs testing is deployed")

  val primaryPath =
    flag[String]("master.primary", "primary master serverset where known good code is deployed")

  val secondaryPath =
    flag[String]("master.secondary", "secondary master serverset where known good code is deployed")

  val candidateApiRoot =
    flag[String]("candidateApiRoot", "", "Api Root for the candidate api to call. E.g:- api/v1")

  val primaryApiRoot =
    flag[String]("primaryApiRoot", "", "Api Root for the primary api to call. E.g:- api/v2")

  val secondaryApiRoot =
    flag[String]("secondaryApiRoot", "", "Api Root for the secondary api to call. E.g:- api/v3")

  val protocol =
    flag[String]("service.protocol", "Service protocol: thrift, http or https")

  val clientId =
    flag[String]("proxy.clientId", "diffy.proxy", "The clientId to be used by the proxy service to talk to candidate, primary, and master")

  val pathToThriftJar =
    flag[String]("thrift.jar", "path/to/thrift.jar", "The path to a fat Thrift jar - the jar should include all dependencies")

  val serviceClass =
    flag[String]("thrift.serviceClass", "unknown", "The service name within the thrift jar e.g. UserService")

  val serviceName =
    flag[String]("serviceName", "The service title e.g. UserService or LocationService")

  val apiRoot =
    flag[String]("apiRoot", "", "A path token that will be removed by a proxy gateway before forwarding UI requests to Diffy")

  val enableThriftMux =
    flag[Boolean]("enableThriftMux", true, "use thrift mux server and clients")

  val relativeThreshold =
    flag[Double]("threshold.relative", 20.0, "minimum (inclusive) relative threshold that a field must have to be returned")

  val absoluteThreshold =
    flag[Double]("threshold.absolute", 0.03, "minimum (inclusive) absolute threshold that a field must have to be returned")

  val teamEmail =
    flag[String]("summary.email", "team email to which cron report should be sent")

  val emailDelay =
    flag[Int]("summary.delay", 5, "minutes to wait before sending report out. e.g. 5")

  val rootUrl =
    flag[String]("rootUrl", "", "Root url to access this service, e.g. diffy-staging-gizmoduck.service.smf1.twitter.com")

  val allowHttpSideEffects =
    flag[Boolean]("allowHttpSideEffects", false, "Ignore POST, PUT, and DELETE requests if set to false")

  val excludeHttpHeadersComparison =
    flag[Boolean]("excludeHttpHeadersComparison", false, "Exclude comparison on HTTP headers if set to false")

  val skipEmailsWhenNoErrors =
    flag[Boolean]("skipEmailsWhenNoErrors", false, "Do not send emails if there are no critical errors")

  val httpsPort =
    flag[String]("httpsPort", "443", "Port to be used when using HTTPS as a protocol")

  val thriftFramedTransport =
    flag[Boolean]("thriftFramedTransport", true, "Run in BufferedTransport mode when false")

  val resourceMappings =
    flag[String]("resource.mapping", "", "Coma separated list of resource paths and names. Each resource is separated by a colon. Example '/foo:foo-resource,/bar:bar-resource")

  @Provides
  @Singleton
  def settings =
    Settings(
      datacenter(),
      servicePort(),
      candidatePath(),
      primaryPath(),
      secondaryPath(),
      candidateApiRoot(),
      primaryApiRoot(),
      secondaryApiRoot(),
      protocol(),
      clientId(),
      pathToThriftJar(),
      serviceClass(),
      serviceName(),
      apiRoot(),
      enableThriftMux(),
      relativeThreshold(),
      absoluteThreshold(),
      teamEmail(),
      Duration.fromMinutes(emailDelay()),
      rootUrl(),
      allowHttpSideEffects(),
      excludeHttpHeadersComparison(),
      skipEmailsWhenNoErrors(),
      httpsPort(),
      thriftFramedTransport(),
      resourceMatcher = Option(resourceMappings()).map(_
        .split(",")
        .map(_.split(";"))
        .filter { x =>
          val wellFormed = x.length == 2
          if (!wellFormed) logger.warn(s"Malformed resource mapping: $x. Should be <pattern>;<resource-name>")
          wellFormed
        }
        .map(x => (x(0), x(1)))
        .toList)
        .map(new ResourceMatcher(_))
    )

  @Provides
  @Singleton
  def providesRawCounter = RawDifferenceCounter(new InMemoryDifferenceCounter)

  @Provides
  @Singleton
  def providesNoiseCounter = NoiseDifferenceCounter(new InMemoryDifferenceCounter)

  @Provides
  @Singleton
  def providesCollector = new InMemoryDifferenceCollector
}
