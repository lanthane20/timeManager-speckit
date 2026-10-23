package fr.timemanager.application.admin;

import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.team.ValidationMode;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.util.List;
import java.util.UUID;

/**
 * Use case EF-032 et EF-033 : gestion des équipes et sous-équipes, hiérarchie N+1 / N+2 et mode de
 * validation propre à chaque équipe.
 */
public class ManageTeams {

  private final AdminRepository adminRepository;
  private final TeamRepository teamRepository;
  private final AuthorizationService authorizationService;

  public ManageTeams(
      AdminRepository adminRepository,
      TeamRepository teamRepository,
      AuthorizationService authorizationService) {
    this.adminRepository = adminRepository;
    this.teamRepository = teamRepository;
    this.authorizationService = authorizationService;
  }

  public List<Team> list(User admin) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    return adminRepository.findAllTeams();
  }

  public Team create(
      User admin,
      String name,
      UUID parentTeamId,
      ValidationMode validationMode,
      UUID n1ManagerId,
      UUID n2ManagerId) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    assertHierarchy(validationMode, n1ManagerId, n2ManagerId);
    return adminRepository.saveTeam(
        new Team(
            UUID.randomUUID(), name, parentTeamId, validationMode, n1ManagerId, n2ManagerId, true));
  }

  public Team update(
      User admin,
      UUID teamId,
      String name,
      UUID parentTeamId,
      ValidationMode validationMode,
      UUID n1ManagerId,
      UUID n2ManagerId) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    assertHierarchy(validationMode, n1ManagerId, n2ManagerId);
    Team existing = requireTeam(teamId);
    return adminRepository.saveTeam(
        new Team(
            existing.getId(),
            name,
            parentTeamId,
            validationMode,
            n1ManagerId,
            n2ManagerId,
            existing.isActive()));
  }

  public Team deactivate(User admin, UUID teamId) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    Team existing = requireTeam(teamId);
    return adminRepository.saveTeam(
        new Team(
            existing.getId(),
            existing.getName(),
            existing.getParentTeamId(),
            existing.getValidationMode(),
            existing.getN1ManagerId(),
            existing.getN2ManagerId(),
            false));
  }

  /** EF-033 : une équipe en double validation doit disposer d'un N+2 identifié. */
  private void assertHierarchy(ValidationMode validationMode, UUID n1ManagerId, UUID n2ManagerId) {
    if (n1ManagerId == null) {
      throw new IllegalArgumentException("Un N+1 est obligatoire pour chaque équipe");
    }
    if (validationMode == ValidationMode.N1_AND_N2 && n2ManagerId == null) {
      throw new IllegalArgumentException(
          "Le mode N+1 puis N+2 exige un N+2 identifié sur l'équipe");
    }
    if (n1ManagerId.equals(n2ManagerId)) {
      throw new IllegalArgumentException("Le N+1 et le N+2 doivent être deux managers distincts");
    }
  }

  private Team requireTeam(UUID teamId) {
    return teamRepository
        .findById(teamId)
        .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable : " + teamId));
  }
}
