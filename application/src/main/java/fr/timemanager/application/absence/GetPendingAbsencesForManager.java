package fr.timemanager.application.absence;

import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.AbsenceResponseMapper;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Use case : files de validation du manager. La file de premier niveau ne contient que les demandes
 * encore EN_ATTENTE ; celle de second niveau (EF-012) les demandes déjà validées par le N+1 sur les
 * équipes en double validation.
 */
public class GetPendingAbsencesForManager {

  private final AbsenceRepository absenceRepository;
  private final AbsenceTypeRepository absenceTypeRepository;
  private final TeamRepository teamRepository;
  private final AuthorizationService authorizationService;

  public GetPendingAbsencesForManager(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      TeamRepository teamRepository,
      AuthorizationService authorizationService) {
    this.absenceRepository = absenceRepository;
    this.absenceTypeRepository = absenceTypeRepository;
    this.teamRepository = teamRepository;
    this.authorizationService = authorizationService;
  }

  /** File de premier niveau : demandes en attente de la décision du N+1. */
  public List<AbsenceResponseDto> execute(User manager) {
    return pendingFor(manager, team -> true, AbsenceVersionStatus.EN_ATTENTE);
  }

  /** EF-012 : file de second niveau, réservée au N+2 des équipes en double validation. */
  public List<AbsenceResponseDto> executeForSecondLevel(User manager) {
    return pendingFor(
        manager,
        team ->
            team.requiresDoubleValidation()
                && Objects.equals(team.getN2ManagerId(), manager.getId()),
        AbsenceVersionStatus.VALIDEE_N1);
  }

  private List<AbsenceResponseDto> pendingFor(
      User manager, Predicate<Team> teamFilter, AbsenceVersionStatus awaitedStatus) {
    authorizationService.assertHasRole(manager, UserRole.MANAGER);
    return teamRepository.findTeamsByManagerId(manager.getId()).stream()
        .filter(teamFilter)
        .map(Team::getId)
        .distinct()
        .flatMap(
            teamId ->
                absenceRepository
                    .findByTeamIdAndStatus(teamId, AbsenceSimplifiedStatus.EN_ATTENTE)
                    .stream())
        .filter(absence -> absence.getActiveVersion().getStatus() == awaitedStatus)
        // EF-010 : un manager ne traite jamais ses propres demandes.
        .filter(absence -> !absence.getEmployeeId().equals(manager.getId()))
        .map(this::toDto)
        .toList();
  }

  private AbsenceResponseDto toDto(Absence absence) {
    var type = absenceTypeRepository.findById(absence.getAbsenceTypeId()).orElseThrow();
    return AbsenceResponseMapper.toDto(absence, type);
  }
}
