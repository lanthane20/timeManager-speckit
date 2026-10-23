package fr.timemanager.application.absence;

import fr.timemanager.application.dto.payroll.HrAbsenceSearchCriteria;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.ExistingAbsenceInfo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port — persistance des absences. */
public interface AbsenceRepository {

  Absence save(Absence absence);

  Optional<Absence> findById(UUID id);

  List<Absence> findByEmployeeId(UUID employeeId);

  List<Absence> findByTeamIdAndStatus(UUID teamId, AbsenceSimplifiedStatus status);

  List<ExistingAbsenceInfo> findOverlappingInfo(
      UUID employeeId, java.time.LocalDate start, java.time.LocalDate end);

  /** EF-021 : consultation RH, tous filtres optionnels et cumulables. */
  List<Absence> search(HrAbsenceSearchCriteria criteria);

  /** EF-027 : absences validées dont la période chevauche l'année demandée. */
  List<Absence> findValidatedInYear(int year);
}
