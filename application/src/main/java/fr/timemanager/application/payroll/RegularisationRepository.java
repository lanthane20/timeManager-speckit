package fr.timemanager.application.payroll;

import fr.timemanager.domain.payroll.Regularisation;
import java.util.List;
import java.util.UUID;

/** Port — régularisations détectées après clôture (EF-025, EF-026). */
public interface RegularisationRepository {

  Regularisation save(Regularisation regularisation);

  List<Regularisation> findByPeriodClosureId(UUID periodClosureId);

  List<Regularisation> findAll();
}
