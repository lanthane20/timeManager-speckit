package fr.timemanager.infrastructure.persistence.payroll;

import fr.timemanager.domain.payroll.PeriodStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodClosureJpaRepository extends JpaRepository<PeriodClosureEntity, String> {

  Optional<PeriodClosureEntity> findByYearAndMonth(int year, int month);

  List<PeriodClosureEntity> findByYearAndStatus(int year, PeriodStatus status);
}
