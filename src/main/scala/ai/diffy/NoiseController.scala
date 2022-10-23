package ai.diffy

import ai.diffy.repository.{Noise, NoiseRepository}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation._

import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava}

@RestController
class NoiseController(@Autowired noise: NoiseRepository) {

  def empty: java.util.List[String] = new java.util.ArrayList[String]()


  @GetMapping(path = Array("/api/1/noise"))
  def getAllNoise(): java.util.Map[String, java.util.List[String]] = {
    noise.findAll().asScala
      .map(endpointNoise => endpointNoise.endpoint -> endpointNoise.noisyfields)
      .toMap.asJava
  }

  @GetMapping(path = Array("/api/1/noise/{endpoint}"))
  def getNoise(@PathVariable("endpoint") endpoint: String): java.util.List[String] = {
    noise.findById(endpoint).map(_.noisyfields).orElse(empty)
  }

  @PostMapping(path = Array("/api/1/noise/{endpoint}/prefix/{fieldPrefix}"))
  def postNoise(
      @PathVariable("endpoint") endpoint: String,
      @PathVariable("fieldPrefix") fieldPrefix: String,
      @RequestBody mark: Mark): Boolean = {
    val noisyFields: java.util.List[String] =
      noise.findById(endpoint).map(_.noisyfields).orElse(empty)
    var success = false
    if(mark.isNoise){
      success = noisyFields.add(fieldPrefix)
    } else {
      success = noisyFields.remove(fieldPrefix)
    }
    noise.save(new Noise(endpoint, noisyFields))
    success
  }
}
case class Mark(isNoise: Boolean)


