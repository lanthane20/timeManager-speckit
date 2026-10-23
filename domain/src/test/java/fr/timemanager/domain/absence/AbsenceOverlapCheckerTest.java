package fr.timemanager.domain.absence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.timemanager.domain.shared.DateRange;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 🔴 TDD T039 — AbsenceOverlapChecker */
class AbsenceOverlapCheckerTest {

  private AbsenceOverlapChecker checker;

  @BeforeEach
  void setUp() {
    checker = new AbsenceOverlapChecker();
  }

  @Test
  void noOverlap_ok() {
    var existing = existingAbsence(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), false);
    var requested = new DateRange(LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 10));
    checker.assertNoConflict(requested, List.of(existing));
  }

  @Test
  void overlap_notAllowsOverlap_throws() {
    var existing = existingAbsence(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), false);
    var requested = new DateRange(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 15));
    assertThatThrownBy(() -> checker.assertNoConflict(requested, List.of(existing)))
        .isInstanceOf(AbsenceOverlapException.class);
  }

  @Test
  void overlap_allowsOverlap_ok() {
    // Type Ecole : allowsOverlap = true → pas de blocage
    var existing = existingAbsence(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), true);
    var requested = new DateRange(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 15));
    checker.assertNoConflict(requested, List.of(existing));
  }

  private ExistingAbsenceInfo existingAbsence(
      LocalDate start, LocalDate end, boolean allowsOverlap) {
    return new ExistingAbsenceInfo(new DateRange(start, end), allowsOverlap);
  }
}
