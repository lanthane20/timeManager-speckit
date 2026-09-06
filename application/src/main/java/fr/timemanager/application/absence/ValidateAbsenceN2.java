package fr.timemanager.application.absence;

import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.application.payroll.RegularisationDetector;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.user.User;
import java.time.Instant;
import java.util.UUID;

/**
 * Use case EF-012 : validation de second niveau. Le N+2 peut valider une demande déjà passée par le
 * N+1 (statut VALIDEE_N1) ou court-circuiter le N+1 sur une demande encore EN_ATTENTE.
 */
public class ValidateAbsenceN2 {

  private final AbsenceRepository absenceRepository;
  private final AuditRepository auditRepository;
  private final NotificationService notificationService;
  private final AuthorizationService authorizationService;
  private final RegularisationDetector regularisationDetector;
  private final ManagerPerimeter perimeter;
  private final WorkflowEngine workflowEngine = new WorkflowEngine();

  public ValidateAbsenceN2(
      AbsenceRepository absenceRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      NotificationService notificationService,
      AuthorizationService authorizationService,
      RegularisationDetector regularisationDetector) {
    this.absenceRepository = absenceRepository;
    this.auditRepository = auditRepository;
    this.notificationService = notificationService;
    this.authorizationService = authorizationService;
    this.regularisationDetector = regularisationDetector;
    this.perimeter = new ManagerPerimeter(teamRepository, authorizationService);
  }

  public void execute(User manager, UUID absenceId, String correlationId) {
    Absence absence = AbsenceLookup.require(absenceRepository, absenceId);
    Team team = perimeter.assertCanDecideOn(manager, absence);
    authorizationService.assertIsN2ManagerOf(manager, team.getN2ManagerId());
    if (!workflowEngine.allowsN2Validation(team)) {
      throw new UnauthorizedException(
          "L'équipe " + team.getId() + " n'est pas configurée en double validation");
    }

    VersionAbsence version = absence.getVersionUnderReview();
    version.validate();
    // EF-008 : la version revalidée ne remplace l'ancienne qu'une fois approuvée.
    absence.promote(version);
    absence.updateSimplifiedStatus();
    absenceRepository.save(absence);

    auditRepository.save(
        new EvenementAudit(
            UUID.randomUUID(),
            absenceId,
            version.getId(),
            AuditActionType.VALIDATION_N2,
            manager.getId().toString(),
            "MANAGER",
            null,
            null,
            UUID.fromString(correlationId),
            Instant.now()));

    regularisationDetector.detectOnValidation(absence, version);
    notificationService.notifyAbsenceValidated(absence.getEmployeeId(), absenceId);
  }
}
