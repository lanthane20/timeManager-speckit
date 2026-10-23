package fr.timemanager.api.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.counter.CompteurAbsence;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

/** EF-009 à EF-014 : endpoints manager — validation, refus, annulation, saisie déléguée. */
@WebMvcTest(controllers = ManagerAbsenceController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class ManagerAbsenceControllerTest {

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

  private final UUID managerId = UUID.randomUUID();
  private final UUID employeeId = UUID.randomUUID();
  private final UUID absenceId = UUID.randomUUID();
  private User manager;

  @BeforeEach
  void setUp() {
    manager = buildUser(managerId, UserRole.SALARIE, UserRole.MANAGER);
    given(currentUserProvider.resolve(any())).willReturn(manager);
  }

  @Test
  void pending_retourneLaFileDeValidationDuManager() throws Exception {
    given(getPendingAbsencesForManager.execute(manager)).willReturn(List.of(absenceDto()));

    mockMvc
        .perform(get("/api/v1/manager/absences/pending").with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("EN_ATTENTE"));
  }

  @Test
  void validate_demandeDuPerimetre_retourne204() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/manager/absences/{id}/validate", absenceId)
                .with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isNoContent());

    verify(validateAbsence).execute(eq(manager), eq(absenceId), any());
  }

  @Test
  void refuse_avecMotif_retourne204() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/manager/absences/{id}/refuse", absenceId)
                .with(jwt().authorities(ROLE_MANAGER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Effectif insuffisant\"}"))
        .andExpect(status().isNoContent());

    verify(refuseAbsence).execute(eq(manager), eq(absenceId), eq("Effectif insuffisant"), any());
  }

  @Test
  void refuse_sansMotif_retourne400() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/manager/absences/{id}/refuse", absenceId)
                .with(jwt().authorities(ROLE_MANAGER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"  \"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void validate_sansRoleManager_retourne403() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/manager/absences/{id}/validate", absenceId)
                .with(jwt().authorities(ROLE_SALARIE)))
        .andExpect(status().isForbidden());
  }

  @Test
  void cancel_absenceValideeAvecJustification_retourne204() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/manager/absences/{id}/cancel", absenceId)
                .with(jwt().authorities(ROLE_MANAGER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Fermeture du site\"}"))
        .andExpect(status().isNoContent());

    verify(cancelValidatedAbsence)
        .execute(eq(manager), eq(absenceId), eq("Fermeture du site"), any());
  }

  @Test
  void cancel_sansJustification_retourne400() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/manager/absences/{id}/cancel", absenceId)
                .with(jwt().authorities(ROLE_MANAGER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cancel_sansRoleManager_retourne403() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/manager/absences/{id}/cancel", absenceId)
                .with(jwt().authorities(ROLE_SALARIE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Fermeture du site\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void createForEmployee_salarieDuPerimetre_retourne201() throws Exception {
    given(createAbsenceForEmployee.execute(eq(manager), eq(employeeId), any(), any()))
        .willReturn(absenceDto());

    mockMvc
        .perform(
            post("/api/v1/manager/employees/{employeeId}/absences", employeeId)
                .with(jwt().authorities(ROLE_MANAGER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"absenceTypeId":"%s","startDate":"2026-09-07","endDate":"2026-09-08",
                     "halfDayStart":"NONE","halfDayEnd":"NONE"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("EN_ATTENTE"));
  }

  @Test
  void createForEmployee_horsPerimetre_retourne403() throws Exception {
    willThrow(new UnauthorizedException("hors périmètre"))
        .given(createAbsenceForEmployee)
        .execute(eq(manager), eq(employeeId), any(), any());

    mockMvc
        .perform(
            post("/api/v1/manager/employees/{employeeId}/absences", employeeId)
                .with(jwt().authorities(ROLE_MANAGER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"absenceTypeId":"%s","startDate":"2026-09-07","endDate":"2026-09-08",
                     "halfDayStart":"NONE","halfDayEnd":"NONE"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isForbidden());
  }

  @Test
  void employeeCounters_salarieDuPerimetre_retourne200() throws Exception {
    UUID absenceTypeId = UUID.randomUUID();
    given(getEmployeeCounters.execute(manager, employeeId))
        .willReturn(
            List.of(
                new CompteurAbsence(
                    employeeId,
                    absenceTypeId,
                    new BigDecimal("3.0"),
                    new BigDecimal("1.0"),
                    new BigDecimal("2.0"))));

    mockMvc
        .perform(
            get("/api/v1/manager/employees/{employeeId}/counters", employeeId)
                .with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].absenceTypeId").value(absenceTypeId.toString()))
        .andExpect(jsonPath("$[0].solde").doesNotExist());
  }

  @Test
  void employeeCounters_horsPerimetre_retourne403() throws Exception {
    given(getEmployeeCounters.execute(manager, employeeId))
        .willThrow(new UnauthorizedException("hors périmètre"));

    mockMvc
        .perform(
            get("/api/v1/manager/employees/{employeeId}/counters", employeeId)
                .with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void employeeCounters_sansRoleManager_retourne403() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/manager/employees/{employeeId}/counters", employeeId)
                .with(jwt().authorities(ROLE_SALARIE)))
        .andExpect(status().isForbidden());
  }

  private AbsenceResponseDto absenceDto() {
    return new AbsenceResponseDto(
        absenceId,
        employeeId,
        UUID.randomUUID(),
        "Congés payés",
        "#4CAF50",
        LocalDate.of(2026, 9, 7),
        LocalDate.of(2026, 9, 8),
        new BigDecimal("2.0"),
        AbsenceSimplifiedStatus.EN_ATTENTE,
        Instant.now());
  }

  private User buildUser(UUID id, UserRole... roles) {
    return new User(
        id,
        "sub_" + id,
        id + "@timemanager.fr",
        "Manon",
        "Dubois",
        true,
        null,
        false,
        Set.of(roles),
        Instant.now());
  }
}
