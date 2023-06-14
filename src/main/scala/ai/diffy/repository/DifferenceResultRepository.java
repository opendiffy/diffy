package ai.diffy.repository;

import ai.diffy.analysis.DifferenceResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Component
@Repository
public interface DifferenceResultRepository extends MongoRepository<DifferenceResult, String>{
    List<DifferenceResult> findByTimestampMsecBetween(Long start, Long end);
}
