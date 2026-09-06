package fr.timemanager.infrastructure.persistence.recurrence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrencePatternJpaRepository
    extends JpaRepository<RecurrencePatternEntity, String> {

  List<RecurrencePatternEntity> findByEmployeeId(String employeeId);

  List<RecurrencePatternEntity> findByActiveTrue();
}
