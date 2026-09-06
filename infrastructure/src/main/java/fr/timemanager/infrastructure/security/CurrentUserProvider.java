package fr.timemanager.infrastructure.security;

import fr.timemanager.domain.user.User;
import fr.timemanager.infrastructure.persistence.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Résout l'utilisateur métier courant à partir du JWT Google authentifié. */
@Component
public class CurrentUserProvider {

  private final UserRepository userRepository;

  public CurrentUserProvider(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User resolve(Jwt jwt) {
    return userRepository
        .findByGoogleSub(jwt.getSubject())
        .map(entity -> entity.toDomain())
        .orElseThrow(() -> new IllegalStateException("Utilisateur inconnu : " + jwt.getSubject()));
  }
}
