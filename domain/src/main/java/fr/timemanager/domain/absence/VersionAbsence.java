package fr.timemanager.domain.absence;

import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDayType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class VersionAbsence {

  private final UUID id;
  private final UUID absenceId;
  private final int versionNumber;
  private final DateRange dateRange;
  private final HalfDayType halfDayStart;
  private final HalfDayType halfDayEnd;
  private final BigDecimal workingDays;
  private AbsenceVersionStatus status;
  private String refusalReason;
  private String cancellationReason;
  private final UUID submittedBy;
  private boolean active;
  private UUID periodClosureId;
  private boolean regularisation;
  private final Instant createdAt;

  public VersionAbsence(
      UUID id,
      UUID absenceId,
      int versionNumber,
      DateRange dateRange,
      HalfDayType halfDayStart,
      HalfDayType halfDayEnd,
      BigDecimal workingDays,
      AbsenceVersionStatus status,
      UUID submittedBy,
      boolean active,
      Instant createdAt) {
    if (workingDays == null || workingDays.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("workingDays doit être > 0");
    }
    this.id = id;
    this.absenceId = absenceId;
    this.versionNumber = versionNumber;
    this.dateRange = dateRange;
    this.halfDayStart = halfDayStart;
    this.halfDayEnd = halfDayEnd;
    this.workingDays = workingDays;
    this.status = status;
    this.submittedBy = submittedBy;
    this.active = active;
    this.createdAt = createdAt;
  }

  public void validate() {
    if (status != AbsenceVersionStatus.EN_ATTENTE && status != AbsenceVersionStatus.VALIDEE_N1) {
      throw new IllegalStateException("Impossible de valider depuis le statut : " + status);
    }
    this.status = AbsenceVersionStatus.VALIDEE;
  }

  public void validateN1() {
    if (status != AbsenceVersionStatus.EN_ATTENTE) {
      throw new IllegalStateException("Impossible de valider N1 depuis le statut : " + status);
    }
    this.status = AbsenceVersionStatus.VALIDEE_N1;
  }

  public void refuse(String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Le motif de refus est obligatoire");
    }
    this.status = AbsenceVersionStatus.REFUSEE;
    this.refusalReason = reason;
  }

  public void cancelByEmployee() {
    if (status != AbsenceVersionStatus.EN_ATTENTE && status != AbsenceVersionStatus.VALIDEE_N1) {
      throw new IllegalStateException("Annulation salarié impossible depuis le statut : " + status);
    }
    this.status = AbsenceVersionStatus.ANNULEE;
  }

  public void cancelByManager(String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("La justification d'annulation manager est obligatoire");
    }
    if (status != AbsenceVersionStatus.VALIDEE) {
      throw new IllegalStateException("Annulation manager impossible depuis le statut : " + status);
    }
    this.status = AbsenceVersionStatus.ANNULEE;
    this.cancellationReason = reason;
  }

  public void deactivate() {
    this.active = false;
  }

  /** EF-008 : la nouvelle version ne devient la référence qu'une fois validée. */
  public void activate() {
    this.active = true;
  }

  public boolean isUnderReview() {
    return status == AbsenceVersionStatus.EN_ATTENTE || status == AbsenceVersionStatus.VALIDEE_N1;
  }

  public UUID getId() {
    return id;
  }

  public UUID getAbsenceId() {
    return absenceId;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public DateRange getDateRange() {
    return dateRange;
  }

  public HalfDayType getHalfDayStart() {
    return halfDayStart;
  }

  public HalfDayType getHalfDayEnd() {
    return halfDayEnd;
  }

  public BigDecimal getWorkingDays() {
    return workingDays;
  }

  public AbsenceVersionStatus getStatus() {
    return status;
  }

  public String getRefusalReason() {
    return refusalReason;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public UUID getSubmittedBy() {
    return submittedBy;
  }

  public boolean isActive() {
    return active;
  }

  public UUID getPeriodClosureId() {
    return periodClosureId;
  }

  public boolean isRegularisation() {
    return regularisation;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
