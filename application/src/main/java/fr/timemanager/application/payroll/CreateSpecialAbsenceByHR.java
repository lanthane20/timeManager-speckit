package fr.timemanager.application.payroll;

import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.absence.PublicHolidayRepository;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.AbsenceResponseMapper;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
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
 * Use case EF-022 : saisie par la RH d'une absence spéciale (arrêt maladie, congé maternité, congé
 * spécial…) pour tout salarié. L'absence est directement validée — elle ne passe pas par le circuit
 * manager — et les règles de chevauchement ne s'appliquent pas à ce type de saisie subie.
 */
public class CreateSpecialAbsenceByHR {

  private final AbsenceRepository absenceRepository;
  private final AbsenceTypeRepository absenceTypeRepository;
  private final PublicHolidayRepository publicHolidayRepository;
  private final TeamRepository teamRepository;
  private final AuditRepository auditRepository;
  private final AuthorizationService authorizationService;
  private final WorkingDaysCalculator workingDaysCalculator = new WorkingDaysCalculator();

  public CreateSpecialAbsenceByHR(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      PublicHolidayRepository publicHolidayRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      AuthorizationService authorizationService) {
    this.absenceRepository = absenceRepository;
    this.absenceTypeRepository = absenceTypeRepository;
    this.publicHolidayRepository = publicHolidayRepository;
    this.teamRepository = teamRepository;
    this.auditRepository = auditRepository;
    this.authorizationService = authorizationService;
  }

  public AbsenceResponseDto execute(
      User hrUser, UUID employeeId, CreateAbsenceRequestDto request, String correlationId) {
    authorizationService.assertHasRole(hrUser, UserRole.RH);

    var type =
        absenceTypeRepository
            .findById(request.absenceTypeId())
            .orElseThrow(() -> new IllegalArgumentException("Type d'absence inconnu"));
    UUID teamId =
        teamRepository
            .findTeamIdByMemberId(employeeId)
            .orElseThrow(
                () -> new IllegalStateException("Aucune équipe pour le salarié " + employeeId));

    Instant now = Instant.now();
    UUID absenceId = UUID.randomUUID();
    VersionAbsence version = buildVersion(absenceId, hrUser, request, now);
    Absence absence =
        new Absence(
            absenceId, employeeId, type.getId(), teamId, AbsenceSimplifiedStatus.VALIDEE, now);
    absence.addVersion(version);
    absenceRepository.save(absence);

    auditRepository.save(
        new EvenementAudit(
            UUID.randomUUID(),
            absenceId,
            version.getId(),
            AuditActionType.CREATION_RH,
            hrUser.getId().toString(),
            "RH",
            null,
            type.getCode()
                + " du "
                + request.startDate()
                + " au "
                + request.endDate()
                + " pour "
                + employeeId,
            UUID.fromString(correlationId),
            now));

    return AbsenceResponseMapper.toDto(absence, version, type);
  }

  private VersionAbsence buildVersion(
      UUID absenceId, User hrUser, CreateAbsenceRequestDto request, Instant now) {
    var dateRange = new DateRange(request.startDate(), request.endDate());
    HalfDay halfDay =
        new HalfDay(
            request.halfDayStart() != null ? request.halfDayStart() : HalfDayType.NONE,
            request.halfDayEnd() != null ? request.halfDayEnd() : HalfDayType.NONE);
    List<LocalDate> holidays =
        publicHolidayRepository.findBetween(request.startDate(), request.endDate());
    BigDecimal workingDays =
        workingDaysCalculator.calculate(dateRange, halfDay, holidays, List.of());

    return new VersionAbsence(
        UUID.randomUUID(),
        absenceId,
        1,
        dateRange,
        halfDay.start(),
        halfDay.end(),
        workingDays,
        AbsenceVersionStatus.VALIDEE,
        hrUser.getId(),
        true,
        now);
  }
}
