package fr.timemanager.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicHolidayJpaRepository extends JpaRepository<PublicHolidayEntity, LocalDate> {

  List<PublicHolidayEntity> findByYear(int year);

  List<PublicHolidayEntity> findByHolidayDateBetween(LocalDate from, LocalDate to);
}
