package fr.timemanager.infrastructure.persistence;

import fr.timemanager.application.absence.PublicHolidayRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublicHolidayRepositoryAdapter implements PublicHolidayRepository {

  private final PublicHolidayJpaRepository jpaRepository;

  public PublicHolidayRepositoryAdapter(PublicHolidayJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public List<LocalDate> findByYear(int year) {
    return jpaRepository.findByYear(year).stream()
        .map(PublicHolidayEntity::getHolidayDate)
        .toList();
  }

  @Override
  public List<LocalDate> findBetween(LocalDate from, LocalDate to) {
    return jpaRepository.findByHolidayDateBetween(from, to).stream()
        .map(PublicHolidayEntity::getHolidayDate)
        .toList();
  }
}
