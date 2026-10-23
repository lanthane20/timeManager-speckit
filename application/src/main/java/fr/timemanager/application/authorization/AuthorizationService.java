package fr.timemanager.application.authorization;

import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.util.UUID;

/**
 * Centralise toute la logique d'autorisation (Constitution Principe IV). Jamais inline dans les
 * contrôleurs.
 */
public class AuthorizationService {

  public void assertCannotSelfApprove(UUID requesterId, UUID approverId) {
    if (requesterId.equals(approverId)) {
      throw new SelfApprovalException(
          "Un utilisateur ne peut pas valider sa propre demande : " + requesterId);
    }
  }

  public void assertHasRole(User user, UserRole requiredRole) {
    if (!user.hasRole(requiredRole)) {
      throw new UnauthorizedException(user.getId() + " n'a pas le rôle requis : " + requiredRole);
    }
  }

  public void assertIsManagerOf(User manager, UUID employeeId, java.util.Set<UUID> teamMemberIds) {
    if (!manager.hasRole(UserRole.MANAGER)) {
      throw new UnauthorizedException(manager.getId() + " n'est pas manager");
    }
    if (!teamMemberIds.contains(employeeId)) {
      throw new UnauthorizedException(
          manager.getId() + " n'est pas manager de l'employé " + employeeId);
    }
  }

  /** EF-012 : seul le N+2 déclaré sur l'équipe peut valider au second niveau. */
  public void assertIsN2ManagerOf(User manager, UUID n2ManagerId) {
    if (!manager.hasRole(UserRole.MANAGER) || !manager.getId().equals(n2ManagerId)) {
      throw new UnauthorizedException(manager.getId() + " n'est pas le N+2 de l'équipe concernée");
    }
  }

  public void assertAdminHasNoAbsenceDataAccess(User user) {
    if (user.hasRole(UserRole.ADMIN) && !user.hasRole(UserRole.RH)) {
      throw new UnauthorizedException("L'admin technique n'a pas accès aux données d'absence");
    }
  }

  public void assertAdminCannotAccessCounters(User user) {
    if (user.hasRole(UserRole.ADMIN) && !user.hasRole(UserRole.RH)) {
      throw new UnauthorizedException("L'admin technique n'a pas accès aux compteurs d'absence");
    }
  }
}
