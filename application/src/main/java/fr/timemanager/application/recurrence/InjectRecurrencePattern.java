package fr.timemanager.application.recurrence;

import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.absence.PublicHolidayRepository;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.absence.WorkingDaysCalculator;
import fr.timemanager.domain.recurrence.InjectionSource;
import fr.timemanager.domain.recurrence.RecurrenceInjectionLog;
import fr.timemanager.domain.recurrence.RecurrencePattern;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDay;
import fr.timemanager.domain.shared.HalfDayType;
import fr.timemanager.domain.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Use case EF-028 à EF-030 : injection d'absences récurrentes (Ecole, jours non travaillés). Le
 * module n'est ouvert qu'aux utilisateurs explicitement désignés (EF-030) et les chevauchements ne
 * bloquent jamais l'injection — ils sont remontés comme avertissements (scénario 3 de US6).
 */
public class InjectRecurrencePattern {

  private final AbsenceRepository absenceRepository;
  private final AbsenceTypeRepository absenceTypeRepository;
  private final PublicHolidayRepository publicHolidayRepository;
  private final TeamRepository teamRepository;
  private final RecurrenceRepository recurrenceRepository;
  private final WorkingDaysCalculator workingDaysCalculator = new WorkingDaysCalculator();

  public InjectRecurrencePattern(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      PublicHolidayRepository publicHolidayRepository,
      TeamRepository teamRepository,
      RecurrenceRepository recurrenceRepository) {
    this.absenceRepository = absenceRepository;
    this.absenceTypeRepository = absenceTypeRepository;
    this.publicHolidayRepository = publicHolidayRepository;
    this.teamRepository = teamRepository;
    this.recurrenceRepository = recurrenceRepository;
  }

  /**
   * Injecte une absence par date demandée et journalise l'opération.
   *
   * @param dates dates à injecter — chacune produit une absence d'une journée
   */
  public InjectionResult execute(
      User author, UUID patternId, List<LocalDate> dates, InjectionSource source) {
    assertRecurrenceEnabled(author);
    RecurrencePattern pattern = requirePattern(patternId);
    var type =
        absenceTypeRepository
            .findById(pattern.getAbsenceTypeId())
            .orElseThrow(() -> new IllegalStateException("Type d'absence du patron introuvable"));
    UUID teamId =
        teamRepository
            .findTeamIdByMemberId(pattern.getEmployeeId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Aucune équipe pour le salarié " + pattern.getEmployeeId()));

    List<String> warnings = new ArrayList<>();
    int created = 0;
    for (LocalDate date : dates.stream().sorted().distinct().toList()) {
      collectOverlapWarning(pattern.getEmployeeId(), date).ifPresent(warnings::add);
      if (injectOneDay(pattern, teamId, date, author)) {
        created++;
      }
    }

    RecurrenceInjectionLog log =
        recurrenceRepository.saveInjectionLog(
            new RecurrenceInjectionLog(
                UUID.randomUUID(),
                pattern.getId(),
                source,
                Instant.now(),
                author.getId(),
                created,
                dates.stream().min(LocalDate::compareTo).orElse(null),
                dates.stream().max(LocalDate::compareTo).orElse(null)));

    return new InjectionResult(log, warnings, type.getCode());
  }

  /** EF-030 : le module n'est ouvert qu'aux utilisateurs explicitement désignés. */
  private void assertRecurrenceEnabled(User author) {
    if (!author.isRecurrenceEnabled()) {
      throw new UnauthorizedException(
          "Le module récurrences n'est pas activé pour " + author.getId());
    }
  }

  private RecurrencePattern requirePattern(UUID patternId) {
    RecurrencePattern pattern =
        recurrenceRepository
            .findPatternById(patternId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Patron de récurrence introuvable : " + patternId));
    if (!pattern.isActive()) {
      throw new IllegalStateException("Le patron de récurrence " + patternId + " est désactivé");
    }
    return pattern;
  }

  /** Scénario 3 de US6 : le chevauchement est signalé mais ne bloque pas l'injection. */
  private java.util.Optional<String> collectOverlapWarning(UUID employeeId, LocalDate date) {
    boolean overlaps = !absenceRepository.findOverlappingInfo(employeeId, date, date).isEmpty();
    return overlaps
        ? java.util.Optional.of("Chevauchement avec une absence existante le " + date)
        : java.util.Optional.empty();
  }

  /**
   * @return {@code true} si une absence a été créée ; {@code false} si la date n'est pas ouvrée
   */
  private boolean injectOneDay(
      RecurrencePattern pattern, UUID teamId, LocalDate date, User author) {
    Instant now = Instant.now();
    UUID absenceId = UUID.randomUUID();
    var dateRange = new DateRange(date, date);
    BigDecimal workingDays =
        workingDaysCalculator.calculate(
            dateRange, HalfDay.NONE, publicHolidayRepository.findBetween(date, date), List.of());
    if (workingDays.compareTo(BigDecimal.ZERO) <= 0) {
      // Jour férié ou week-end : rien à injecter pour cette date.
      return false;
    }

    VersionAbsence version =
        new VersionAbsence(
            UUID.randomUUID(),
            absenceId,
            1,
            dateRange,
            HalfDayType.NONE,
            HalfDayType.NONE,
            workingDays,
            AbsenceVersionStatus.VALIDEE,
            author.getId(),
            true,
            now);
    Absence absence =
        new Absence(
            absenceId,
            pattern.getEmployeeId(),
            pattern.getAbsenceTypeId(),
            teamId,
            AbsenceSimplifiedStatus.VALIDEE,
            now);
    absence.addVersion(version);
    absenceRepository.save(absence);
    return true;
  }

  /** Résultat d'une injection : trace journalisée et avertissements non bloquants. */
  public record InjectionResult(
      RecurrenceInjectionLog log, List<String> warnings, String absenceTypeCode) {}
}
