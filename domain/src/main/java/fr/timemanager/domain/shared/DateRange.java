package fr.timemanager.domain.shared;

import java.time.LocalDate;

/** Plage de dates inclusive. Immuable. */
public record DateRange(LocalDate startDate, LocalDate endDate) {

  public DateRange {
    if (startDate == null) {
      throw new IllegalArgumentException("startDate obligatoire");
    }
    if (endDate == null) {
      throw new IllegalArgumentException("endDate obligatoire");
    }
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException(
          "endDate doit être >= startDate : " + startDate + " - " + endDate);
    }
  }

  public boolean overlaps(DateRange other) {
    return !this.endDate.isBefore(other.startDate) && !other.endDate.isBefore(this.startDate);
  }

  public boolean contains(LocalDate date) {
    return !date.isBefore(startDate) && !date.isAfter(endDate);
  }
}
