package fr.timemanager.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkScheduleJpaRepository extends JpaRepository<WorkScheduleEntity, String> {

  List<WorkScheduleEntity> findByUserIdOrderByEffectiveFromDesc(String userId);
}
