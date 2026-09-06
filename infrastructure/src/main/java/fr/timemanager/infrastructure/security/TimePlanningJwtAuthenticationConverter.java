package fr.timemanager.infrastructure.security;

import java.util.Collection;
import java.util.Collections;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * Extrait le sub Google et charge les rôles applicatifs depuis la base de données. Les rôles ne
 * sont pas dans le token Google — ils sont dans la table user_roles.
 */
@Component
public class TimePlanningJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private final UserRolesLoader userRolesLoader;

  public TimePlanningJwtAuthenticationConverter(UserRolesLoader userRolesLoader) {
    this.userRolesLoader = userRolesLoader;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String googleSub = jwt.getSubject();
    Collection<GrantedAuthority> authorities = loadAuthorities(googleSub);

    JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
    delegate.setJwtGrantedAuthoritiesConverter(j -> authorities);
    return delegate.convert(jwt);
  }

  private Collection<GrantedAuthority> loadAuthorities(String googleSub) {
    try {
      return userRolesLoader.loadAuthorities(googleSub);
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
