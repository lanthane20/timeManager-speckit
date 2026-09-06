package fr.timemanager.application.payroll;

import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.payroll.PeriodeCloture;
import java.time.Instant;
import java.util.UUID;

/**
 * EF-025 : détecte automatiquement qu'une absence vient d'être validée sur une période déjà
 * clôturée et enregistre la régularisation correspondante, sans action manuelle de la RH.
 */
public class RegularisationDetector {

  private final PeriodClosureRepository periodClosureRepository;
  private final RegularisationRepository regularisationRepository;

  public RegularisationDetector(
      PeriodClosureRepository periodClosureRepository,
      RegularisationRepository regularisationRepository) {
    this.periodClosureRepository = periodClosureRepository;
    this.regularisationRepository = regularisationRepository;
  }

  public void detectOnValidation(Absence absence, VersionAbsence version) {
    periodClosureRepository
        .findClosedPeriodCovering(version.getDateRange().startDate())
        .ifPresent(period -> record(period, absence, version));
  }

  private void record(PeriodeCloture period, Absence absence, VersionAbsence version) {
    regularisationRepository.save(
        new fr.timemanager.domain.payroll.Regularisation(
            UUID.randomUUID(),
            period.getId(),
            version.getId(),
            absence.getEmployeeId(),
            Instant.now(),
            "Absence validée après la clôture de "
                + period.yearMonth()
                + " ("
                + version.getWorkingDays()
                + " jours ouvrés)"));
  }
}
