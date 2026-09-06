package fr.timemanager.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Constitution Principe IV et V : tout accès refusé (403) doit être journalisé comme un événement
 * d'audit ACCES_REFUSE portant l'utilisateur, la ressource et le résultat.
 */
class AccessDeniedAuditHandlerTest {

  private final AuditRepository auditRepository = mock(AuditRepository.class);
  private final AccessDeniedAuditHandler handler = new AccessDeniedAuditHandler(auditRepository);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  void refus403_persisteUnEvenementAudit_accesRefuse() throws Exception {
    String userId = UUID.randomUUID().toString();
    UUID correlationId = UUID.randomUUID();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                userId, "n/a", AuthorityUtils.createAuthorityList("ROLE_SALARIE")));
    MDC.put("correlation_id", correlationId.toString());

    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/v1/manager/absences/pending");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("refusé"));

    ArgumentCaptor<EvenementAudit> captor = ArgumentCaptor.forClass(EvenementAudit.class);
    verify(auditRepository).save(captor.capture());
    EvenementAudit audit = captor.getValue();
    assertThat(audit.actionType()).isEqualTo(AuditActionType.ACCES_REFUSE);
    assertThat(audit.authorId()).isEqualTo(userId);
    assertThat(audit.correlationId()).isEqualTo(correlationId);
    assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void refus403_sansAuthentification_journaliseUnAuteurAnonyme() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/planning");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("refusé"));

    ArgumentCaptor<EvenementAudit> captor = ArgumentCaptor.forClass(EvenementAudit.class);
    verify(auditRepository).save(captor.capture());
    assertThat(captor.getValue().authorId()).isEqualTo("anonymous");
    assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
  }
}
