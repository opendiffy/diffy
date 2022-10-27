package ai.diffy.lifter;

case class AnalysisRequest(
  request: Message,
  candidate: Message,
  primary: Message,
  secondary: Message)
