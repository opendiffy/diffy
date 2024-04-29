package ai.diffy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@Repository
public interface NoiseRepository extends JpaRepository<Noise, String> {
}
