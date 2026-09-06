package fr.timemanager.domain.absence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDay;
import fr.timemanager.domain.shared.HalfDayType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 🔴 TDD T038 — WorkingDaysCalculator */
class WorkingDaysCalculatorTest {

  private WorkingDaysCalculator calculator;
  private List<LocalDate> publicHolidays;

  @BeforeEach
  void setUp() {
    // Jours fériés 2026 : 01/01, 06/04 (Pâques), 01/05, 08/05, 14/05 (Ascension),
    // 25/05 (Pentecôte), 14/07, 15/08, 01/11, 11/11, 25/12
    publicHolidays =
        List.of(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 4, 6),
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 8),
            LocalDate.of(2026, 5, 14),
            LocalDate.of(2026, 5, 25),
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 8, 15),
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 11),
            LocalDate.of(2026, 12, 25));
    calculator = new WorkingDaysCalculator();
  }

  @Test
  void oneWeek_noHolidays_5jours() {
    // Lundi 9 au vendredi 13 mars 2026
    var range = new DateRange(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 13));
    var result = calculator.calculate(range, HalfDay.NONE, List.of(), List.of());
    assertThat(result).isEqualByComparingTo(new BigDecimal("5.0"));
  }

  @Test
  void weekendExcluded() {
    // Vendredi 13 au lundi 16 mars 2026 → 2 jours (ven + lun)
    var range = new DateRange(LocalDate.of(2026, 3, 13), LocalDate.of(2026, 3, 16));
    var result = calculator.calculate(range, HalfDay.NONE, List.of(), List.of());
    assertThat(result).isEqualByComparingTo(new BigDecimal("2.0"));
  }

  @Test
  void publicHolidayExcluded() {
    // Lundi 2 novembre au vendredi 6 novembre 2026
    // → 01/11 (Toussaint, dim) et 11/11 (mercredi) hors de cette semaine
    // Juste 5 jours ordinaires
    var range = new DateRange(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13));
    var result = calculator.calculate(range, HalfDay.NONE, publicHolidays, List.of());
    assertThat(result).isEqualByComparingTo(new BigDecimal("4.0")); // 11/11 est dans cette semaine
  }

  @Test
  void halfDayPmStart() {
    // Mercredi 11 mars 2026 (une journée) avec demi-journée PM start → 0.5
    var range = new DateRange(LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 11));
    var halfDay = new HalfDay(HalfDayType.PM, HalfDayType.NONE);
    var result = calculator.calculate(range, halfDay, List.of(), List.of());
    assertThat(result).isEqualByComparingTo(new BigDecimal("0.5"));
  }

  @Test
  void halfDayAmEnd() {
    // Vendredi 13 mars 2026 (une journée) avec demi-journée AM end → 0.5
    var range = new DateRange(LocalDate.of(2026, 3, 13), LocalDate.of(2026, 3, 13));
    var halfDay = new HalfDay(HalfDayType.NONE, HalfDayType.AM);
    var result = calculator.calculate(range, halfDay, List.of(), List.of());
    assertThat(result).isEqualByComparingTo(new BigDecimal("0.5"));
  }

  @Test
  void crossYearBoundary_counted() {
    // 30/12/2026 (mer) au 04/01/2027 (lun) → 30/12, 31/12, 02/01/2027, 04/01/2027 = 4 jours
    // (01/01/2027 férié, weekend exclus)
    var range = new DateRange(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 4));
    List<LocalDate> holidays2027 = List.of(LocalDate.of(2027, 1, 1));
    var result = calculator.calculate(range, HalfDay.NONE, holidays2027, List.of());
    assertThat(result).isEqualByComparingTo(new BigDecimal("3.0"));
  }

  @Test
  void nonWorkingDaysExcluded() {
    // Semaine complète mais salarié ne travaille pas le lundi → 4 jours
    var range = new DateRange(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 13));
    List<java.time.DayOfWeek> nonWorkingDays = List.of(java.time.DayOfWeek.MONDAY);
    var result = calculator.calculate(range, HalfDay.NONE, List.of(), nonWorkingDays);
    assertThat(result).isEqualByComparingTo(new BigDecimal("4.0"));
  }
}
