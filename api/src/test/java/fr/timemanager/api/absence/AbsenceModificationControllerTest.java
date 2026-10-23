package fr.timemanager.api.absence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.timemanager.api.testsupport.WebMvcTestConfig;
import fr.timemanager.application.absence.CancelAbsenceRequest;
import fr.timemanager.application.absence.CreateAbsenceRequest;
import fr.timemanager.application.absence.GetMyAbsences;
import fr.timemanager.application.absence.RequestAbsenceModification;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.counter.GetAbsenceCounters;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** EF-007, EF-008 : demande de modification d'une absence validée. */
@WebMvcTest(controllers = AbsenceController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class AbsenceModificationControllerTest {

  private static final SimpleGrantedAuthority ROLE_SALARIE =
      new SimpleGrantedAuthority("ROLE_SALARIE");
  private static final String BODY =
      """
      {"absenceTypeId":"%s","startDate":"2026-11-09","endDate":"2026-11-13",
       "halfDayStart":"NONE","halfDayEnd":"NONE"}
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateAbsenceRequest createAbsenceRequest;
  @MockitoBean private CancelAbsenceRequest cancelAbsenceRequest;
  @MockitoBean private GetMyAbsences getMyAbsences;
  @MockitoBean private RequestAbsenceModification requestAbsenceModification;
  @MockitoBean private GetAbsenceCounters getAbsenceCounters;
  @MockitoBean private TeamRepository teamRepository;
  @MockitoBean private CurrentUserProvider currentUserProvider;

  private final UUID employeeId = UUID.randomUUID();
  private final UUID absenceId = UUID.randomUUID();
  private final UUID absenceTypeId = UUID.randomUUID();
  private User employee;

  @BeforeEach
  void setUp() {
    employee =
        new User(
            employeeId,
            "sub_" + employeeId,
            employeeId + "@timemanager.fr",
            "Léa",
            "Martin",
            true,
            null,
            false,
            Set.of(UserRole.SALARIE),
            Instant.now());
    given(currentUserProvider.resolve(any())).willReturn(employee);
  }

  @Test
  void modify_absenceValidee_retourne200AvecLaNouvelleVersionEnAttente() throws Exception {
    given(requestAbsenceModification.execute(eq(employeeId), eq(absenceId), any(), any()))
        .willReturn(
            new AbsenceResponseDto(
                absenceId,
                employeeId,
                absenceTypeId,
                "Congés payés",
                "#4CAF50",
                LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 13),
                new BigDecimal("5.0"),
                AbsenceSimplifiedStatus.VALIDEE,
                Instant.now()));

    mockMvc
        .perform(
            put("/api/v1/absences/{id}/modify", absenceId)
                .with(jwt().authorities(ROLE_SALARIE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(absenceTypeId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startDate").value("2026-11-09"))
        .andExpect(jsonPath("$.workingDays").value(5.0));
  }

  @Test
  void modify_absenceNonValidee_retourne409() throws Exception {
    willThrow(new IllegalStateException("Seule une absence validée peut être modifiée"))
        .given(requestAbsenceModification)
        .execute(eq(employeeId), eq(absenceId), any(), any());

    mockMvc
        .perform(
            put("/api/v1/absences/{id}/modify", absenceId)
                .with(jwt().authorities(ROLE_SALARIE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(absenceTypeId)))
        .andExpect(status().isConflict());
  }

  @Test
  void modify_absenceDUnAutreSalarie_retourne403() throws Exception {
    willThrow(new SecurityException("absence d'un autre salarié"))
        .given(requestAbsenceModification)
        .execute(eq(employeeId), eq(absenceId), any(), any());

    mockMvc
        .perform(
            put("/api/v1/absences/{id}/modify", absenceId)
                .with(jwt().authorities(ROLE_SALARIE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(absenceTypeId)))
        .andExpect(status().isForbidden());
  }

  @Test
  void modify_sansRoleSalarie_retourne403() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/absences/{id}/modify", absenceId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INCONNU")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(absenceTypeId)))
        .andExpect(status().isForbidden());
  }
}
