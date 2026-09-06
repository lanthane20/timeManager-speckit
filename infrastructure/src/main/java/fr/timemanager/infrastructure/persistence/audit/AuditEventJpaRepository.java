package fr.timemanager.infrastructure.persistence.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, String> {

  List<AuditEventEntity> findByAbsenceIdOrderByOccurredAtAsc(String absenceId);
}
