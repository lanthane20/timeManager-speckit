package fr.timemanager.infrastructure.persistence;

import fr.timemanager.domain.user.WorkSchedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "work_schedules")
public class WorkScheduleEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private String id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private String userId;

  @Column(name = "working_days_pattern", nullable = false)
  private String workingDaysPattern;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_until")
  private LocalDate effectiveUntil;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected WorkScheduleEntity() {}

  public static WorkScheduleEntity fromDomain(WorkSchedule schedule) {
    var entity = new WorkScheduleEntity();
    entity.id = schedule.id().toString();
    entity.userId = schedule.userId().toString();
    entity.workingDaysPattern = schedule.toPattern();
    entity.effectiveFrom = schedule.effectiveFrom();
    entity.effectiveUntil = schedule.effectiveUntil();
    entity.createdAt = schedule.createdAt();
    return entity;
  }

  public WorkSchedule toDomain() {
    return new WorkSchedule(
        UUID.fromString(id),
        UUID.fromString(userId),
        WorkSchedule.parsePattern(workingDaysPattern),
        effectiveFrom,
        effectiveUntil,
        createdAt);
  }
}
