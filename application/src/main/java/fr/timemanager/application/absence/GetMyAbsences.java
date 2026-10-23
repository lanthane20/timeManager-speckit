package fr.timemanager.application.absence;

import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.AbsenceResponseMapper;
import fr.timemanager.domain.absence.Absence;
import java.util.List;
import java.util.UUID;

/** Use case : liste des absences du salarié connecté. */
public class GetMyAbsences {

  private final AbsenceRepository absenceRepository;
  private final AbsenceTypeRepository absenceTypeRepository;

  public GetMyAbsences(
      AbsenceRepository absenceRepository, AbsenceTypeRepository absenceTypeRepository) {
    this.absenceRepository = absenceRepository;
    this.absenceTypeRepository = absenceTypeRepository;
  }

  public List<AbsenceResponseDto> execute(UUID employeeId) {
    return absenceRepository.findByEmployeeId(employeeId).stream()
        .map(absence -> toResponse(absence))
        .toList();
  }

  private AbsenceResponseDto toResponse(Absence absence) {
    var type = absenceTypeRepository.findById(absence.getAbsenceTypeId()).orElseThrow();
    return AbsenceResponseMapper.toDto(absence, type);
  }
}
