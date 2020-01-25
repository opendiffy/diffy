package ai.diffy

import java.net.InetSocketAddress

import ai.diffy.analysis._
import ai.diffy.compare.Difference
import ai.diffy.proxy._
import com.twitter.util.Duration
import org.scalatest.mock.MockitoSugar

object TestHelper extends MockitoSugar {
  lazy val testSettings = Settings(
    datacenter = "test",
    servicePort = new InetSocketAddress(9999),
    candidate = "candidate",
    primary = "primary",
    secondary = "secondary",
    candidateApiRoot = "api/v2",
    primaryApiRoot = "api/v1",
    secondaryApiRoot = "api/v1",
    protocol = "test",
    clientId = "test",
    pathToThriftJar = "test",
    serviceClass = "test",
    serviceName = "test",
    apiRoot = "test",
    enableThriftMux = false,
    relativeThreshold = 0.0,
    absoluteThreshold = 0.0,
    teamEmail = "test",
    emailDelay = Duration.fromSeconds(0),
    rootUrl = "test",
    allowHttpSideEffects = true,
    excludeHttpHeadersComparison = true,
    skipEmailsWhenNoErrors = false,
    httpsPort = "443",
    useFramedThriftTransport = false
  )

  def makeEmptyJoinedDifferences = {
    val rawCounter = RawDifferenceCounter(new InMemoryDifferenceCounter())
    val noiseCounter = NoiseDifferenceCounter(new InMemoryDifferenceCounter())
    JoinedDifferences(rawCounter, noiseCounter)
  }

  def makePopulatedJoinedDifferences(endpoint : String, diffs : Map[String, Difference]) = {
    val rawCounter = RawDifferenceCounter(new InMemoryDifferenceCounter())
    val noiseCounter = NoiseDifferenceCounter(new InMemoryDifferenceCounter())
    val data = new InMemoryEndpointMetadata()
    data.add(diffs)
    rawCounter.counter.asInstanceOf[InMemoryDifferenceCounter].endpointsMap += (endpoint -> data)

    JoinedDifferences(rawCounter, noiseCounter)
  }
}