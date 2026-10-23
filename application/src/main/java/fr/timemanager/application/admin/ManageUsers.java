package fr.timemanager.application.admin;

import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.domain.user.WorkSchedule;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Use case EF-031 et EF-031b : gestion des utilisateurs (création, modification, désactivation),
 * affectation aux équipes et configuration du planning contractuel.
 */
public class ManageUsers {

  private final AdminRepository adminRepository;
  private final AuthorizationService authorizationService;

  public ManageUsers(AdminRepository adminRepository, AuthorizationService authorizationService) {
    this.adminRepository = adminRepository;
    this.authorizationService = authorizationService;
  }

  public List<User> list(User admin) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    return adminRepository.findAllUsers();
  }

  public User create(
      User admin,
      String googleSub,
      String email,
      String firstName,
      String lastName,
      Set<UserRole> roles) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    assertRolesNotEmpty(roles);
    User user =
        new User(
            UUID.randomUUID(),
            googleSub,
            email,
            firstName,
            lastName,
            true,
            null,
            false,
            roles,
            Instant.now());
    return adminRepository.saveUser(user);
  }

  public User update(
      User admin,
      UUID userId,
      String firstName,
      String lastName,
      Set<UserRole> roles,
      boolean recurrenceEnabled) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    assertRolesNotEmpty(roles);
    User existing = requireUser(userId);
    User updated =
        new User(
            existing.getId(),
            existing.getGoogleSub(),
            existing.getEmail(),
            firstName,
            lastName,
            existing.isActive(),
            existing.getNonWorkingDaysPattern(),
            recurrenceEnabled,
            roles,
            existing.getCreatedAt());
    return adminRepository.saveUser(updated);
  }

  /** EF-031 : la désactivation remplace la suppression — l'historique reste intact. */
  public User deactivate(User admin, UUID userId) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    User existing = requireUser(userId);
    User deactivated =
        new User(
            existing.getId(),
            existing.getGoogleSub(),
            existing.getEmail(),
            existing.getFirstName(),
            existing.getLastName(),
            false,
            existing.getNonWorkingDaysPattern(),
            existing.isRecurrenceEnabled(),
            existing.getRoles(),
            existing.getCreatedAt());
    return adminRepository.saveUser(deactivated);
  }

  public void assignToTeam(User admin, UUID userId, UUID teamId) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    requireUser(userId);
    adminRepository.assignUserToTeam(userId, teamId);
  }

  /** EF-031b : planning contractuel — jours travaillés récurrents avec date d'effet. */
  public WorkSchedule defineWorkSchedule(
      User admin,
      UUID userId,
      Set<DayOfWeek> workingDays,
      LocalDate effectiveFrom,
      LocalDate effectiveUntil) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    requireUser(userId);
    return adminRepository.saveWorkSchedule(
        new WorkSchedule(
            UUID.randomUUID(), userId, workingDays, effectiveFrom, effectiveUntil, Instant.now()));
  }

  public List<WorkSchedule> workSchedules(User admin, UUID userId) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    return adminRepository.findWorkSchedules(userId);
  }

  private User requireUser(UUID userId) {
    return adminRepository
        .findUserById(userId)
        .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
  }

  private void assertRolesNotEmpty(Set<UserRole> roles) {
    if (roles == null || roles.isEmpty()) {
      throw new IllegalArgumentException("Au moins un rôle est obligatoire");
    }
  }
}
