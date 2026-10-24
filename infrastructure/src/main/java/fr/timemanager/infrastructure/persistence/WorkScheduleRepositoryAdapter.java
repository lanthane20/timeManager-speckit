package fr.timemanager.infrastructure.persistence;

import fr.timemanager.application.absence.WorkScheduleRepository;
import fr.timemanager.domain.user.WorkSchedule;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkScheduleRepositoryAdapter implements WorkScheduleRepository {

  private final WorkScheduleJpaRepository jpaRepository;

  public WorkScheduleRepositoryAdapter(WorkScheduleJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<WorkSchedule> findByUserId(UUID userId) {
    return jpaRepository.findByUserIdOrderByEffectiveFromDesc(userId.toString()).stream()
        .map(WorkScheduleEntity::toDomain)
        .toList();
  }
}
