package ai.diffy

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters.mapAsJavaMapConverter

@Controller
class Frontend (@Autowired settings: Settings) {

  case class Dashboard(
    serviceName: String,
    serviceClass: String,
    apiRoot: String,
    excludeNoise: Boolean,
    relativeThreshold: Double,
    absoluteThreshold: Double)

  @GetMapping(path = Array("/"))
  def root(): ModelAndView = {
    new ModelAndView(
      "dashboard",
      Map(
        "serviceName" -> settings.serviceName,
        "apiRoot" -> settings.apiRoot,
        "excludeNoise" -> false,
        "relativeThreshold" -> settings.relativeThreshold,
        "absoluteThreshold" -> settings.absoluteThreshold
      ).asJava
    )
  }
}