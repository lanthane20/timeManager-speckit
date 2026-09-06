package fr.timemanager.application.dto.absence;

import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.absencetype.AbsenceType;

/** Projection commune d'une absence vers sa représentation applicative. */
public final class AbsenceResponseMapper {

  private AbsenceResponseMapper() {}

  public static AbsenceResponseDto toDto(Absence absence, AbsenceType type) {
    return toDto(absence, absence.getActiveVersion(), type);
  }

  public static AbsenceResponseDto toDto(
      Absence absence, VersionAbsence version, AbsenceType type) {
    return new AbsenceResponseDto(
        absence.getId(),
        absence.getEmployeeId(),
        absence.getAbsenceTypeId(),
        type.getLabel(),
        type.getColor(),
        version.getDateRange().startDate(),
        version.getDateRange().endDate(),
        version.getWorkingDays(),
        absence.getSimplifiedStatus(),
        absence.getCreatedAt());
  }
}
