package fr.timemanager.application.absence;

import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import java.time.Instant;
import java.util.UUID;

/** Use case EF-006 : annulation d'une demande par le salarié (statuts EN_ATTENTE ou VALIDEE_N1). */
public class CancelAbsenceRequest {

  private final AbsenceRepository absenceRepository;
  private final AuditRepository auditRepository;
  private final NotificationService notificationService;

  public CancelAbsenceRequest(
      AbsenceRepository absenceRepository,
      AuditRepository auditRepository,
      NotificationService notificationService) {
    this.absenceRepository = absenceRepository;
    this.auditRepository = auditRepository;
    this.notificationService = notificationService;
  }

  public void execute(UUID employeeId, UUID absenceId, String correlationId) {
    Absence absence =
        absenceRepository
            .findById(absenceId)
            .orElseThrow(() -> new IllegalArgumentException("Absence introuvable : " + absenceId));

    if (!absence.getEmployeeId().equals(employeeId)) {
      throw new SecurityException(
          "L'employé " + employeeId + " ne peut pas annuler l'absence de quelqu'un d'autre");
    }

    var version = absence.getActiveVersion();
    version.cancelByEmployee();
    absence.updateSimplifiedStatus();
    absenceRepository.save(absence);

    auditRepository.save(
        new EvenementAudit(
            UUID.randomUUID(),
            absenceId,
            version.getId(),
            AuditActionType.ANNULATION_SALARIE,
            employeeId.toString(),
            "SALARIE",
            null,
            null,
            UUID.fromString(correlationId),
            Instant.now()));
  }
}
