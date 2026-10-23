package fr.timemanager.api.user;

import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profil de l'utilisateur connecté. Les rôles applicatifs sont en base, pas dans le jeton du
 * fournisseur d'identité : le frontend les lit ici pour adapter la navigation et ses guards.
 */
@RestController
@RequestMapping("/api/v1/me")
public class CurrentUserController {

  private final CurrentUserProvider currentUserProvider;

  public CurrentUserController(CurrentUserProvider currentUserProvider) {
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public CurrentUserDto me(JwtAuthenticationToken authentication) {
    User user = currentUserProvider.resolve(authentication.getToken());
    return new CurrentUserDto(
        user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRoles());
  }

  public record CurrentUserDto(
      UUID id, String firstName, String lastName, String email, Set<UserRole> roles) {}
}
