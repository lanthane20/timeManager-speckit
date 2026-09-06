package fr.timemanager.domain.absence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDay;
import fr.timemanager.domain.user.WorkSchedule;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** EF-031b : prise en compte du planning contractuel dans le calcul des jours ouvrés. */
class WorkingDaysCalculatorContractTest {

  private static final UUID EMPLOYEE_ID = UUID.randomUUID();
  private final WorkingDaysCalculator calculator = new WorkingDaysCalculator();

  /** Semaine du lundi 7 au vendredi 11 septembre 2026. */
  private final DateRange fullWeek =
      new DateRange(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 11));

  @Test
  void sansPlanningContractuel_touteLaSemaineEstOuvree() {
    BigDecimal days =
        calculator.calculateWithSchedules(fullWeek, HalfDay.NONE, List.of(), List.of());

    assertThat(days).isEqualByComparingTo(new BigDecimal("5.0"));
  }

  @Test
  void salarieAQuatreCinquiemes_leMercrediNEstPasCompte() {
    WorkSchedule fourFifths =
        schedule(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            LocalDate.of(2026, 1, 1),
            null);

    BigDecimal days =
        calculator.calculateWithSchedules(fullWeek, HalfDay.NONE, List.of(), List.of(fourFifths));

    assertThat(days).isEqualByComparingTo(new BigDecimal("4.0"));
  }

  @Test
  void planningDontLaDateDEffetEstFuture_nEstPasApplique() {
    WorkSchedule futureSchedule =
        schedule(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            LocalDate.of(2026, 10, 1),
            null);

    BigDecimal days =
        calculator.calculateWithSchedules(
            fullWeek, HalfDay.NONE, List.of(), List.of(futureSchedule));

    assertThat(days).isEqualByComparingTo(new BigDecimal("5.0"));
  }

  @Test
  void planningExpire_nEstPlusApplique() {
    WorkSchedule expiredSchedule =
        schedule(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 8, 31));

    BigDecimal days =
        calculator.calculateWithSchedules(
            fullWeek, HalfDay.NONE, List.of(), List.of(expiredSchedule));

    assertThat(days).isEqualByComparingTo(new BigDecimal("5.0"));
  }

  @Test
  void planningContractuelEtJourFerie_seCumulentSansDoubleDecompte() {
    WorkSchedule fourFifths =
        schedule(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            LocalDate.of(2026, 1, 1),
            null);

    BigDecimal days =
        calculator.calculateWithSchedules(
            fullWeek, HalfDay.NONE, List.of(LocalDate.of(2026, 9, 10)), List.of(fourFifths));

    assertThat(days).isEqualByComparingTo(new BigDecimal("3.0"));
  }

  @Test
  void leMotifDePlanningEstSerialiseEtRelu() {
    WorkSchedule schedule =
        schedule(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            LocalDate.of(2026, 1, 1),
            null);

    assertThat(schedule.toPattern()).isEqualTo("MONDAY,TUESDAY,THURSDAY,FRIDAY");
    assertThat(WorkSchedule.parsePattern(schedule.toPattern())).isEqualTo(schedule.workingDays());
    assertThat(schedule.nonWorkingDays()).containsExactly(DayOfWeek.WEDNESDAY);
  }

  private WorkSchedule schedule(
      Set<DayOfWeek> workingDays, LocalDate effectiveFrom, LocalDate effectiveUntil) {
    return new WorkSchedule(
        UUID.randomUUID(), EMPLOYEE_ID, workingDays, effectiveFrom, effectiveUntil, Instant.now());
  }
}
