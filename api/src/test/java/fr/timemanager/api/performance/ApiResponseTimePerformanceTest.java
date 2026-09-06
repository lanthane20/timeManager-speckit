package fr.timemanager.api.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import fr.timemanager.api.absence.AbsenceController;
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
import java.util.ArrayList;
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

/**
 * CS-001 : les opérations courantes doivent répondre sous 200 ms au 95e centile.
 *
 * <p>Ce test mesure le coût de la couche HTTP (sérialisation, sécurité de méthode, mapping) avec
 * les cas d'usage doublés : il isole la part maîtrisée par l'application. La mesure de bout en
 * bout, base de données comprise, relève des tests de charge d'environnement d'intégration.
 */
@WebMvcTest(controllers = AbsenceController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class ApiResponseTimePerformanceTest {

  private static final int WARMUP_REQUESTS = 20;
  private static final int MEASURED_REQUESTS = 100;
  private static final long P95_BUDGET_MS = 200;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateAbsenceRequest createAbsenceRequest;
  @MockitoBean private CancelAbsenceRequest cancelAbsenceRequest;
  @MockitoBean private GetMyAbsences getMyAbsences;
  @MockitoBean private RequestAbsenceModification requestAbsenceModification;
  @MockitoBean private GetAbsenceCounters getAbsenceCounters;
  @MockitoBean private TeamRepository teamRepository;
  @MockitoBean private CurrentUserProvider currentUserProvider;

  private final UUID employeeId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    User employee =
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
    given(getMyAbsences.execute(employeeId)).willReturn(buildAbsences(50));
  }

  @Test
  void cs001_laListeDesDemandesRepondSous200msAu95eCentile() throws Exception {
    for (int i = 0; i < WARMUP_REQUESTS; i++) {
      performRequest();
    }

    List<Long> durations = new ArrayList<>(MEASURED_REQUESTS);
    for (int i = 0; i < MEASURED_REQUESTS; i++) {
      long startedAt = System.nanoTime();
      performRequest();
      durations.add((System.nanoTime() - startedAt) / 1_000_000);
    }

    durations.sort(Long::compareTo);
    long p95 = durations.get((int) Math.ceil(MEASURED_REQUESTS * 0.95) - 1);

    assertThat(p95).as("p95 mesuré : %d ms", p95).isLessThan(P95_BUDGET_MS);
  }

  private void performRequest() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/absences/me")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SALARIE"))))
        .andReturn();
  }

  private List<AbsenceResponseDto> buildAbsences(int count) {
    List<AbsenceResponseDto> absences = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      LocalDate start = LocalDate.of(2026, 1 + index % 12, 1 + index % 28);
      absences.add(
          new AbsenceResponseDto(
              UUID.randomUUID(),
              employeeId,
              UUID.randomUUID(),
              "Congés payés",
              "#4CAF50",
              start,
              start.plusDays(2),
              new BigDecimal("3.0"),
              AbsenceSimplifiedStatus.VALIDEE,
              Instant.now()));
    }
    return absences;
  }
}
