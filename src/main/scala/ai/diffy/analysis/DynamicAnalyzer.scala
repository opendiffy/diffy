package ai.diffy.analysis

import ai.diffy.analysis.DynamicAnalyzer.decodeFieldMap
import ai.diffy.lifter.{FieldMap, JsonLifter, Message}
import ai.diffy.repository.DifferenceResultRepository

/**
 * Filters a DifferenceAnalyzer using a specified time range to output another DifferenceAnalyzer
 */
object DynamicAnalyzer {
  def decodeFieldMap(payload: String): FieldMap = {
    JsonLifter.decode(s"{\"value\":$payload}", classOf[FieldMap])
  }
}
class DynamicAnalyzer(repository: DifferenceResultRepository) {
  def filter(start: Long, end: Long): Report = {
    val collector = new InMemoryDifferenceCollector
    val raw = RawDifferenceCounter(new InMemoryDifferenceCounter("raw"))
    val noise = NoiseDifferenceCounter(new InMemoryDifferenceCounter("noise"))
    val joinedDifferences = JoinedDifferences(raw, noise)
    val analyzer = new DifferenceAnalyzer(raw, noise, collector)

    val diffs = repository.findByTimestampMsecBetween(start, end)
    diffs.forEach(dr => {
      val request = Message(Some(dr.endpoint), decodeFieldMap(dr.request))
      val primary = Message(Some(dr.endpoint), decodeFieldMap(dr.responses.primary))
      val secondary = Message(Some(dr.endpoint), decodeFieldMap(dr.responses.secondary))
      val candidate = Message(Some(dr.endpoint), decodeFieldMap(dr.responses.candidate))
      analyzer.apply(request, candidate, primary, secondary, Some(dr.id))
    })
    Report(analyzer, joinedDifferences, collector, start, end)
  }
}
