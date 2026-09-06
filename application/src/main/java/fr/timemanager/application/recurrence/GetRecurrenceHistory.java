package fr.timemanager.application.recurrence;

import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.domain.recurrence.RecurrenceInjectionLog;
import fr.timemanager.domain.user.User;
import java.util.Comparator;
import java.util.List;

/** Use case EF-029 : historique des injections, de la plus récente à la plus ancienne. */
public class GetRecurrenceHistory {

  private final RecurrenceRepository recurrenceRepository;

  public GetRecurrenceHistory(RecurrenceRepository recurrenceRepository) {
    this.recurrenceRepository = recurrenceRepository;
  }

  public List<RecurrenceInjectionLog> execute(User author) {
    if (!author.isRecurrenceEnabled()) {
      throw new UnauthorizedException(
          "Le module récurrences n'est pas activé pour " + author.getId());
    }
    return recurrenceRepository.findInjectionLogs().stream()
        .sorted(Comparator.comparing(RecurrenceInjectionLog::injectedAt).reversed())
        .toList();
  }
}
