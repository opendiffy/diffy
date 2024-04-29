package ai.diffy.repository;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
public class Noise {
	
    @Id 
    @Column()
    public String endpoint;
  
    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "noisyfields", joinColumns = @JoinColumn(name = "endpoint"))
    @Column(name = "noisyfields", nullable = false)
    public List<String> noisyfields;

    public Noise(String endpoint, List<String> noisyfields) {
        this.endpoint = endpoint;
        this.noisyfields = noisyfields;
    }
}
