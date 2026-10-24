package fr.timemanager.application.absence;

import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.AbsenceResponseMapper;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.absence.WorkingDaysCalculator;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDay;
import fr.timemanager.domain.shared.HalfDayType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Use case EF-007, EF-008 : le salarié demande la modification d'une absence validée. Une nouvelle
 * version est créée au statut EN_ATTENTE ; l'ancienne reste la référence active jusqu'à
 * approbation.
 */
public class RequestAbsenceModification {

  private final AbsenceRepository absenceRepository;
  private final AbsenceTypeRepository absenceTypeRepository;
  private final PublicHolidayRepository publicHolidayRepository;
  private final TeamRepository teamRepository;
  private final AuditRepository auditRepository;
  private final NotificationService notificationService;
  private final WorkScheduleRepository workScheduleRepository;
  private final WorkingDaysCalculator workingDaysCalculator = new WorkingDaysCalculator();

  public RequestAbsenceModification(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      PublicHolidayRepository publicHolidayRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      NotificationService notificationService,
      WorkScheduleRepository workScheduleRepository) {
    this.absenceRepository = absenceRepository;
    this.absenceTypeRepository = absenceTypeRepository;
    this.publicHolidayRepository = publicHolidayRepository;
    this.teamRepository = teamRepository;
    this.auditRepository = auditRepository;
    this.notificationService = notificationService;
    this.workScheduleRepository = workScheduleRepository;
  }

  public AbsenceResponseDto execute(
      UUID employeeId, UUID absenceId, CreateAbsenceRequestDto request, String correlationId) {
    Absence absence = AbsenceLookup.require(absenceRepository, absenceId);
    assertOwnedBy(absence, employeeId);
    VersionAbsence current = absence.getActiveVersion();
    assertModifiable(absence, current);

    VersionAbsence newVersion = buildVersion(absence, employeeId, request);
    absence.addPendingVersion(newVersion);
    absenceRepository.save(absence);

    auditRepository.save(
        new EvenementAudit(
            UUID.randomUUID(),
            absenceId,
            newVersion.getId(),
            AuditActionType.MODIFICATION_DEMANDEE,
            employeeId.toString(),
            "SALARIE",
            describe(current),
            describe(newVersion),
            UUID.fromString(correlationId),
            Instant.now()));

    notificationService.notifyAbsenceModified(employeeId, absenceId);
    notifyFirstLevelManager(absence.getTeamId(), absenceId);

    var type = absenceTypeRepository.findById(absence.getAbsenceTypeId()).orElseThrow();
    return AbsenceResponseMapper.toDto(absence, newVersion, type);
  }

  private void assertOwnedBy(Absence absence, UUID employeeId) {
    if (!absence.getEmployeeId().equals(employeeId)) {
      throw new SecurityException(
          "L'employé " + employeeId + " ne peut pas modifier l'absence d'un autre salarié");
    }
  }

  /** EF-007 : seule une absence validée se modifie ; une seule revalidation à la fois. */
  private void assertModifiable(Absence absence, VersionAbsence current) {
    if (current.getStatus() != AbsenceVersionStatus.VALIDEE) {
      throw new IllegalStateException(
          "Seule une absence validée peut être modifiée — statut actuel : " + current.getStatus());
    }
    if (absence.getPendingVersion().isPresent()) {
      throw new IllegalStateException(
          "Une demande de modification est déjà en cours de validation");
    }
  }

  private VersionAbsence buildVersion(
      Absence absence, UUID employeeId, CreateAbsenceRequestDto request) {
    var dateRange = new DateRange(request.startDate(), request.endDate());
    HalfDay halfDay =
        new HalfDay(
            request.halfDayStart() != null ? request.halfDayStart() : HalfDayType.NONE,
            request.halfDayEnd() != null ? request.halfDayEnd() : HalfDayType.NONE);
    List<LocalDate> holidays =
        publicHolidayRepository.findBetween(request.startDate(), request.endDate());
    BigDecimal workingDays =
        workingDaysCalculator.calculateWithSchedules(
            dateRange, halfDay, holidays, workScheduleRepository.findByUserId(employeeId));

    return new VersionAbsence(
        UUID.randomUUID(),
        absence.getId(),
        absence.nextVersionNumber(),
        dateRange,
        halfDay.start(),
        halfDay.end(),
        workingDays,
        AbsenceVersionStatus.EN_ATTENTE,
        employeeId,
        false,
        Instant.now());
  }

  /** EF-037 : la revalidation est soumise au N+1, comme une demande initiale. */
  private void notifyFirstLevelManager(UUID teamId, UUID absenceId) {
    teamRepository
        .findById(teamId)
        .map(team -> team.getN1ManagerId())
        .ifPresent(
            managerId ->
                notificationService.notifyPendingValidationToManager(managerId, absenceId));
  }

  private String describe(VersionAbsence version) {
    return version.getDateRange().startDate()
        + " → "
        + version.getDateRange().endDate()
        + " ("
        + version.getWorkingDays()
        + " j)";
  }
}
