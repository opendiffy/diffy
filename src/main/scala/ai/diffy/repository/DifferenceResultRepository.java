package ai.diffy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import ai.diffy.analysis.DifferenceResult;

@Component
@Repository
public interface DifferenceResultRepository extends JpaRepository<DifferenceResult, String>{
    List<DifferenceResult> findByTimestampMsecBetween(Long start, Long end);
}
