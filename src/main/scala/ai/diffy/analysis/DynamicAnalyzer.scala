package ai.diffy.analysis

import ai.diffy.repository.DifferenceResultRepository

/**
 * Filters a DifferenceAnalyzer using a specified time range to output another DifferenceAnalyzer
 */
class DynamicAnalyzer(repository: DifferenceResultRepository) {
  def filter(start: Long, end: Long): DifferenceAnalyzer = {
    null
  }
}
