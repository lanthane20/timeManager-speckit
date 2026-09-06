package fr.timemanager.application.absence;

import java.time.LocalDate;
import java.util.List;

/** Port — jours fériés. */
public interface PublicHolidayRepository {

  List<LocalDate> findByYear(int year);

  List<LocalDate> findBetween(LocalDate from, LocalDate to);
}
