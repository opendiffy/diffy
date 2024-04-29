package ai.diffy.transformations;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "Transformation")
public class Transformation {
    
	@Id
    @Column(name = "injection_Point")
    public String injectionPoint;
	
    @Column(name = "transformation_Js")
    public String transformationJs;

    public Transformation(String injectionPoint, String transformationJs) {
        this.injectionPoint = injectionPoint;
        this.transformationJs = transformationJs;
    }

    public String getTransformationJs() {
        return transformationJs;
    }
}
