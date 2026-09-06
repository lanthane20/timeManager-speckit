package fr.timemanager.infrastructure.persistence.counter;

import fr.timemanager.application.counter.AbsenceCounterEntry;
import fr.timemanager.application.counter.CounterRepository;
import fr.timemanager.infrastructure.persistence.absence.AbsenceVersionEntity;
import fr.timemanager.infrastructure.persistence.absence.AbsenceVersionJpaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter EF-018 : lit les versions d'absence actives d'un salarié pour le calcul des compteurs.
 */
@Component
public class CounterRepositoryAdapter implements CounterRepository {

  private final AbsenceVersionJpaRepository versionJpaRepository;

  public CounterRepositoryAdapter(AbsenceVersionJpaRepository versionJpaRepository) {
    this.versionJpaRepository = versionJpaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AbsenceCounterEntry> findCounterEntries(UUID employeeId) {
    return versionJpaRepository.findActiveByEmployeeId(employeeId).stream()
        .map(CounterRepositoryAdapter::toEntry)
        .toList();
  }

  private static AbsenceCounterEntry toEntry(AbsenceVersionEntity version) {
    return new AbsenceCounterEntry(
        version.getAbsence().getAbsenceTypeId(),
        version.getStartDate(),
        version.getEndDate(),
        version.getWorkingDays(),
        version.getStatus());
  }
}
