package ai.diffy

import java.util.concurrent.atomic.AtomicInteger

import ai.diffy.IsotopeSdkModule.IsotopeClient
import ai.diffy.proxy.Settings
import com.twitter.finagle.Http
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import javax.inject.Inject

class Frontend @Inject()(settings: Settings, isotopeClient: IsotopeClient) extends Controller {

  case class Dashboard(
    serviceName: String,
    serviceClass: String,
    apiRoot: String,
    excludeNoise: Boolean,
    relativeThreshold: Double,
    absoluteThreshold: Double,
    isotopeReason: String,
    hasIsotope: Boolean = false)

  val reasons = Seq(
    "Do you want to compare side effects like logs and downstream interactions?",
    "Do you want to save and share this comparison?",
    "Do you want to organize all your comparisons across all your services and teams in one place?",
    "Do you want to download sampled traffic?"
  )
  val reasonIndex = new AtomicInteger(0)
  get("/") { req: Request =>
    response.ok.view(
      "dashboard.mustache",
      Dashboard(
        settings.serviceName,
        settings.serviceClass,
        settings.apiRoot,
        req.params.getBooleanOrElse("exclude_noise", false),
        settings.relativeThreshold,
        settings.absoluteThreshold,
        reasons(reasonIndex.getAndIncrement() % reasons.length),
        isotopeClient.isConcrete
      )
    )
  }

  get("/css/:*") { request: Request =>
    response.ok.file(request.path)
  }

  get("/scripts/:*") { request: Request =>
    response.ok.file(request.path)
  }
}