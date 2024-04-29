package ai.diffy.analysis;

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
@Table(name = "Responses")
public class Responses {
	
	@Id
    @Column(name = "id")
    public String id;
	
	@Column(name = "'primary'")
    public String primary;
	
	@Column(name = "'secondary'")
    public String secondary;
	
	@Column(name = "'candidate'")
    public String candidate;

    public Responses(String primary, String secondary, String candidate) {
        this.primary = primary;
        this.secondary = secondary;
        this.candidate = candidate; 
    }
}
