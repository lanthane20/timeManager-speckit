package fr.timemanager.domain.payroll;

import java.time.Instant;
import java.util.UUID;

/**
 * EF-025 : trace immuable d'une absence validée après la clôture de sa période de paie. Créée
 * automatiquement, sans action manuelle de la RH.
 */
public record Regularisation(
    UUID id,
    UUID periodClosureId,
    UUID absenceVersionId,
    UUID employeeId,
    Instant detectedAt,
    String description) {

  public Regularisation {
    if (id == null) {
      throw new IllegalArgumentException("id obligatoire");
    }
    if (periodClosureId == null) {
      throw new IllegalArgumentException("periodClosureId obligatoire");
    }
    if (absenceVersionId == null) {
      throw new IllegalArgumentException("absenceVersionId obligatoire");
    }
    if (employeeId == null) {
      throw new IllegalArgumentException("employeeId obligatoire");
    }
    if (detectedAt == null) {
      throw new IllegalArgumentException("detectedAt obligatoire");
    }
  }
}
