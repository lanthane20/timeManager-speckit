package fr.timemanager.infrastructure.persistence.absence;

import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "absence_versions")
public class AbsenceVersionEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "absence_id", nullable = false, updatable = false)
  private AbsenceEntity absence;

  @Column(name = "version_number", nullable = false, updatable = false)
  private int versionNumber;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "half_day_start")
  private HalfDayType halfDayStart;

  @Enumerated(EnumType.STRING)
  @Column(name = "half_day_end")
  private HalfDayType halfDayEnd;

  @Column(name = "working_days", nullable = false)
  private BigDecimal workingDays;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private AbsenceVersionStatus status;

  @Column(name = "refusal_reason", columnDefinition = "TEXT")
  private String refusalReason;

  @Column(name = "cancellation_reason", columnDefinition = "TEXT")
  private String cancellationReason;

  @Column(name = "submitted_by", nullable = false, updatable = false)
  private UUID submittedBy;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "period_closure_id")
  private UUID periodClosureId;

  @Column(name = "is_regularisation", nullable = false)
  private boolean regularisation;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected AbsenceVersionEntity() {}

  public static AbsenceVersionEntity fromDomain(
      VersionAbsence domain, AbsenceEntity absenceEntity) {
    var entity = new AbsenceVersionEntity();
    entity.id = domain.getId();
    entity.absence = absenceEntity;
    entity.versionNumber = domain.getVersionNumber();
    entity.startDate = domain.getDateRange().startDate();
    entity.endDate = domain.getDateRange().endDate();
    entity.halfDayStart = domain.getHalfDayStart();
    entity.halfDayEnd = domain.getHalfDayEnd();
    entity.workingDays = domain.getWorkingDays();
    entity.status = domain.getStatus();
    entity.refusalReason = domain.getRefusalReason();
    entity.cancellationReason = domain.getCancellationReason();
    entity.submittedBy = domain.getSubmittedBy();
    entity.active = domain.isActive();
    entity.periodClosureId = domain.getPeriodClosureId();
    entity.regularisation = domain.isRegularisation();
    entity.createdAt = domain.getCreatedAt();
    return entity;
  }

  public VersionAbsence toDomain() {
    return new VersionAbsence(
        id,
        absence.getId(),
        versionNumber,
        new DateRange(startDate, endDate),
        halfDayStart,
        halfDayEnd,
        workingDays,
        status,
        submittedBy,
        active,
        createdAt);
  }

  /** Reprend le compteur de verrouillage optimiste de la ligne déjà persistée. */
  public void keepLockVersionOf(AbsenceVersionEntity persisted) {
    this.version = persisted.version;
  }

  public UUID getId() {
    return id;
  }

  public AbsenceEntity getAbsence() {
    return absence;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public AbsenceVersionStatus getStatus() {
    return status;
  }

  public void setStatus(AbsenceVersionStatus status) {
    this.status = status;
  }

  public String getRefusalReason() {
    return refusalReason;
  }

  public void setRefusalReason(String refusalReason) {
    this.refusalReason = refusalReason;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public UUID getPeriodClosureId() {
    return periodClosureId;
  }

  public UUID getSubmittedBy() {
    return submittedBy;
  }

  public BigDecimal getWorkingDays() {
    return workingDays;
  }
}
