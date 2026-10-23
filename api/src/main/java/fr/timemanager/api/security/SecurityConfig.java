package fr.timemanager.api.security;

import static org.springframework.security.config.Customizer.withDefaults;

import fr.timemanager.infrastructure.security.TimePlanningJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final TimePlanningJwtAuthenticationConverter jwtAuthenticationConverter;
  private final CorrelationIdFilter correlationIdFilter;
  private final AccessDeniedAuditHandler accessDeniedAuditHandler;

  public SecurityConfig(
      TimePlanningJwtAuthenticationConverter jwtAuthenticationConverter,
      CorrelationIdFilter correlationIdFilter,
      AccessDeniedAuditHandler accessDeniedAuditHandler) {
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    this.correlationIdFilter = correlationIdFilter;
    this.accessDeniedAuditHandler = accessDeniedAuditHandler;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .addFilterBefore(correlationIdFilter, BasicAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/actuator/health")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .accessDeniedHandler(accessDeniedAuditHandler))
        .cors(withDefaults());
    return http.build();
  }
}
