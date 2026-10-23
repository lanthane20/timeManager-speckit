package fr.timemanager.infrastructure.persistence.recurrence;

import fr.timemanager.domain.recurrence.InjectionSource;
import fr.timemanager.domain.recurrence.RecurrenceInjectionLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recurrence_injection_logs")
public class RecurrenceInjectionLogEntity {

  @Id
  @Column(nullable = false, updatable = false, columnDefinition = "CHAR(36)")
  private String id;

  @Column(name = "pattern_id", nullable = false, updatable = false, columnDefinition = "CHAR(36)")
  private String patternId;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, updatable = false)
  private InjectionSource source;

  @Column(name = "injected_at", nullable = false, updatable = false)
  private Instant injectedAt;

  @Column(
      name = "injected_by_user_id",
      nullable = false,
      updatable = false,
      columnDefinition = "CHAR(36)")
  private String injectedByUserId;

  @Column(name = "events_created_count", nullable = false, updatable = false)
  private int eventsCreatedCount;

  @Column(name = "period_start", nullable = false, updatable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false, updatable = false)
  private LocalDate periodEnd;

  protected RecurrenceInjectionLogEntity() {}

  public static RecurrenceInjectionLogEntity fromDomain(RecurrenceInjectionLog log) {
    var entity = new RecurrenceInjectionLogEntity();
    entity.id = log.id().toString();
    entity.patternId = log.patternId().toString();
    entity.source = log.source();
    entity.injectedAt = log.injectedAt();
    entity.injectedByUserId = log.injectedByUserId().toString();
    entity.eventsCreatedCount = log.eventsCreatedCount();
    entity.periodStart = log.periodStart();
    entity.periodEnd = log.periodEnd();
    return entity;
  }

  public RecurrenceInjectionLog toDomain() {
    return new RecurrenceInjectionLog(
        UUID.fromString(id),
        UUID.fromString(patternId),
        source,
        injectedAt,
        UUID.fromString(injectedByUserId),
        eventsCreatedCount,
        periodStart,
        periodEnd);
  }
}
