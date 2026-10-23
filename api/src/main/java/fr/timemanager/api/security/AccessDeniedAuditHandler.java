package fr.timemanager.api.security;

import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Persiste un événement d'audit ACCES_REFUSE sur chaque 403. */
@Component
public class AccessDeniedAuditHandler implements AccessDeniedHandler {

  private final AuditRepository auditRepository;

  public AccessDeniedAuditHandler(AuditRepository auditRepository) {
    this.auditRepository = auditRepository;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    String userId = extractUserId();
    String correlationId = MDC.get("correlation_id");
    String resource = request.getRequestURI();

    EvenementAudit audit =
        new EvenementAudit(
            UUID.randomUUID(),
            null,
            null,
            AuditActionType.ACCES_REFUSE,
            userId,
            "UNKNOWN",
            null,
            null,
            correlationId != null ? UUID.fromString(correlationId) : UUID.randomUUID(),
            Instant.now());
    auditRepository.save(audit);

    response.sendError(HttpStatus.FORBIDDEN.value(), "Accès refusé");
  }

  private String extractUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null ? auth.getName() : "anonymous";
  }
}
