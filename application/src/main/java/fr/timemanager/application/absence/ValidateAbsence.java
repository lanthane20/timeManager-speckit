package fr.timemanager.application.absence;

import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.application.payroll.RegularisationDetector;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.user.User;
import java.time.Instant;
import java.util.UUID;

/**
 * Use case EF-009, EF-010 : validation d'une demande par le N+1. Le statut atteint dépend du mode
 * de validation de l'équipe — final en {@code N1_ONLY}, en attente du N+2 en {@code N1_AND_N2}.
 */
public class ValidateAbsence {

  private final AbsenceRepository absenceRepository;
  private final AuditRepository auditRepository;
  private final NotificationService notificationService;
  private final RegularisationDetector regularisationDetector;
  private final ManagerPerimeter perimeter;
  private final WorkflowEngine workflowEngine = new WorkflowEngine();

  public ValidateAbsence(
      AbsenceRepository absenceRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      NotificationService notificationService,
      AuthorizationService authorizationService,
      RegularisationDetector regularisationDetector) {
    this.absenceRepository = absenceRepository;
    this.auditRepository = auditRepository;
    this.notificationService = notificationService;
    this.regularisationDetector = regularisationDetector;
    this.perimeter = new ManagerPerimeter(teamRepository, authorizationService);
  }

  public void execute(User manager, UUID absenceId, String correlationId) {
    Absence absence = AbsenceLookup.require(absenceRepository, absenceId);
    Team team = perimeter.assertCanDecideOn(manager, absence);

    VersionAbsence version = absence.getVersionUnderReview();
    boolean awaitingN2 =
        workflowEngine.statusAfterN1Validation(team) == AbsenceVersionStatus.VALIDEE_N1;
    if (awaitingN2) {
      version.validateN1();
    } else {
      version.validate();
      // EF-008 : la version revalidée ne remplace l'ancienne qu'une fois approuvée.
      absence.promote(version);
    }
    absence.updateSimplifiedStatus();
    absenceRepository.save(absence);

    auditRepository.save(
        new EvenementAudit(
            UUID.randomUUID(),
            absenceId,
            version.getId(),
            awaitingN2 ? AuditActionType.VALIDATION_N1 : AuditActionType.VALIDATION_DIRECTE,
            manager.getId().toString(),
            "MANAGER",
            null,
            null,
            UUID.fromString(correlationId),
            Instant.now()));

    if (awaitingN2) {
      // EF-012 : le N+2 est sollicité seulement une fois le N+1 passé.
      notificationService.notifyPendingValidationToManager(team.getN2ManagerId(), absenceId);
    } else {
      regularisationDetector.detectOnValidation(absence, version);
      notificationService.notifyAbsenceValidated(absence.getEmployeeId(), absenceId);
    }
  }
}
