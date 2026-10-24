package fr.timemanager.api.user;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.timemanager.api.testsupport.WebMvcTestConfig;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Contrat de GET /api/v1/me : profil et rôles applicatifs de l'utilisateur connecté. */
@WebMvcTest(controllers = CurrentUserController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class CurrentUserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CurrentUserProvider currentUserProvider;

  @Test
  void me_retourneLesRolesEnregistresEnBase() throws Exception {
    UUID managerId = UUID.randomUUID();
    given(currentUserProvider.resolve(any()))
        .willReturn(
            new User(
                managerId,
                "sub-manager",
                "manon.dubois@timemanager.fr",
                "Manon",
                "Dubois",
                true,
                null,
                false,
                Set.of(UserRole.SALARIE, UserRole.MANAGER),
                Instant.now()));

    // Le jeton ne porte aucun rôle : ils proviennent de la base (comme pour un jeton Google).
    mockMvc
        .perform(get("/api/v1/me").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(managerId.toString()))
        .andExpect(jsonPath("$.firstName").value("Manon"))
        .andExpect(jsonPath("$.lastName").value("Dubois"))
        .andExpect(jsonPath("$.email").value("manon.dubois@timemanager.fr"))
        .andExpect(jsonPath("$.roles", containsInAnyOrder("SALARIE", "MANAGER")))
        .andExpect(jsonPath("$.googleSub").doesNotExist());
  }

  @Test
  void me_sansJeton_estRefuse() throws Exception {
    mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
  }
}
