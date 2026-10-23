package fr.timemanager.api.recurrence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.timemanager.api.testsupport.WebMvcTestConfig;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.recurrence.GetRecurrenceHistory;
import fr.timemanager.application.recurrence.ImportRecurrenceFromExcel;
import fr.timemanager.application.recurrence.InjectRecurrencePattern;
import fr.timemanager.application.recurrence.InjectRecurrencePattern.InjectionResult;
import fr.timemanager.domain.recurrence.InjectionSource;
import fr.timemanager.domain.recurrence.RecurrenceInjectionLog;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** EF-028 à EF-030 : endpoints du module récurrences. */
@WebMvcTest(controllers = RecurrenceController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class RecurrenceControllerTest {

  private static final SimpleGrantedAuthority ROLE_SALARIE =
      new SimpleGrantedAuthority("ROLE_SALARIE");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private InjectRecurrencePattern injectRecurrencePattern;
  @MockitoBean private ImportRecurrenceFromExcel importRecurrenceFromExcel;
  @MockitoBean private GetRecurrenceHistory getRecurrenceHistory;
  @MockitoBean private CurrentUserProvider currentUserProvider;

  private final UUID patternId = UUID.randomUUID();
  private User author;

  @BeforeEach
  void setUp() {
    UUID authorId = UUID.randomUUID();
    author =
        new User(
            authorId,
            "sub_" + authorId,
            authorId + "@timemanager.fr",
            "Alex",
            "Petit",
            true,
            null,
            true,
            Set.of(UserRole.SALARIE),
            Instant.now());
    given(currentUserProvider.resolve(any())).willReturn(author);
  }

  @Test
  void importExcel_retourne201AvecLeNombreDEvenementsCrees() throws Exception {
    given(importRecurrenceFromExcel.execute(eq(author), eq(patternId), any()))
        .willReturn(injectionResult(12, List.of()));

    var file =
        new MockMultipartFile(
            "file", "planning.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[] {1, 2});

    mockMvc
        .perform(
            multipart("/api/v1/recurrences/import")
                .file(file)
                .param("patternId", patternId.toString())
                .with(jwt().authorities(ROLE_SALARIE)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventsCreatedCount").value(12))
        .andExpect(jsonPath("$.absenceTypeCode").value("ECOLE"));
  }

  @Test
  void generate_retourne201EtRemonteLesAvertissementsSansBloquer() throws Exception {
    given(injectRecurrencePattern.execute(eq(author), eq(patternId), any(), any()))
        .willReturn(
            injectionResult(2, List.of("Chevauchement avec une absence existante le 2026-09-07")));

    mockMvc
        .perform(
            post("/api/v1/recurrences/generate")
                .with(jwt().authorities(ROLE_SALARIE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"patternId":"%s","dates":["2026-09-07","2026-09-14"],"source":"GENERATION"}
                    """
                        .formatted(patternId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventsCreatedCount").value(2))
        .andExpect(jsonPath("$.warnings.length()").value(1));
  }

  @Test
  void generate_sansDate_retourne400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/recurrences/generate")
                .with(jwt().authorities(ROLE_SALARIE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patternId\":\"%s\",\"dates\":[]}".formatted(patternId)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void history_retourneLHistoriqueDesInjections() throws Exception {
    given(getRecurrenceHistory.execute(author)).willReturn(List.of(injectionLog(5)));

    mockMvc
        .perform(get("/api/v1/recurrences/history").with(jwt().authorities(ROLE_SALARIE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].eventsCreatedCount").value(5))
        .andExpect(jsonPath("$[0].source").value("IMPORT"));
  }

  @Test
  void history_moduleNonActive_retourne403() throws Exception {
    given(getRecurrenceHistory.execute(author))
        .willThrow(new UnauthorizedException("module non activé"));

    mockMvc
        .perform(get("/api/v1/recurrences/history").with(jwt().authorities(ROLE_SALARIE)))
        .andExpect(status().isForbidden());
  }

  @Test
  void history_sansRoleSalarie_retourne403() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/recurrences/history")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INCONNU"))))
        .andExpect(status().isForbidden());
  }

  private InjectionResult injectionResult(int created, List<String> warnings) {
    return new InjectionResult(injectionLog(created), warnings, "ECOLE");
  }

  private RecurrenceInjectionLog injectionLog(int created) {
    return new RecurrenceInjectionLog(
        UUID.randomUUID(),
        patternId,
        InjectionSource.IMPORT,
        Instant.now(),
        author.getId(),
        created,
        LocalDate.of(2026, 9, 7),
        LocalDate.of(2026, 9, 21));
  }
}
