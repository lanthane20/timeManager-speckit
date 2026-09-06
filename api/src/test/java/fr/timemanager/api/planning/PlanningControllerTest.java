package fr.timemanager.api.planning;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.timemanager.api.testsupport.WebMvcTestConfig;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.dto.planning.PlanningEntryDto;
import fr.timemanager.application.planning.GetMonthlyPlanning;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** EF-015 à EF-017 : planning mensuel — mois par défaut, filtre équipe, périmètre. */
@WebMvcTest(controllers = PlanningController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class PlanningControllerTest {

  private static final SimpleGrantedAuthority ROLE_MANAGER =
      new SimpleGrantedAuthority("ROLE_MANAGER");
  private static final SimpleGrantedAuthority ROLE_SALARIE =
      new SimpleGrantedAuthority("ROLE_SALARIE");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetMonthlyPlanning getMonthlyPlanning;
  @MockitoBean private CurrentUserProvider currentUserProvider;

  private final UUID managerId = UUID.randomUUID();
  private final UUID teamId = UUID.randomUUID();
  private User manager;

  @BeforeEach
  void setUp() {
    manager =
        new User(
            managerId,
            "sub_" + managerId,
            "manager@timemanager.fr",
            "Manon",
            "Dubois",
            true,
            null,
            false,
            Set.of(UserRole.SALARIE, UserRole.MANAGER),
            Instant.now());
    given(currentUserProvider.resolve(any())).willReturn(manager);
  }

  @Test
  void planning_sansParametre_utiliseLeMoisCourantEtToutLePerimetre() throws Exception {
    given(getMonthlyPlanning.execute(eq(manager), isNull(), isNull())).willReturn(List.of(entry()));

    mockMvc
        .perform(get("/api/v1/planning").with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].absenceTypeCode").value("CP"))
        .andExpect(jsonPath("$[0].absenceTypeColor").value("#4CAF50"))
        .andExpect(jsonPath("$[0].employeeName").value("Léa Martin"));
  }

  @Test
  void planning_avecMoisEtEquipe_transmetLesFiltres() throws Exception {
    given(getMonthlyPlanning.execute(manager, YearMonth.of(2026, 9), teamId))
        .willReturn(List.of(entry()));

    mockMvc
        .perform(
            get("/api/v1/planning")
                .param("month", "2026-09")
                .param("teamId", teamId.toString())
                .with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void planning_moisMalForme_retourne400() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/planning")
                .param("month", "septembre")
                .with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void planning_equipeHorsPerimetre_retourne403() throws Exception {
    given(getMonthlyPlanning.execute(eq(manager), isNull(), eq(teamId)))
        .willThrow(new UnauthorizedException("hors périmètre"));

    mockMvc
        .perform(
            get("/api/v1/planning")
                .param("teamId", teamId.toString())
                .with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void planning_sansRoleManagerNiRh_retourne403() throws Exception {
    mockMvc
        .perform(get("/api/v1/planning").with(jwt().authorities(ROLE_SALARIE)))
        .andExpect(status().isForbidden());
  }

  private PlanningEntryDto entry() {
    return new PlanningEntryDto(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Léa Martin",
        teamId,
        "CP",
        "Congés payés",
        "#4CAF50",
        LocalDate.of(2026, 9, 7),
        LocalDate.of(2026, 9, 8),
        AbsenceSimplifiedStatus.VALIDEE);
  }
}
