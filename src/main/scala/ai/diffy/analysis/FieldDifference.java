package ai.diffy.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "Field_Difference")
public class FieldDifference {
	
	@Id
    @Column(name = "id")
    public String id;
	
	@Column(name = "field")
    public String field;
	
	@Column(name = "difference")
    public String difference;

    public FieldDifference(String field, String difference) {
        this.field = field;
        this.difference = difference;
    }
}
