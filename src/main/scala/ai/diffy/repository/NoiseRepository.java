package ai.diffy.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@Repository
public interface NoiseRepository extends MongoRepository<Noise, String> {
}
