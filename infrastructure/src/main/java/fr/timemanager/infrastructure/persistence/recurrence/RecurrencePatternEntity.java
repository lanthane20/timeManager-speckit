package fr.timemanager.infrastructure.persistence.recurrence;

import fr.timemanager.domain.recurrence.RecurrencePattern;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recurrence_patterns")
public class RecurrencePatternEntity {

  @Id
  @Column(nullable = false, updatable = false, columnDefinition = "CHAR(36)")
  private String id;

  @Column(name = "employee_id", nullable = false, updatable = false, columnDefinition = "CHAR(36)")
  private String employeeId;

  @Column(
      name = "absence_type_id",
      nullable = false,
      updatable = false,
      columnDefinition = "CHAR(36)")
  private String absenceTypeId;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected RecurrencePatternEntity() {}

  public static RecurrencePatternEntity fromDomain(RecurrencePattern pattern) {
    var entity = new RecurrencePatternEntity();
    entity.id = pattern.getId().toString();
    entity.employeeId = pattern.getEmployeeId().toString();
    entity.absenceTypeId = pattern.getAbsenceTypeId().toString();
    entity.description = pattern.getDescription();
    entity.active = pattern.isActive();
    entity.createdAt = pattern.getCreatedAt();
    return entity;
  }

  public RecurrencePattern toDomain() {
    return new RecurrencePattern(
        UUID.fromString(id),
        UUID.fromString(employeeId),
        UUID.fromString(absenceTypeId),
        description,
        active,
        createdAt);
  }
}
