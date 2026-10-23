package fr.timemanager.application.dto.payroll;

import java.time.LocalDate;
import java.util.UUID;

/** EF-021 : filtres de consultation RH — tous optionnels et cumulables. */
public record HrAbsenceSearchCriteria(
    UUID employeeId, UUID teamId, UUID absenceTypeId, LocalDate from, LocalDate to) {

  public static HrAbsenceSearchCriteria none() {
    return new HrAbsenceSearchCriteria(null, null, null, null, null);
  }
}
