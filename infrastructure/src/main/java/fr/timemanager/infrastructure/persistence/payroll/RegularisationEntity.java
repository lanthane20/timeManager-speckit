package fr.timemanager.infrastructure.persistence.payroll;

import fr.timemanager.domain.payroll.Regularisation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "regularisations")
public class RegularisationEntity {

  @Id
  @Column(nullable = false, updatable = false, columnDefinition = "CHAR(36)")
  private String id;

  @Column(
      name = "period_closure_id",
      nullable = false,
      updatable = false,
      columnDefinition = "CHAR(36)")
  private String periodClosureId;

  @Column(
      name = "absence_version_id",
      nullable = false,
      updatable = false,
      columnDefinition = "CHAR(36)")
  private String absenceVersionId;

  @Column(name = "employee_id", nullable = false, updatable = false, columnDefinition = "CHAR(36)")
  private String employeeId;

  @Column(name = "detected_at", nullable = false, updatable = false)
  private Instant detectedAt;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  protected RegularisationEntity() {}

  public static RegularisationEntity fromDomain(Regularisation regularisation) {
    var entity = new RegularisationEntity();
    entity.id = regularisation.id().toString();
    entity.periodClosureId = regularisation.periodClosureId().toString();
    entity.absenceVersionId = regularisation.absenceVersionId().toString();
    entity.employeeId = regularisation.employeeId().toString();
    entity.detectedAt = regularisation.detectedAt();
    entity.description = regularisation.description();
    return entity;
  }

  public Regularisation toDomain() {
    return new Regularisation(
        UUID.fromString(id),
        UUID.fromString(periodClosureId),
        UUID.fromString(absenceVersionId),
        UUID.fromString(employeeId),
        detectedAt,
        description);
  }
}
