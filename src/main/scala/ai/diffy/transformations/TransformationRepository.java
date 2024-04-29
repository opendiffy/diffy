package ai.diffy.transformations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@Repository
public interface TransformationRepository extends JpaRepository<Transformation, String> {}
