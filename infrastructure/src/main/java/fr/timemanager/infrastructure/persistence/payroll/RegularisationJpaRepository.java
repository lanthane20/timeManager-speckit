package fr.timemanager.infrastructure.persistence.payroll;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegularisationJpaRepository extends JpaRepository<RegularisationEntity, String> {

  List<RegularisationEntity> findByPeriodClosureId(String periodClosureId);
}
