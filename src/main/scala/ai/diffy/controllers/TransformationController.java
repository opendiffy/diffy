package ai.diffy.controllers;

import ai.diffy.repository.transformations.Transformation;
import ai.diffy.repository.transformations.TransformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class TransformationController {
    @Autowired
    TransformationRepository repository;

    @PostMapping("/api/1/transformations/{injectionPoint}")
    public void set(@PathVariable("injectionPoint") String injectionPoint, @RequestBody String transformationJs) {
        repository.save(new Transformation(injectionPoint, transformationJs));
    }

    @GetMapping("/api/1/transformations/{injectionPoint}")
    public Transformation get(@PathVariable("injectionPoint") String injectionPoint) {
        return repository
                .findById(injectionPoint)
                .orElse(new Transformation(injectionPoint, "(request) => (request)"));
    }
}
