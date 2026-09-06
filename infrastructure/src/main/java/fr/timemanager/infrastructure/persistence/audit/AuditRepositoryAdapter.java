package fr.timemanager.infrastructure.persistence.audit;

import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.domain.audit.EvenementAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuditRepositoryAdapter implements AuditRepository {

  private final AuditEventJpaRepository jpaRepository;

  public AuditRepositoryAdapter(AuditEventJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public void save(EvenementAudit evenement) {
    jpaRepository.save(AuditEventEntity.fromDomain(evenement));
  }

  @Override
  public List<EvenementAudit> findByAbsenceId(UUID absenceId) {
    return jpaRepository.findByAbsenceIdOrderByOccurredAtAsc(absenceId.toString()).stream()
        .map(AuditEventEntity::toDomain)
        .toList();
  }
}
