package fr.timemanager.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateRangeTest {

  @Test
  void dateRange_valid_ok() {
    var range = new DateRange(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 15));
    assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 1, 10));
  }

  @Test
  void dateRange_endBeforeStart_throws() {
    assertThatThrownBy(() -> new DateRange(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 10)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void overlaps_trueCases() {
    var r1 = new DateRange(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));
    var r2 = new DateRange(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 25));
    assertThat(r1.overlaps(r2)).isTrue();
  }

  @Test
  void overlaps_adjacent_false() {
    var r1 = new DateRange(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 14));
    var r2 = new DateRange(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 20));
    assertThat(r1.overlaps(r2)).isFalse();
  }

  @Test
  void contains_dateInRange_true() {
    var range = new DateRange(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));
    assertThat(range.contains(LocalDate.of(2026, 1, 15))).isTrue();
  }
}
