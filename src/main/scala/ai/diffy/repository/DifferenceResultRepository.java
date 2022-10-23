package ai.diffy.repository;

import ai.diffy.analysis.DifferenceResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@Repository
public interface DifferenceResultRepository extends MongoRepository<DifferenceResult, String>{}
