package ai.diffy.transformations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class TransformationController {
    @Autowired
    TransformationCachingService service;

    @PostMapping("/api/1/transformations/{injectionPoint}")
    public void set(@PathVariable("injectionPoint") String injectionPoint, @RequestBody String transformationJs) {
        service.set(new Transformation(injectionPoint, transformationJs));
    }

    @GetMapping("/api/1/transformations/{injectionPoint}")
    public Transformation get(@PathVariable("injectionPoint") String injectionPoint) {
        return service
                .get(injectionPoint)
                .map(txJs -> new Transformation(injectionPoint, txJs))
                .orElse(new Transformation(injectionPoint, "(request) => (request)"));
    }

    @DeleteMapping("/api/1/transformations/{injectionPoint}")
    public void delete(@PathVariable("injectionPoint") String injectionPoint) {
        service.delete(injectionPoint);
    }
}
