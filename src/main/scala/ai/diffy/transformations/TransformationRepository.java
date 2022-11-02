package ai.diffy.transformations;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@Repository
public interface TransformationRepository extends MongoRepository<Transformation, String> {}
