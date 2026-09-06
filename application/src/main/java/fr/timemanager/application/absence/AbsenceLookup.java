package fr.timemanager.application.absence;

import fr.timemanager.domain.absence.Absence;
import java.util.UUID;

/** Chargement d'une absence existante, avec message d'erreur homogène. */
final class AbsenceLookup {

  private AbsenceLookup() {}

  static Absence require(AbsenceRepository absenceRepository, UUID absenceId) {
    return absenceRepository
        .findById(absenceId)
        .orElseThrow(() -> new IllegalArgumentException("Absence introuvable : " + absenceId));
  }
}
