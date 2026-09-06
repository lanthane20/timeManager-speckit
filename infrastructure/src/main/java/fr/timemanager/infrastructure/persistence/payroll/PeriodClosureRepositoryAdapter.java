package fr.timemanager.infrastructure.persistence.payroll;

import fr.timemanager.domain.payroll.PeriodStatus;
import fr.timemanager.domain.payroll.PeriodeCloture;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PeriodClosureRepositoryAdapter
    implements fr.timemanager.application.payroll.PeriodClosureRepository {

  private final PeriodClosureJpaRepository jpaRepository;

  public PeriodClosureRepositoryAdapter(PeriodClosureJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  @Transactional
  public PeriodeCloture save(PeriodeCloture period) {
    return jpaRepository.save(PeriodClosureEntity.fromDomain(period)).toDomain();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<PeriodeCloture> findByYearAndMonth(int year, int month) {
    return jpaRepository.findByYearAndMonth(year, month).map(PeriodClosureEntity::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<PeriodeCloture> findById(UUID id) {
    return jpaRepository.findById(id.toString()).map(PeriodClosureEntity::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PeriodeCloture> findAll() {
    return jpaRepository.findAll().stream().map(PeriodClosureEntity::toDomain).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<PeriodeCloture> findClosedPeriodCovering(LocalDate date) {
    return jpaRepository
        .findByYearAndMonth(date.getYear(), date.getMonthValue())
        .map(PeriodClosureEntity::toDomain)
        .filter(period -> period.getStatus() == PeriodStatus.CLOSED);
  }
}
