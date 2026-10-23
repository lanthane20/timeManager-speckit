package fr.timemanager.domain.absence;

import fr.timemanager.domain.shared.DateRange;
import java.util.List;

/** Vérifie les chevauchements d'absence. Le type Ecole (allowsOverlap=true) ne bloque pas. */
public class AbsenceOverlapChecker {

  public void assertNoConflict(DateRange requested, List<ExistingAbsenceInfo> existing) {
    existing.stream()
        .filter(e -> !e.allowsOverlap())
        .filter(e -> e.dateRange().overlaps(requested))
        .findFirst()
        .ifPresent(
            e -> {
              throw new AbsenceOverlapException(
                  "Chevauchement avec une absence existante : " + e.dateRange());
            });
  }
}
