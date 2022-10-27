package ai.diffy.proxy;

import ai.diffy.analysis.DifferenceResult;
import ai.diffy.lifter.AnalysisRequest;
import ai.diffy.functional.topology.TriConsumer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import scala.Option;

public class AnalysisSpanLogger implements TriConsumer<AnalysisRequest, Option<DifferenceResult>, Span> {
    private AnalysisSpanLogger(){};
    public static AnalysisSpanLogger INSTANCE = new AnalysisSpanLogger();
    @Override
    public void accept(AnalysisRequest analysisRequest, Option<DifferenceResult> result, Span span) {
        result.foreach(diffResult ->
                span.addEvent("DifferenceResult", Attributes.of(
                        AttributeKey.stringKey("endpoint"), diffResult.endpoint,
                        AttributeKey.stringKey("request"), diffResult.request,
                        AttributeKey.stringKey("candidate"), diffResult.responses.candidate,
                        AttributeKey.stringKey("primary"), diffResult.responses.primary,
                        AttributeKey.stringKey("secondary"), diffResult.responses.secondary
                ))
        );
    }
}
