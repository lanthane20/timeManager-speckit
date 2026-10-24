package fr.timemanager.application.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.user.UserRepository;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.team.ValidationMode;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** EF-017 : périmètre du planning — équipes managées et leurs sous-équipes, tout pour la RH. */
class GetMonthlyPlanningTest {

  private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
  private final TeamRepository teamRepository = mock(TeamRepository.class);

  private final User manager = user(UserRole.SALARIE, UserRole.MANAGER);
  private final User hr = user(UserRole.SALARIE, UserRole.RH);

  // Hiérarchie : Production > Atelier > Atelier nuit ; Commercial est indépendante.
  private final Team production = team("Production", null, manager.getId());
  private final Team atelier = team("Atelier", production.getId(), UUID.randomUUID());
  private final Team atelierNuit = team("Atelier nuit", atelier.getId(), UUID.randomUUID());
  private final Team commercial = team("Commercial", null, UUID.randomUUID());

  private GetMonthlyPlanning useCase;

  @BeforeEach
  void setUp() {
    when(teamRepository.findAllActive())
        .thenReturn(List.of(production, atelier, atelierNuit, commercial));
    when(teamRepository.findTeamsByManagerId(manager.getId())).thenReturn(List.of(production));
    when(absenceRepository.findByTeamIdAndStatus(any(), any())).thenReturn(List.of());
    useCase =
        new GetMonthlyPlanning(
            absenceRepository,
            mock(AbsenceTypeRepository.class),
            teamRepository,
            mock(UserRepository.class),
            new AuthorizationService());
  }

  @Test
  void visibleTeams_manager_voitSesEquipesEtToutesLeursSousEquipes() {
    assertThat(useCase.visibleTeams(manager)).containsExactly(production, atelier, atelierNuit);
  }

  @Test
  void visibleTeams_rh_voitToutesLesEquipesActives() {
    assertThat(useCase.visibleTeams(hr))
        .containsExactly(production, atelier, atelierNuit, commercial);
  }

  @Test
  void execute_rhSansFiltre_parcourtToutesLesEquipes() {
    useCase.execute(hr, YearMonth.of(2026, 9), null);

    verify(absenceRepository)
        .findByTeamIdAndStatus(commercial.getId(), AbsenceSimplifiedStatus.VALIDEE);
  }

  @Test
  void execute_filtreSurUneEquipe_inclutSesSousEquipesMaisPasLesAutres() {
    useCase.execute(manager, YearMonth.of(2026, 9), atelier.getId());

    verify(absenceRepository)
        .findByTeamIdAndStatus(atelierNuit.getId(), AbsenceSimplifiedStatus.VALIDEE);
    verify(absenceRepository, never())
        .findByTeamIdAndStatus(production.getId(), AbsenceSimplifiedStatus.VALIDEE);
  }

  @Test
  void execute_equipeHorsPerimetre_estRefusee() {
    assertThatThrownBy(() -> useCase.execute(manager, YearMonth.of(2026, 9), commercial.getId()))
        .isInstanceOf(UnauthorizedException.class);
  }

  private static Team team(String name, UUID parentTeamId, UUID n1ManagerId) {
    return new Team(
        UUID.randomUUID(), name, parentTeamId, ValidationMode.N1_ONLY, n1ManagerId, null, true);
  }

  private static User user(UserRole... roles) {
    UUID id = UUID.randomUUID();
    return new User(
        id,
        "sub-" + id,
        id + "@timemanager.fr",
        "Prénom",
        "Nom",
        true,
        null,
        false,
        Set.of(roles),
        Instant.now());
  }
}
