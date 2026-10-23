package fr.timemanager.infrastructure.persistence.payroll;

import fr.timemanager.domain.payroll.Regularisation;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RegularisationRepositoryAdapter
    implements fr.timemanager.application.payroll.RegularisationRepository {

  private final RegularisationJpaRepository jpaRepository;

  public RegularisationRepositoryAdapter(RegularisationJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  @Transactional
  public Regularisation save(Regularisation regularisation) {
    return jpaRepository.save(RegularisationEntity.fromDomain(regularisation)).toDomain();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Regularisation> findByPeriodClosureId(UUID periodClosureId) {
    return jpaRepository.findByPeriodClosureId(periodClosureId.toString()).stream()
        .map(RegularisationEntity::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Regularisation> findAll() {
    return jpaRepository.findAll().stream().map(RegularisationEntity::toDomain).toList();
  }
}
