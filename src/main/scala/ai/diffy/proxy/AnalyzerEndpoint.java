package ai.diffy.proxy;

import ai.diffy.analysis.DifferenceAnalyzer;
import ai.diffy.analysis.DifferenceResult;
import ai.diffy.lifter.AnalysisRequest;
import ai.diffy.functional.endpoints.IndependentEndpoint;
import scala.Option;

public class AnalyzerEndpoint extends IndependentEndpoint<AnalysisRequest, Option<DifferenceResult>> {
    protected AnalyzerEndpoint(DifferenceAnalyzer analyzer) {
        super("Analyzer", () -> (AnalysisRequest req) ->
            analyzer.apply(req.request(),req.primary(),req.secondary(),req.candidate())
        );
    }
}
