package fr.timemanager.api.absence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.timemanager.api.testsupport.WebMvcTestConfig;
import fr.timemanager.application.absence.CancelAbsenceRequest;
import fr.timemanager.application.absence.CreateAbsenceRequest;
import fr.timemanager.application.absence.GetMyAbsences;
import fr.timemanager.application.absence.RequestAbsenceModification;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.counter.GetAbsenceCounters;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.shared.HalfDayType;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** EF-001, EF-006 : tests MockMvc pour la création et l'annulation de demandes d'absence. */
@WebMvcTest(controllers = AbsenceController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class AbsenceControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CreateAbsenceRequest createAbsenceRequest;
  @MockitoBean private CancelAbsenceRequest cancelAbsenceRequest;
  @MockitoBean private GetMyAbsences getMyAbsences;
  @MockitoBean private RequestAbsenceModification requestAbsenceModification;
  @MockitoBean private GetAbsenceCounters getAbsenceCounters;
  @MockitoBean private TeamRepository teamRepository;
  @MockitoBean private CurrentUserProvider currentUserProvider;

  private final UUID employeeId = UUID.randomUUID();
  private final UUID teamId = UUID.randomUUID();

  @Test
  void create_demandeValide_retourne201AvecStatutEnAttente() throws Exception {
    User employee = buildUser(employeeId, UserRole.SALARIE);
    given(currentUserProvider.resolve(any())).willReturn(employee);
    given(teamRepository.findTeamIdByMemberId(employeeId)).willReturn(Optional.of(teamId));

    UUID absenceTypeId = UUID.randomUUID();
    CreateAbsenceRequestDto requestDto =
        new CreateAbsenceRequestDto(
            absenceTypeId,
            LocalDate.of(2026, 6, 22),
            LocalDate.of(2026, 6, 23),
            HalfDayType.NONE,
            HalfDayType.NONE,
            null);
    AbsenceResponseDto responseDto =
        new AbsenceResponseDto(
            UUID.randomUUID(),
            employeeId,
            absenceTypeId,
            "Congés payés",
            "#0000FF",
            requestDto.startDate(),
            requestDto.endDate(),
            new BigDecimal("2.0"),
            AbsenceSimplifiedStatus.EN_ATTENTE,
            Instant.now());
    given(createAbsenceRequest.execute(eq(employeeId), eq(teamId), any(), any()))
        .willReturn(responseDto);

    mockMvc
        .perform(
            post("/api/v1/absences")
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_SALARIE")))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("EN_ATTENTE"));
  }

  @Test
  void create_sansRoleSalarie_retourne403() throws Exception {
    UUID absenceTypeId = UUID.randomUUID();
    CreateAbsenceRequestDto requestDto =
        new CreateAbsenceRequestDto(
            absenceTypeId,
            LocalDate.of(2026, 6, 22),
            LocalDate.of(2026, 6, 23),
            HalfDayType.NONE,
            HalfDayType.NONE,
            null);

    mockMvc
        .perform(
            post("/api/v1/absences")
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_MANAGER")))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isForbidden());
  }

  @Test
  void cancel_demandeDuSalarie_retourne204() throws Exception {
    User employee = buildUser(employeeId, UserRole.SALARIE);
    given(currentUserProvider.resolve(any())).willReturn(employee);
    UUID absenceId = UUID.randomUUID();

    mockMvc
        .perform(
            delete("/api/v1/absences/{id}", absenceId)
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_SALARIE"))))
        .andExpect(status().isNoContent());

    verify(cancelAbsenceRequest).execute(eq(employeeId), eq(absenceId), any());
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
