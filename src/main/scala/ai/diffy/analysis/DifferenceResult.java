package ai.diffy.analysis;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "Difference_Result", indexes = @Index(name = "fnx_index", columnList = "timestamp_Msec"))
public class DifferenceResult {
    
	@Id
    @Column(name = "id")
    public String id;
    
    @Column(name = "trace_Id")
    public String traceId;
    
    @Column(name = "endpoint")
    public String endpoint;
    
    @Column(name = "timestamp_Msec")
    public Long timestampMsec;
    
    @Column(name = "request")
    public String request;
    
    @MapsId
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public Responses responses;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public List<FieldDifference> differences;
    
    public DifferenceResult(String id, String traceId, String endpoint, Long timestampMsec, List<FieldDifference> differences, String request, Responses responses) {
        this.id = id;
        this.traceId = traceId;
        this.endpoint = endpoint;
        this.timestampMsec = timestampMsec;
        this.differences = differences;
        this.request = request;
        this.responses = responses;
        
        responses.setId(id);
        
        if(null != differences) {
        	differences.stream().forEach(dif -> dif.setId(id));
        }
        
    }
}

