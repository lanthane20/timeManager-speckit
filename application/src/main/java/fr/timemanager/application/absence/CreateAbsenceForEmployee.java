package fr.timemanager.application.absence;

import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.domain.user.User;
import java.util.UUID;

/** Use case EF-014 : saisie d'une absence par un manager pour un salarié de son périmètre. */
public class CreateAbsenceForEmployee {

  private final CreateAbsenceRequest createAbsenceRequest;
  private final TeamRepository teamRepository;
  private final ManagerPerimeter perimeter;

  public CreateAbsenceForEmployee(
      CreateAbsenceRequest createAbsenceRequest,
      TeamRepository teamRepository,
      AuthorizationService authorizationService) {
    this.createAbsenceRequest = createAbsenceRequest;
    this.teamRepository = teamRepository;
    this.perimeter = new ManagerPerimeter(teamRepository, authorizationService);
  }

  public AbsenceResponseDto execute(
      User manager, UUID employeeId, CreateAbsenceRequestDto request, String correlationId) {
    UUID teamId =
        teamRepository
            .findTeamIdByMemberId(employeeId)
            .orElseThrow(
                () -> new IllegalStateException("Aucune équipe pour le salarié " + employeeId));
    perimeter.assertManagesEmployee(manager, teamId, employeeId);
    return createAbsenceRequest.execute(employeeId, teamId, correlationId, request);
  }
}
