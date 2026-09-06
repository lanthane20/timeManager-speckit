package fr.timemanager.domain.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace immuable de chaque action métier. Jamais modifiée après création (Constitution Principe V).
 */
public record EvenementAudit(
    UUID id,
    UUID absenceId,
    UUID absenceVersionId,
    AuditActionType actionType,
    String authorId,
    String authorRole,
    String valueBefore,
    String valueAfter,
    UUID correlationId,
    Instant occurredAt) {

  public EvenementAudit {
    if (id == null) {
      throw new IllegalArgumentException("id obligatoire");
    }
    if (actionType == null) {
      throw new IllegalArgumentException("actionType obligatoire");
    }
    if (authorId == null || authorId.isBlank()) {
      throw new IllegalArgumentException("authorId obligatoire");
    }
    if (correlationId == null) {
      throw new IllegalArgumentException("correlationId obligatoire");
    }
    if (occurredAt == null) {
      throw new IllegalArgumentException("occurredAt obligatoire");
    }
  }
}
