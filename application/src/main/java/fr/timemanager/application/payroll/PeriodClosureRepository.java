package fr.timemanager.application.payroll;

import fr.timemanager.domain.payroll.PeriodeCloture;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port — périodes de clôture de paie (EF-024). */
public interface PeriodClosureRepository {

  PeriodeCloture save(PeriodeCloture period);

  Optional<PeriodeCloture> findByYearAndMonth(int year, int month);

  Optional<PeriodeCloture> findById(UUID id);

  List<PeriodeCloture> findAll();

  /** Période close couvrant la date donnée, s'il en existe une (EF-025). */
  Optional<PeriodeCloture> findClosedPeriodCovering(LocalDate date);
}
