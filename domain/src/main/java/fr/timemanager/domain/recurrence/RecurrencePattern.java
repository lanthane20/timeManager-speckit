package fr.timemanager.domain.recurrence;

import java.time.Instant;
import java.util.UUID;

/**
 * EF-028 : patron de récurrence d'un salarié — support des absences Ecole et des jours non
 * travaillés contractuels. Un patron désactivé ne produit plus d'injection.
 */
public class RecurrencePattern {

  private final UUID id;
  private final UUID employeeId;
  private final UUID absenceTypeId;
  private final String description;
  private boolean active;
  private final Instant createdAt;

  public RecurrencePattern(
      UUID id,
      UUID employeeId,
      UUID absenceTypeId,
      String description,
      boolean active,
      Instant createdAt) {
    if (employeeId == null) {
      throw new IllegalArgumentException("employeeId obligatoire");
    }
    if (absenceTypeId == null) {
      throw new IllegalArgumentException("absenceTypeId obligatoire");
    }
    this.id = id;
    this.employeeId = employeeId;
    this.absenceTypeId = absenceTypeId;
    this.description = description;
    this.active = active;
    this.createdAt = createdAt;
  }

  public void deactivate() {
    this.active = false;
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

  public String getDescription() {
    return description;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
