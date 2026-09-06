package fr.timemanager.domain.recurrence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * EF-029 : trace immuable d'une injection — date, source, périmètre concerné et nombre d'événements
 * créés.
 */
public record RecurrenceInjectionLog(
    UUID id,
    UUID patternId,
    InjectionSource source,
    Instant injectedAt,
    UUID injectedByUserId,
    int eventsCreatedCount,
    LocalDate periodStart,
    LocalDate periodEnd) {

  public RecurrenceInjectionLog {
    if (id == null) {
      throw new IllegalArgumentException("id obligatoire");
    }
    if (source == null) {
      throw new IllegalArgumentException("source obligatoire");
    }
    if (injectedAt == null) {
      throw new IllegalArgumentException("injectedAt obligatoire");
    }
    if (eventsCreatedCount < 0) {
      throw new IllegalArgumentException("eventsCreatedCount ne peut pas être négatif");
    }
  }
}
