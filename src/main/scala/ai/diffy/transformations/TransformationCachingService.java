package ai.diffy.transformations;

import ai.diffy.functional.endpoints.Endpoint;
import ai.diffy.interpreter.Transformer;
import ai.diffy.proxy.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Service
public class TransformationCachingService {
    @Autowired
    TransformationRepository repository;

    private final ConcurrentHashMap<TransformationEdge, String> transformations = new ConcurrentHashMap<>(TransformationEdge.values().length);
    private final ConcurrentHashMap<TransformationEdge, Transformer<HttpRequest>> rxTx = new ConcurrentHashMap<>(TransformationEdge.values().length);

    @PostConstruct
    public void postConstruct() {
        Arrays.stream(TransformationEdge.values()).flatMap(edge ->
                repository.findById(edge.name()).stream()
        ).forEach(this::putCacheOnly);
    }
    private void putCacheOnly(Transformation tx){
        TransformationEdge edge = TransformationEdge.valueOf(tx.injectionPoint);
        transformations.put(edge, tx.transformationJs);
        rxTx.put(edge, new Transformer<>(HttpRequest.class, tx.transformationJs));
    }

    // Read back
    public Optional<String> get(String injectionPoint){
        return Optional.ofNullable(transformations.get(TransformationEdge.valueOf(injectionPoint)));
    }
    public Optional<Transformer<HttpRequest>> get(TransformationEdge edge){
        return Optional.ofNullable(rxTx.get(edge));
    }

    // Write through
    public void set(Transformation transformation) {
        this.putCacheOnly(transformation);
        repository.save(transformation);
    }

    public void delete(String injectionPoint){
        transformations.remove(TransformationEdge.valueOf(injectionPoint));
        rxTx.remove(TransformationEdge.valueOf(injectionPoint));
        repository.deleteById(injectionPoint);
    }

    // Application for syntactic sugar
    public <Response> Endpoint<HttpRequest,Response> apply(TransformationEdge edge, Endpoint<HttpRequest,Response> endpoint) {
        Optional<Transformer<HttpRequest>> tx = get(edge);
        if(tx.isEmpty()){
            return endpoint;
        }
        return endpoint.andThenMiddleware((applier) -> applier.compose(tx.get().suppressThrowable()));
    }
}
