package fr.timemanager.infrastructure.persistence.absence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AbsenceVersionJpaRepository extends JpaRepository<AbsenceVersionEntity, UUID> {

  @Query(
      "SELECT v FROM AbsenceVersionEntity v WHERE v.absence.employeeId = :employeeId AND v.active = true")
  List<AbsenceVersionEntity> findActiveByEmployeeId(@Param("employeeId") UUID employeeId);
}
