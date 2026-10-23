package fr.timemanager.api.absence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** EF-019, EF-020 : tests MockMvc pour la liste des demandes et les compteurs du salarié. */
@WebMvcTest(controllers = AbsenceController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class MyAbsencesControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateAbsenceRequest createAbsenceRequest;
  @MockitoBean private CancelAbsenceRequest cancelAbsenceRequest;
  @MockitoBean private GetMyAbsences getMyAbsences;
  @MockitoBean private RequestAbsenceModification requestAbsenceModification;
  @MockitoBean private GetAbsenceCounters getAbsenceCounters;
  @MockitoBean private TeamRepository teamRepository;
  @MockitoBean private CurrentUserProvider currentUserProvider;

  private final UUID employeeId = UUID.randomUUID();

  @Test
  void myAbsences_retourneLaListeDesDemandesDuSalarie() throws Exception {
    User employee = buildUser(employeeId, UserRole.SALARIE);
    given(currentUserProvider.resolve(any())).willReturn(employee);

    AbsenceResponseDto absence =
        new AbsenceResponseDto(
            UUID.randomUUID(),
            employeeId,
            UUID.randomUUID(),
            "Congés payés",
            "#0000FF",
            LocalDate.of(2026, 6, 22),
            LocalDate.of(2026, 6, 23),
            new BigDecimal("2.0"),
            AbsenceSimplifiedStatus.EN_ATTENTE,
            Instant.now());
    given(getMyAbsences.execute(employeeId)).willReturn(List.of(absence));

    mockMvc
        .perform(
            get("/api/v1/absences/me")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SALARIE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("EN_ATTENTE"))
        .andExpect(jsonPath("$[0].absenceTypeLabel").value("Congés payés"));
  }

  @Test
  void myCounters_retourneLesCompteursSansSoldeNiDroitsAcquis() throws Exception {
    User employee = buildUser(employeeId, UserRole.SALARIE);
    given(currentUserProvider.resolve(any())).willReturn(employee);

    UUID absenceTypeId = UUID.randomUUID();
    CompteurAbsence counter =
        new CompteurAbsence(
            employeeId,
            absenceTypeId,
            new BigDecimal("3.0"),
            new BigDecimal("1.0"),
            new BigDecimal("2.0"));
    given(getAbsenceCounters.execute(employeeId)).willReturn(List.of(counter));

    mockMvc
        .perform(
            get("/api/v1/absences/me/counters")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SALARIE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].absenceTypeId").value(absenceTypeId.toString()))
        .andExpect(jsonPath("$[0].daysPending").value(2.0))
        .andExpect(jsonPath("$[0].solde").doesNotExist())
        .andExpect(jsonPath("$[0].droitsAcquis").doesNotExist())
        .andExpect(jsonPath("$[0].creditDisponible").doesNotExist());
  }

  private User buildUser(UUID id, UserRole role) {
    return new User(
        id,
        "sub_" + id,
        "user@example.com",
        "Jean",
        "Dupont",
        true,
        null,
        false,
        Set.of(role),
        Instant.now());
  }
}
