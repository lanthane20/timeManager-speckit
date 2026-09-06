package fr.timemanager.api.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation OpenAPI de l'API v1 (exposée sur {@code /v3/api-docs} et {@code /swagger-ui.html}).
 * Tous les endpoints sont protégés par un JWT Google validé en Resource Server.
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI timePlanningOpenApi(
      @Value("${timemanager.app.base-url:http://localhost:8080}") String baseUrl) {
    return new OpenAPI()
        .info(
            new Info()
                .title("TimePlanning — API de gestion des absences")
                .version("v1")
                .description(
                    """
                    API interne de gestion des absences et du planning d'équipe.
                    Rôles : Salarié, Manager, RH, Admin technique.
                    Authentification : Google OAuth2 / OIDC, jeton JWT en en-tête Authorization.
                    """)
                .contact(new Contact().name("Équipe TimePlanning"))
                .license(new License().name("Usage interne")))
        .servers(List.of(new Server().url(baseUrl).description("Instance courante")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Jeton d'accès Google OIDC")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }

  /** Regroupe les endpoints par domaine fonctionnel pour la lecture dans Swagger UI. */
  @Bean
  public GroupedOpenApi absencesApi() {
    return GroupedOpenApi.builder()
        .group("1-absences")
        .pathsToMatch("/api/v1/absences/**", "/api/v1/absence-types/**")
        .build();
  }

  @Bean
  public GroupedOpenApi managerApi() {
    return GroupedOpenApi.builder()
        .group("2-manager")
        .pathsToMatch("/api/v1/manager/**", "/api/v1/planning/**")
        .build();
  }

  @Bean
  public GroupedOpenApi payrollApi() {
    return GroupedOpenApi.builder().group("3-rh").pathsToMatch("/api/v1/rh/**").build();
  }

  @Bean
  public GroupedOpenApi recurrenceApi() {
    return GroupedOpenApi.builder()
        .group("4-recurrences")
        .pathsToMatch("/api/v1/recurrences/**")
        .build();
  }

  @Bean
  public GroupedOpenApi adminApi() {
    return GroupedOpenApi.builder().group("5-admin").pathsToMatch("/api/v1/admin/**").build();
  }
}
