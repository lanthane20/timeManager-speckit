package fr.timemanager.application.payroll;

import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.AbsenceResponseMapper;
import fr.timemanager.application.dto.payroll.HrAbsenceSearchCriteria;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.util.List;

/** Use case EF-021 : consultation RH de toutes les absences, filtrée. */
public class SearchAbsencesForHR {

  private final AbsenceRepository absenceRepository;
  private final AbsenceTypeRepository absenceTypeRepository;
  private final AuthorizationService authorizationService;

  public SearchAbsencesForHR(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      AuthorizationService authorizationService) {
    this.absenceRepository = absenceRepository;
    this.absenceTypeRepository = absenceTypeRepository;
    this.authorizationService = authorizationService;
  }

  public List<AbsenceResponseDto> execute(User hrUser, HrAbsenceSearchCriteria criteria) {
    authorizationService.assertHasRole(hrUser, UserRole.RH);
    return absenceRepository.search(criteria).stream().map(this::toDto).toList();
  }

  private AbsenceResponseDto toDto(Absence absence) {
    var type = absenceTypeRepository.findById(absence.getAbsenceTypeId()).orElseThrow();
    return AbsenceResponseMapper.toDto(absence, type);
  }
}
