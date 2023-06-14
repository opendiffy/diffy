package ai.diffy.analysis

case class Report(
  differenceAnalyzer: DifferenceAnalyzer,
  joinedDifferences: JoinedDifferences,
  collector: InMemoryDifferenceCollector,
  start: Long,
  end: Long)
