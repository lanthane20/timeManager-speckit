package fr.timemanager.application.payroll;

import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.absence.PublicHolidayRepository;
import fr.timemanager.application.absence.WorkScheduleRepository;
import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.AbsenceResponseMapper;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.absence.WorkingDaysCalculator;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDay;
import fr.timemanager.domain.shared.HalfDayType;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Use case EF-023 : correction RH d'une absence existante. La correction crée une version validée
 * qui devient la référence et conserve une trace immuable (valeur avant, valeur après, auteur,
 * date).
 */
public class CorrectAbsence {

  private final AbsenceRepository absenceRepository;
  private final AbsenceTypeRepository absenceTypeRepository;
  private final PublicHolidayRepository publicHolidayRepository;
  private final AuditRepository auditRepository;
  private final AuthorizationService authorizationService;
  private final WorkScheduleRepository workScheduleRepository;
  private final WorkingDaysCalculator workingDaysCalculator = new WorkingDaysCalculator();

  public CorrectAbsence(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      PublicHolidayRepository publicHolidayRepository,
      AuditRepository auditRepository,
      AuthorizationService authorizationService,
      WorkScheduleRepository workScheduleRepository) {
    this.absenceRepository = absenceRepository;
    this.absenceTypeRepository = absenceTypeRepository;
    this.publicHolidayRepository = publicHolidayRepository;
    this.auditRepository = auditRepository;
    this.authorizationService = authorizationService;
    this.workScheduleRepository = workScheduleRepository;
  }

  public AbsenceResponseDto execute(
      User hrUser, UUID absenceId, CreateAbsenceRequestDto request, String correlationId) {
    authorizationService.assertHasRole(hrUser, UserRole.RH);
    Absence absence =
        absenceRepository
            .findById(absenceId)
            .orElseThrow(() -> new IllegalArgumentException("Absence introuvable : " + absenceId));

    VersionAbsence previous = absence.getActiveVersion();
    VersionAbsence corrected = buildCorrectedVersion(absence, hrUser, request);
    absence.addPendingVersion(corrected);
    absence.promote(corrected);
    absence.updateSimplifiedStatus();
    absenceRepository.save(absence);

    auditRepository.save(
        new EvenementAudit(
            UUID.randomUUID(),
            absenceId,
            corrected.getId(),
            AuditActionType.CORRECTION_RH,
            hrUser.getId().toString(),
            "RH",
            describe(previous),
            describe(corrected),
            UUID.fromString(correlationId),
            Instant.now()));

    var type = absenceTypeRepository.findById(absence.getAbsenceTypeId()).orElseThrow();
    return AbsenceResponseMapper.toDto(absence, corrected, type);
  }

  private VersionAbsence buildCorrectedVersion(
      Absence absence, User hrUser, CreateAbsenceRequestDto request) {
    var dateRange = new DateRange(request.startDate(), request.endDate());
    HalfDay halfDay =
        new HalfDay(
            request.halfDayStart() != null ? request.halfDayStart() : HalfDayType.NONE,
            request.halfDayEnd() != null ? request.halfDayEnd() : HalfDayType.NONE);
    List<LocalDate> holidays =
        publicHolidayRepository.findBetween(request.startDate(), request.endDate());
    BigDecimal workingDays =
        workingDaysCalculator.calculateWithSchedules(
            dateRange,
            halfDay,
            holidays,
            workScheduleRepository.findByUserId(absence.getEmployeeId()));

    return new VersionAbsence(
        UUID.randomUUID(),
        absence.getId(),
        absence.nextVersionNumber(),
        dateRange,
        halfDay.start(),
        halfDay.end(),
        workingDays,
        AbsenceVersionStatus.VALIDEE,
        hrUser.getId(),
        false,
        Instant.now());
  }

  private String describe(VersionAbsence version) {
    return version.getDateRange().startDate()
        + " au "
        + version.getDateRange().endDate()
        + " ("
        + version.getWorkingDays()
        + " jours ouvrés)";
  }
}
