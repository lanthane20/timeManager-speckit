package fr.timemanager.application.absence;

import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.AbsenceResponseMapper;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceOverlapChecker;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.ExistingAbsenceInfo;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.absence.WorkingDaysCalculator;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDay;
import fr.timemanager.domain.shared.HalfDayType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Use case EF-001 à EF-005 : création d'une demande d'absence par un salarié. */
public class CreateAbsenceRequest {

  private final AbsenceRepository absenceRepository;
  private final AbsenceTypeRepository absenceTypeRepository;
  private final PublicHolidayRepository publicHolidayRepository;
  private final AuditRepository auditRepository;
  private final NotificationService notificationService;
  private final WorkingDaysCalculator workingDaysCalculator;
  private final AbsenceOverlapChecker overlapChecker;
  private final TeamRepository teamRepository;
  private final WorkScheduleRepository workScheduleRepository;

  public CreateAbsenceRequest(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      PublicHolidayRepository publicHolidayRepository,
      AuditRepository auditRepository,
      NotificationService notificationService,
      TeamRepository teamRepository,
      WorkScheduleRepository workScheduleRepository) {
    this.absenceRepository = absenceRepository;
    this.absenceTypeRepository = absenceTypeRepository;
    this.publicHolidayRepository = publicHolidayRepository;
    this.auditRepository = auditRepository;
    this.notificationService = notificationService;
    this.teamRepository = teamRepository;
    this.workScheduleRepository = workScheduleRepository;
    this.workingDaysCalculator = new WorkingDaysCalculator();
    this.overlapChecker = new AbsenceOverlapChecker();
  }

  public AbsenceResponseDto execute(
      UUID employeeId, UUID teamId, String correlationId, CreateAbsenceRequestDto request) {
    AbsenceType type = resolveActiveType(request.absenceTypeId());

    var dateRange = new DateRange(request.startDate(), request.endDate());

    List<ExistingAbsenceInfo> existing =
        absenceRepository.findOverlappingInfo(employeeId, request.startDate(), request.endDate());
    overlapChecker.assertNoConflict(dateRange, existing);

    BigDecimal workingDays = computeWorkingDays(employeeId, dateRange, request);

    UUID absenceId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();
    Instant now = Instant.now();

    VersionAbsence version =
        new VersionAbsence(
            versionId,
            absenceId,
            1,
            dateRange,
            request.halfDayStart(),
            request.halfDayEnd(),
            workingDays,
            AbsenceVersionStatus.EN_ATTENTE,
            employeeId,
            true,
            now);

    Absence absence =
        new Absence(
            absenceId, employeeId, type.getId(), teamId, AbsenceSimplifiedStatus.EN_ATTENTE, now);
    absence.addVersion(version);

    absenceRepository.save(absence);

    recordCreationAudit(absenceId, versionId, employeeId, correlationId, now);

    notificationService.notifyAbsenceCreated(employeeId, absenceId);
    notifyFirstLevelManager(teamId, absenceId);

    return toResponse(absence, version, type);
  }

  /**
   * EF-037 : à la création, seul le N+1 est sollicité — le N+2 n'est notifié qu'après la validation
   * de premier niveau, dans les équipes en double validation.
   */
  private void notifyFirstLevelManager(UUID teamId, UUID absenceId) {
    teamRepository
        .findById(teamId)
        .map(team -> team.getN1ManagerId())
        .ifPresent(
            managerId ->
                notificationService.notifyPendingValidationToManager(managerId, absenceId));
  }

  /** EF-002 : seul un type d'absence actif du référentiel peut être demandé. */
  private AbsenceType resolveActiveType(UUID absenceTypeId) {
    AbsenceType type =
        absenceTypeRepository
            .findById(absenceTypeId)
            .orElseThrow(() -> new IllegalArgumentException("Type d'absence inconnu"));
    if (!type.isActive()) {
      throw new IllegalArgumentException("Le type d'absence est inactif");
    }
    return type;
  }

  /**
   * EF-003, EF-004, EF-031b : jours ouvrés déduits des week-ends, jours fériés, demi-journées et
   * jours non travaillés du planning contractuel du salarié.
   */
  private BigDecimal computeWorkingDays(
      UUID employeeId, DateRange dateRange, CreateAbsenceRequestDto request) {
    List<java.time.LocalDate> holidays =
        publicHolidayRepository.findBetween(request.startDate(), request.endDate());
    HalfDay halfDay =
        new HalfDay(
            request.halfDayStart() != null ? request.halfDayStart() : HalfDayType.NONE,
            request.halfDayEnd() != null ? request.halfDayEnd() : HalfDayType.NONE);
    return workingDaysCalculator.calculateWithSchedules(
        dateRange, halfDay, holidays, workScheduleRepository.findByUserId(employeeId));
  }

  /** Constitution Principe V : toute mutation métier émet un événement d'audit persisté. */
  private void recordCreationAudit(
      UUID absenceId, UUID versionId, UUID employeeId, String correlationId, Instant now) {
    auditRepository.save(
        new EvenementAudit(
            UUID.randomUUID(),
            absenceId,
            versionId,
            AuditActionType.CREATION,
            employeeId.toString(),
            "SALARIE",
            null,
            null,
            UUID.fromString(correlationId),
            now));
  }

  private AbsenceResponseDto toResponse(Absence absence, VersionAbsence version, AbsenceType type) {
    return AbsenceResponseMapper.toDto(absence, version, type);
  }
}
