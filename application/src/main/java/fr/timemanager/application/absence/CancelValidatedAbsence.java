package fr.timemanager.application.absence;

import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import fr.timemanager.domain.user.User;
import java.time.Instant;
import java.util.UUID;

/** Use case EF-013 : annulation d'une absence validée par le manager, justification obligatoire. */
public class CancelValidatedAbsence {

  private final AbsenceRepository absenceRepository;
  private final AuditRepository auditRepository;
  private final NotificationService notificationService;
  private final ManagerPerimeter perimeter;

  public CancelValidatedAbsence(
      AbsenceRepository absenceRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      NotificationService notificationService,
      AuthorizationService authorizationService) {
    this.absenceRepository = absenceRepository;
    this.auditRepository = auditRepository;
    this.notificationService = notificationService;
    this.perimeter = new ManagerPerimeter(teamRepository, authorizationService);
  }

  public void execute(User manager, UUID absenceId, String reason, String correlationId) {
    Absence absence = AbsenceLookup.require(absenceRepository, absenceId);
    perimeter.assertCanDecideOn(manager, absence);

    VersionAbsence version = absence.getActiveVersion();
    version.cancelByManager(reason);
    absence.updateSimplifiedStatus();
    absenceRepository.save(absence);

    auditRepository.save(
        new EvenementAudit(
            UUID.randomUUID(),
            absenceId,
            version.getId(),
            AuditActionType.ANNULATION_MANAGER,
            manager.getId().toString(),
            "MANAGER",
            null,
            reason,
            UUID.fromString(correlationId),
            Instant.now()));

    notificationService.notifyAbsenceCancelledByManager(absence.getEmployeeId(), absenceId, reason);
  }
}
