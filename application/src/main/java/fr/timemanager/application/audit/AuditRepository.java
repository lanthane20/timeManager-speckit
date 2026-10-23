package fr.timemanager.application.audit;

import fr.timemanager.domain.audit.EvenementAudit;
import java.util.List;
import java.util.UUID;

/** Port — interface de persistance des événements d'audit. */
public interface AuditRepository {

  void save(EvenementAudit evenement);

  List<EvenementAudit> findByAbsenceId(UUID absenceId);
}
