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

/** Use case EF-011 : refus d'une demande — le motif est obligatoire, sans exception. */
public class RefuseAbsence {

  private final AbsenceRepository absenceRepository;
  private final AuditRepository auditRepository;
  private final NotificationService notificationService;
  private final ManagerPerimeter perimeter;

  public RefuseAbsence(
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

    VersionAbsence version = absence.getVersionUnderReview();
    version.refuse(reason);
    absence.updateSimplifiedStatus();
    absenceRepository.save(absence);

    auditRepository.save(
        new EvenementAudit(
            UUID.randomUUID(),
            absenceId,
            version.getId(),
            AuditActionType.REFUS,
            manager.getId().toString(),
            "MANAGER",
            null,
            reason,
            UUID.fromString(correlationId),
            Instant.now()));

    notificationService.notifyAbsenceRefused(absence.getEmployeeId(), absenceId, reason);
  }
}
