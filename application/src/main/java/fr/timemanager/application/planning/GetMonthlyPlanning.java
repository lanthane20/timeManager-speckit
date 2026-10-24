package fr.timemanager.application.planning;

import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.dto.planning.PlanningEntryDto;
import fr.timemanager.application.user.UserRepository;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case EF-015 à EF-017 : planning mensuel de l'équipe. Le mois courant est utilisé par défaut
 * (EF-016) et le filtrage par équipe respecte le périmètre de l'utilisateur connecté (EF-017).
 */
public class GetMonthlyPlanning {

  /** Statuts visibles sur le planning : les demandes annulées ou refusées n'y figurent pas. */
  private static final Set<AbsenceSimplifiedStatus> VISIBLE_STATUSES =
      Set.of(AbsenceSimplifiedStatus.EN_ATTENTE, AbsenceSimplifiedStatus.VALIDEE);

  private final AbsenceRepository absenceRepository;
  private final AbsenceTypeRepository absenceTypeRepository;
  private final TeamRepository teamRepository;
  private final UserRepository userRepository;
  private final AuthorizationService authorizationService;

  public GetMonthlyPlanning(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      TeamRepository teamRepository,
      UserRepository userRepository,
      AuthorizationService authorizationService) {
    this.absenceRepository = absenceRepository;
    this.absenceTypeRepository = absenceTypeRepository;
    this.teamRepository = teamRepository;
    this.userRepository = userRepository;
    this.authorizationService = authorizationService;
  }

  /**
   * @param month mois demandé ; {@code null} = mois courant (EF-016)
   * @param teamIdFilter équipe demandée ; {@code null} = toutes les équipes du périmètre (EF-017)
   */
  public List<PlanningEntryDto> execute(User viewer, YearMonth month, UUID teamIdFilter) {
    authorizationService.assertAdminHasNoAbsenceDataAccess(viewer);
    YearMonth targetMonth = month != null ? month : YearMonth.now();
    List<UUID> teamIds = resolveTeamIds(viewer, teamIdFilter);

    List<Absence> absences =
        teamIds.stream()
            .flatMap(teamId -> VISIBLE_STATUSES.stream().map(status -> Map.entry(teamId, status)))
            .flatMap(
                entry ->
                    absenceRepository
                        .findByTeamIdAndStatus(entry.getKey(), entry.getValue())
                        .stream())
            .filter(absence -> overlapsMonth(absence, targetMonth))
            .toList();

    Map<UUID, String> employeeNames = loadEmployeeNames(absences);
    return absences.stream().map(absence -> toEntry(absence, employeeNames)).toList();
  }

  /**
   * EF-017 : équipes dont l'utilisateur peut consulter le planning — toutes les équipes actives
   * pour la RH, sinon les équipes qu'il manage (N+1 ou N+2) et toutes leurs sous-équipes.
   */
  public List<Team> visibleTeams(User viewer) {
    authorizationService.assertAdminHasNoAbsenceDataAccess(viewer);
    List<Team> activeTeams = teamRepository.findAllActive();
    if (viewer.hasRole(UserRole.RH)) {
      return activeTeams;
    }
    Set<UUID> managedTeamIds =
        teamRepository.findTeamsByManagerId(viewer.getId()).stream()
            .map(Team::getId)
            .collect(Collectors.toSet());
    return withSubTeams(activeTeams, managedTeamIds);
  }

  /** Sans filtre, tout le périmètre ; avec un filtre, l'équipe choisie et ses sous-équipes. */
  private List<UUID> resolveTeamIds(User viewer, UUID teamIdFilter) {
    List<Team> visibleTeams = visibleTeams(viewer);
    if (teamIdFilter == null) {
      return visibleTeams.stream().map(Team::getId).toList();
    }
    if (visibleTeams.stream().noneMatch(team -> team.getId().equals(teamIdFilter))) {
      throw new UnauthorizedException(
          viewer.getId() + " n'a pas accès au planning de l'équipe " + teamIdFilter);
    }
    return withSubTeams(visibleTeams, Set.of(teamIdFilter)).stream().map(Team::getId).toList();
  }

  /** Équipes racines et leurs descendantes, dans l'ordre de {@code teams}. */
  private static List<Team> withSubTeams(List<Team> teams, Set<UUID> rootIds) {
    Set<UUID> included = new HashSet<>(rootIds);
    int previousSize;
    do {
      previousSize = included.size();
      for (Team team : teams) {
        if (team.getParentTeamId() != null && included.contains(team.getParentTeamId())) {
          included.add(team.getId());
        }
      }
    } while (included.size() > previousSize);
    return teams.stream().filter(team -> included.contains(team.getId())).toList();
  }

  private boolean overlapsMonth(Absence absence, YearMonth month) {
    DateRange range = absence.getActiveVersion().getDateRange();
    LocalDate firstDay = month.atDay(1);
    LocalDate lastDay = month.atEndOfMonth();
    return !range.endDate().isBefore(firstDay) && !range.startDate().isAfter(lastDay);
  }

  private Map<UUID, String> loadEmployeeNames(List<Absence> absences) {
    Set<UUID> employeeIds =
        absences.stream().map(Absence::getEmployeeId).collect(Collectors.toSet());
    return userRepository.findAllByIds(employeeIds).stream()
        .collect(
            Collectors.toMap(
                User::getId, user -> user.getFirstName() + " " + user.getLastName(), (a, b) -> a));
  }

  private PlanningEntryDto toEntry(Absence absence, Map<UUID, String> employeeNames) {
    VersionAbsence version = absence.getActiveVersion();
    var type =
        absenceTypeRepository
            .findById(absence.getAbsenceTypeId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Type d'absence introuvable : " + absence.getAbsenceTypeId()));
    return new PlanningEntryDto(
        absence.getId(),
        absence.getEmployeeId(),
        employeeNames.getOrDefault(absence.getEmployeeId(), "Salarié inconnu"),
        absence.getTeamId(),
        type.getCode(),
        type.getLabel(),
        type.getColor(),
        version.getDateRange().startDate(),
        version.getDateRange().endDate(),
        absence.getSimplifiedStatus());
  }
}
