package fr.timemanager.application.counter;

import fr.timemanager.application.absence.ManagerPerimeter;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.domain.counter.CompteurAbsence;
import fr.timemanager.domain.user.User;
import java.util.List;
import java.util.UUID;

/**
 * Use case : compteurs d'un salarié consultés par son manager. Le périmètre est vérifié dans la
 * couche application, jamais dans le contrôleur (Constitution Principe IV).
 */
public class GetEmployeeCounters {

  private final GetAbsenceCounters getAbsenceCounters;
  private final TeamRepository teamRepository;
  private final ManagerPerimeter perimeter;

  public GetEmployeeCounters(
      GetAbsenceCounters getAbsenceCounters,
      TeamRepository teamRepository,
      AuthorizationService authorizationService) {
    this.getAbsenceCounters = getAbsenceCounters;
    this.teamRepository = teamRepository;
    this.perimeter = new ManagerPerimeter(teamRepository, authorizationService);
  }

  public List<CompteurAbsence> execute(User manager, UUID employeeId) {
    UUID teamId =
        teamRepository
            .findTeamIdByMemberId(employeeId)
            .orElseThrow(
                () -> new IllegalStateException("Aucune équipe pour le salarié " + employeeId));
    perimeter.assertManagesEmployee(manager, teamId, employeeId);
    return getAbsenceCounters.execute(employeeId);
  }
}
