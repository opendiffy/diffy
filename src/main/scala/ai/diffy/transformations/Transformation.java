package ai.diffy.transformations;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Transformation {
    @Id
    public String injectionPoint;
    public String transformationJs;

    public Transformation(String injectionPoint, String transformationJs) {
        this.injectionPoint = injectionPoint;
        this.transformationJs = transformationJs;
    }

    public String getTransformationJs() {
        return transformationJs;
    }
}
