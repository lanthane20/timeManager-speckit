package fr.timemanager.infrastructure.persistence.absence;

import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "absences")
public class AbsenceEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "employee_id", nullable = false, updatable = false)
  private UUID employeeId;

  @Column(name = "absence_type_id", nullable = false, updatable = false)
  private UUID absenceTypeId;

  @Column(name = "team_id", nullable = false, updatable = false)
  private UUID teamId;

  @Column(name = "current_version_id")
  private UUID currentVersionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "simplified_status", nullable = false)
  private AbsenceSimplifiedStatus simplifiedStatus;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @OneToMany(
      mappedBy = "absence",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<AbsenceVersionEntity> versions = new ArrayList<>();

  protected AbsenceEntity() {}

  public static AbsenceEntity fromDomain(Absence absence) {
    var entity = new AbsenceEntity();
    entity.id = absence.getId();
    entity.employeeId = absence.getEmployeeId();
    entity.absenceTypeId = absence.getAbsenceTypeId();
    entity.teamId = absence.getTeamId();
    entity.currentVersionId = absence.getCurrentVersionId();
    entity.simplifiedStatus = absence.getSimplifiedStatus();
    entity.createdAt = absence.getCreatedAt();
    entity.updatedAt = absence.getUpdatedAt();
    return entity;
  }

  public Absence toDomain() {
    var domainVersions = versions.stream().map(AbsenceVersionEntity::toDomain).toList();
    return Absence.load(
        id,
        employeeId,
        absenceTypeId,
        teamId,
        currentVersionId,
        simplifiedStatus,
        createdAt,
        updatedAt,
        domainVersions);
  }

  /** Reprend le compteur de verrouillage optimiste de la ligne déjà persistée. */
  public void keepLockVersionOf(AbsenceEntity persisted) {
    this.version = persisted.version;
  }

  public UUID getId() {
    return id;
  }

  public UUID getEmployeeId() {
    return employeeId;
  }

  public UUID getAbsenceTypeId() {
    return absenceTypeId;
  }

  public UUID getTeamId() {
    return teamId;
  }

  public UUID getCurrentVersionId() {
    return currentVersionId;
  }

  public void setCurrentVersionId(UUID currentVersionId) {
    this.currentVersionId = currentVersionId;
  }

  public AbsenceSimplifiedStatus getSimplifiedStatus() {
    return simplifiedStatus;
  }

  public void setSimplifiedStatus(AbsenceSimplifiedStatus simplifiedStatus) {
    this.simplifiedStatus = simplifiedStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<AbsenceVersionEntity> getVersions() {
    return versions;
  }
}
