package fr.timemanager.application.absence;

import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.user.User;
import java.util.Set;
import java.util.UUID;

/**
 * Contrôle du périmètre managérial (Constitution Principe IV) : un manager n'agit que sur les
 * salariés des équipes qu'il encadre. Partagé par tous les cas d'usage de validation.
 */
public class ManagerPerimeter {

  private final TeamRepository teamRepository;
  private final AuthorizationService authorizationService;

  public ManagerPerimeter(
      TeamRepository teamRepository, AuthorizationService authorizationService) {
    this.teamRepository = teamRepository;
    this.authorizationService = authorizationService;
  }

  /** Vérifie que le manager encadre le salarié de l'absence, sans auto-validation (EF-010). */
  public Team assertCanDecideOn(User manager, Absence absence) {
    Team team = requireTeam(absence.getTeamId());
    assertManagesEmployee(manager, team.getId(), absence.getEmployeeId());
    authorizationService.assertCannotSelfApprove(absence.getEmployeeId(), manager.getId());
    return team;
  }

  /** Vérifie que le manager encadre le salarié désigné (sans notion d'auto-validation). */
  public void assertManagesEmployee(User manager, UUID teamId, UUID employeeId) {
    Set<UUID> memberIds = teamRepository.findMemberIdsByTeamId(teamId);
    authorizationService.assertIsManagerOf(manager, employeeId, memberIds);
  }

  public Team requireTeam(UUID teamId) {
    return teamRepository
        .findById(teamId)
        .orElseThrow(() -> new IllegalStateException("Équipe introuvable : " + teamId));
  }
}
