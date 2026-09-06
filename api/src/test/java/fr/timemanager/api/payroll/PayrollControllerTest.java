package fr.timemanager.api.payroll;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.timemanager.api.testsupport.WebMvcTestConfig;
import fr.timemanager.application.counter.GetAbsenceCounters;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.payroll.HrAbsenceSearchCriteria;
import fr.timemanager.application.payroll.CloseMonth;
import fr.timemanager.application.payroll.CorrectAbsence;
import fr.timemanager.application.payroll.CreateSpecialAbsenceByHR;
import fr.timemanager.application.payroll.GeneratePayrollExport;
import fr.timemanager.application.payroll.GetRegularisations;
import fr.timemanager.application.payroll.SearchAbsencesForHR;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.counter.CompteurAbsence;
import fr.timemanager.domain.payroll.PeriodeCloture;
import fr.timemanager.domain.payroll.Regularisation;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** EF-020 à EF-027 : endpoints RH — clôture, régularisations, export, consultation, saisie. */
@WebMvcTest(controllers = PayrollController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class PayrollControllerTest {

  private static final SimpleGrantedAuthority ROLE_RH = new SimpleGrantedAuthority("ROLE_RH");
  private static final SimpleGrantedAuthority ROLE_SALARIE =
      new SimpleGrantedAuthority("ROLE_SALARIE");
  private static final SimpleGrantedAuthority ROLE_MANAGER =
      new SimpleGrantedAuthority("ROLE_MANAGER");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CloseMonth closeMonth;
  @MockitoBean private GetRegularisations getRegularisations;
  @MockitoBean private GeneratePayrollExport generatePayrollExport;
  @MockitoBean private SearchAbsencesForHR searchAbsencesForHR;
  @MockitoBean private CreateSpecialAbsenceByHR createSpecialAbsenceByHR;
  @MockitoBean private CorrectAbsence correctAbsence;
  @MockitoBean private GetAbsenceCounters getAbsenceCounters;
  @MockitoBean private CurrentUserProvider currentUserProvider;

  private final UUID hrUserId = UUID.randomUUID();
  private final UUID employeeId = UUID.randomUUID();
  private final UUID absenceTypeId = UUID.randomUUID();
  private User hrUser;

  @BeforeEach
  void setUp() {
    hrUser =
        new User(
            hrUserId,
            "sub_" + hrUserId,
            hrUserId + "@timemanager.fr",
            "Rachel",
            "Hubert",
            true,
            null,
            false,
            Set.of(UserRole.SALARIE, UserRole.RH),
            Instant.now());
    given(currentUserProvider.resolve(any())).willReturn(hrUser);
  }

  @Test
  void close_periodeMensuelle_retourne201() throws Exception {
    var period = new PeriodeCloture(UUID.randomUUID(), 2026, 9);
    period.close(hrUserId, Instant.now());
    given(closeMonth.execute(eq(hrUser), eq(YearMonth.of(2026, 9)), any())).willReturn(period);

    mockMvc
        .perform(
            post("/api/v1/rh/periods/{year}/{month}/close", 2026, 9)
                .with(jwt().authorities(ROLE_RH)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.year").value(2026))
        .andExpect(jsonPath("$.month").value(9))
        .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  void close_sansRoleRh_retourne403() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/rh/periods/{year}/{month}/close", 2026, 9)
                .with(jwt().authorities(ROLE_MANAGER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void regularisations_filtreesParPeriode_retourne200() throws Exception {
    given(getRegularisations.execute(hrUser, YearMonth.of(2026, 9)))
        .willReturn(
            List.of(
                new Regularisation(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    employeeId,
                    Instant.now(),
                    "Absence validée après clôture")));

    mockMvc
        .perform(
            get("/api/v1/rh/regularisations")
                .param("year", "2026")
                .param("month", "9")
                .with(jwt().authorities(ROLE_RH)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].employeeId").value(employeeId.toString()));
  }

  @Test
  void exportAnnuel_retourneUnFichierExcelEnPieceJointe() throws Exception {
    given(generatePayrollExport.execute(hrUser, 2026)).willReturn(new byte[] {1, 2, 3});

    mockMvc
        .perform(get("/api/v1/rh/export/annual/{year}", 2026).with(jwt().authorities(ROLE_RH)))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"absences-2026.xlsx\""))
        .andExpect(
            header()
                .string(
                    "Content-Type",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
  }

  @Test
  void searchAbsences_transmetTousLesFiltres() throws Exception {
    given(searchAbsencesForHR.execute(eq(hrUser), any())).willReturn(List.of(absenceDto()));

    mockMvc
        .perform(
            get("/api/v1/rh/absences")
                .param("employeeId", employeeId.toString())
                .param("typeId", absenceTypeId.toString())
                .param("from", "2026-01-01")
                .param("to", "2026-12-31")
                .with(jwt().authorities(ROLE_RH)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].employeeId").value(employeeId.toString()));

    ArgumentCaptor<HrAbsenceSearchCriteria> captor =
        ArgumentCaptor.forClass(HrAbsenceSearchCriteria.class);
    verify(searchAbsencesForHR).execute(eq(hrUser), captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().employeeId())
        .isEqualTo(employeeId);
    org.assertj.core.api.Assertions.assertThat(captor.getValue().from())
        .isEqualTo(LocalDate.of(2026, 1, 1));
  }

  @Test
  void searchAbsences_sansRoleRh_retourne403() throws Exception {
    mockMvc
        .perform(get("/api/v1/rh/absences").with(jwt().authorities(ROLE_SALARIE)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createSpecialAbsence_parLaRh_retourne201() throws Exception {
    given(createSpecialAbsenceByHR.execute(eq(hrUser), eq(employeeId), any(), any()))
        .willReturn(absenceDto());

    mockMvc
        .perform(
            post("/api/v1/rh/absences")
                .param("employeeId", employeeId.toString())
                .with(jwt().authorities(ROLE_RH))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("VALIDEE"));
  }

  @Test
  void createSpecialAbsence_parUnSalarie_retourne403() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/rh/absences")
                .param("employeeId", employeeId.toString())
                .with(jwt().authorities(ROLE_SALARIE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()))
        .andExpect(status().isForbidden());
  }

  @Test
  void createSpecialAbsence_parUnManager_retourne403() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/rh/absences")
                .param("employeeId", employeeId.toString())
                .with(jwt().authorities(ROLE_MANAGER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()))
        .andExpect(status().isForbidden());
  }

  @Test
  void correct_absenceExistante_retourne200() throws Exception {
    UUID absenceId = UUID.randomUUID();
    given(correctAbsence.execute(eq(hrUser), eq(absenceId), any(), any())).willReturn(absenceDto());

    mockMvc
        .perform(
            put("/api/v1/rh/absences/{id}/correct", absenceId)
                .with(jwt().authorities(ROLE_RH))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.workingDays").value(5.0));
  }

  @Test
  void employeeCounters_parLaRh_retourne200SansSolde() throws Exception {
    given(getAbsenceCounters.execute(employeeId))
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
            get("/api/v1/rh/employees/{employeeId}/counters", employeeId)
                .with(jwt().authorities(ROLE_RH)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].daysPending").value(2.0))
        .andExpect(jsonPath("$[0].solde").doesNotExist());
  }

  private String requestBody() {
    return """
        {"absenceTypeId":"%s","startDate":"2026-10-05","endDate":"2026-10-09",
         "halfDayStart":"NONE","halfDayEnd":"NONE"}
        """
        .formatted(absenceTypeId);
  }

  private AbsenceResponseDto absenceDto() {
    return new AbsenceResponseDto(
        UUID.randomUUID(),
        employeeId,
        absenceTypeId,
        "Arrêt maladie",
        "#F44336",
        LocalDate.of(2026, 10, 5),
        LocalDate.of(2026, 10, 9),
        new BigDecimal("5.0"),
        AbsenceSimplifiedStatus.VALIDEE,
        Instant.now());
  }
}
