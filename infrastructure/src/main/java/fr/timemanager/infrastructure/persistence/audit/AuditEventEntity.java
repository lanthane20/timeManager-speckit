package fr.timemanager.infrastructure.persistence.audit;

import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

  @Id
  @Column(columnDefinition = "CHAR(36)")
  private String id;

  @Column(name = "absence_id", columnDefinition = "CHAR(36)")
  private String absenceId;

  @Column(name = "absence_version_id", columnDefinition = "CHAR(36)")
  private String absenceVersionId;

  @Column(name = "action_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private AuditActionType actionType;

  @Column(name = "author_id", nullable = false)
  private String authorId;

  @Column(name = "author_role", nullable = false)
  private String authorRole;

  @Column(name = "value_before", columnDefinition = "TEXT")
  private String valueBefore;

  @Column(name = "value_after", columnDefinition = "TEXT")
  private String valueAfter;

  @Column(name = "correlation_id", nullable = false, columnDefinition = "CHAR(36)")
  private String correlationId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  public static AuditEventEntity fromDomain(EvenementAudit audit) {
    var entity = new AuditEventEntity();
    entity.id = audit.id().toString();
    entity.absenceId = audit.absenceId() != null ? audit.absenceId().toString() : null;
    entity.absenceVersionId =
        audit.absenceVersionId() != null ? audit.absenceVersionId().toString() : null;
    entity.actionType = audit.actionType();
    entity.authorId = audit.authorId();
    entity.authorRole = audit.authorRole();
    entity.valueBefore = audit.valueBefore();
    entity.valueAfter = audit.valueAfter();
    entity.correlationId = audit.correlationId().toString();
    entity.occurredAt = audit.occurredAt();
    return entity;
  }

  public EvenementAudit toDomain() {
    return new EvenementAudit(
        UUID.fromString(id),
        absenceId != null ? UUID.fromString(absenceId) : null,
        absenceVersionId != null ? UUID.fromString(absenceVersionId) : null,
        actionType,
        authorId,
        authorRole,
        valueBefore,
        valueAfter,
        UUID.fromString(correlationId),
        occurredAt);
  }
}
