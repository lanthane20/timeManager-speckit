package fr.timemanager.application.recurrence;

import fr.timemanager.domain.recurrence.RecurrenceInjectionLog;
import fr.timemanager.domain.recurrence.RecurrencePattern;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port — patrons de récurrence et historique d'injections (EF-028, EF-029). */
public interface RecurrenceRepository {

  RecurrencePattern savePattern(RecurrencePattern pattern);

  Optional<RecurrencePattern> findPatternById(UUID id);

  List<RecurrencePattern> findPatternsByEmployeeId(UUID employeeId);

  List<RecurrencePattern> findActivePatterns();

  RecurrenceInjectionLog saveInjectionLog(RecurrenceInjectionLog log);

  List<RecurrenceInjectionLog> findInjectionLogs();
}
