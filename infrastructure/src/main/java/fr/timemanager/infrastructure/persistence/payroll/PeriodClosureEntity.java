package fr.timemanager.infrastructure.persistence.payroll;

import fr.timemanager.domain.payroll.PeriodStatus;
import fr.timemanager.domain.payroll.PeriodeCloture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "period_closures")
public class PeriodClosureEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private String id;

  @Column(name = "year", nullable = false)
  private int year;

  @Column(name = "month", nullable = false)
  private int month;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "closed_by_user_id")
  private String closedByUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PeriodStatus status;

  protected PeriodClosureEntity() {}

  public static PeriodClosureEntity fromDomain(PeriodeCloture period) {
    var entity = new PeriodClosureEntity();
    entity.id = period.getId().toString();
    entity.year = period.getYear();
    entity.month = period.getMonth();
    entity.closedAt = period.getClosedAt();
    entity.closedByUserId =
        period.getClosedByUserId() != null ? period.getClosedByUserId().toString() : null;
    entity.status = period.getStatus();
    return entity;
  }

  public PeriodeCloture toDomain() {
    return PeriodeCloture.load(
        UUID.fromString(id),
        year,
        month,
        closedAt,
        closedByUserId != null ? UUID.fromString(closedByUserId) : null,
        status);
  }

  public String getId() {
    return id;
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public PeriodStatus getStatus() {
    return status;
  }
}
