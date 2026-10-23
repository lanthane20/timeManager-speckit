package fr.timemanager.api.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.timemanager.api.absence.ManagerAbsenceController;
import fr.timemanager.api.testsupport.WebMvcTestConfig;
import fr.timemanager.application.absence.CancelValidatedAbsence;
import fr.timemanager.application.absence.CreateAbsenceForEmployee;
import fr.timemanager.application.absence.GetPendingAbsencesForManager;
import fr.timemanager.application.absence.RefuseAbsence;
import fr.timemanager.application.absence.ValidateAbsence;
import fr.timemanager.application.absence.ValidateAbsenceN2;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.counter.GetEmployeeCounters;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import java.time.Instant;
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

/** EF-012 : endpoint de validation de second niveau (N+2). */
@WebMvcTest(controllers = ManagerAbsenceController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class N2ValidationControllerTest {

  private static final SimpleGrantedAuthority ROLE_MANAGER =
      new SimpleGrantedAuthority("ROLE_MANAGER");
  private static final SimpleGrantedAuthority ROLE_SALARIE =
      new SimpleGrantedAuthority("ROLE_SALARIE");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetPendingAbsencesForManager getPendingAbsencesForManager;
  @MockitoBean private ValidateAbsence validateAbsence;
  @MockitoBean private ValidateAbsenceN2 validateAbsenceN2;
  @MockitoBean private RefuseAbsence refuseAbsence;
  @MockitoBean private CancelValidatedAbsence cancelValidatedAbsence;
  @MockitoBean private CreateAbsenceForEmployee createAbsenceForEmployee;
  @MockitoBean private GetEmployeeCounters getEmployeeCounters;
  @MockitoBean private CurrentUserProvider currentUserProvider;

  private final UUID absenceId = UUID.randomUUID();
  private User n2Manager;

  @BeforeEach
  void setUp() {
    UUID managerId = UUID.randomUUID();
    n2Manager =
        new User(
            managerId,
            "sub_" + managerId,
            managerId + "@timemanager.fr",
            "Nadia",
            "Bernard",
            true,
            null,
            false,
            Set.of(UserRole.SALARIE, UserRole.MANAGER),
            Instant.now());
    given(currentUserProvider.resolve(any())).willReturn(n2Manager);
  }

  @Test
  void validateN2_parLeN2DeLEquipe_retourne204() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/manager/absences/{id}/validate-n2", absenceId)
                .with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isNoContent());

    verify(validateAbsenceN2).execute(eq(n2Manager), eq(absenceId), any());
  }

  @Test
  void validateN2_parUnManagerQuiNestPasLeN2_retourne403() throws Exception {
    willThrow(new UnauthorizedException("pas le N+2"))
        .given(validateAbsenceN2)
        .execute(eq(n2Manager), eq(absenceId), any());

    mockMvc
        .perform(
            put("/api/v1/manager/absences/{id}/validate-n2", absenceId)
                .with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void validateN2_sansRoleManager_retourne403() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/manager/absences/{id}/validate-n2", absenceId)
                .with(jwt().authorities(ROLE_SALARIE)))
        .andExpect(status().isForbidden());
  }
}
