package ai.diffy.repository.transformations;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransformationRepository extends MongoRepository<Transformation, String> {}
