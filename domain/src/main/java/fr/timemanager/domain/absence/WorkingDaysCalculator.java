package fr.timemanager.domain.absence;

import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDay;
import fr.timemanager.domain.user.WorkSchedule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Calcule les jours ouvrés d'une plage de dates en excluant week-ends, jours fériés et JNT. */
public class WorkingDaysCalculator {

  private static final Set<DayOfWeek> WEEKEND = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

  public BigDecimal calculate(
      DateRange range,
      HalfDay halfDay,
      List<LocalDate> publicHolidays,
      List<DayOfWeek> nonWorkingDays) {
    long workDays = countWorkingDays(range, publicHolidays, nonWorkingDays);
    double adjustment = halfDay.startAdjustment() + halfDay.endAdjustment();
    return BigDecimal.valueOf(workDays + adjustment).setScale(1, RoundingMode.HALF_UP);
  }

  /**
   * EF-002 et EF-031b : variante appliquant le planning contractuel du salarié. Pour chaque jour,
   * le planning en vigueur à cette date détermine les jours non travaillés ; un planning dont la
   * date d'effet est postérieure n'est pas appliqué.
   */
  public BigDecimal calculateWithSchedules(
      DateRange range,
      HalfDay halfDay,
      List<LocalDate> publicHolidays,
      List<WorkSchedule> schedules) {
    long workDays =
        range
            .startDate()
            .datesUntil(range.endDate().plusDays(1))
            .filter(date -> isWorkingDay(date, publicHolidays, nonWorkingDaysOn(schedules, date)))
            .count();
    double adjustment = halfDay.startAdjustment() + halfDay.endAdjustment();
    return BigDecimal.valueOf(workDays + adjustment).setScale(1, RoundingMode.HALF_UP);
  }

  /** Jours non travaillés issus du planning en vigueur à la date donnée, s'il en existe un. */
  private List<DayOfWeek> nonWorkingDaysOn(List<WorkSchedule> schedules, LocalDate date) {
    return schedules.stream()
        .filter(schedule -> schedule.appliesOn(date))
        .findFirst()
        .map(schedule -> List.copyOf(schedule.nonWorkingDays()))
        .orElseGet(List::of);
  }

  private long countWorkingDays(
      DateRange range, List<LocalDate> holidays, List<DayOfWeek> nonWorkingDays) {
    return range
        .startDate()
        .datesUntil(range.endDate().plusDays(1))
        .filter(date -> isWorkingDay(date, holidays, nonWorkingDays))
        .count();
  }

  private boolean isWorkingDay(
      LocalDate date, List<LocalDate> holidays, List<DayOfWeek> nonWorkingDays) {
    return !WEEKEND.contains(date.getDayOfWeek())
        && !holidays.contains(date)
        && !nonWorkingDays.contains(date.getDayOfWeek());
  }
}
