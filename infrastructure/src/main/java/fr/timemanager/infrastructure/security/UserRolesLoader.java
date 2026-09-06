package fr.timemanager.infrastructure.security;

import fr.timemanager.infrastructure.persistence.UserRepository;
import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/** Charge les rôles applicatifs depuis la base à partir du sub Google. */
@Component
public class UserRolesLoader {

  private final UserRepository userRepository;

  public UserRolesLoader(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Collection<GrantedAuthority> loadAuthorities(String googleSub) {
    return userRepository
        .findByGoogleSub(googleSub)
        .map(
            user ->
                user.getRoles().stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList())
        .orElse(Collections.emptyList());
  }
}
