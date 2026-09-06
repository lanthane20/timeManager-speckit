package fr.timemanager.api.absence;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.timemanager.api.testsupport.WebMvcTestConfig;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.user.UserRole;
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

/** Référentiel des types d'absence exposé au formulaire de demande (US1). */
@WebMvcTest(controllers = AbsenceTypeController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class AbsenceTypeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AbsenceTypeRepository absenceTypeRepository;

  @Test
  void listActive_retourneLesTypesActifsSansRegleDeGestionInterne() throws Exception {
    UUID typeId = UUID.randomUUID();
    given(absenceTypeRepository.findAllActive())
        .willReturn(
            List.of(
                new AbsenceType(
                    typeId,
                    "CP",
                    "Congés payés",
                    "#4CAF50",
                    true,
                    false,
                    Set.of(UserRole.SALARIE),
                    true,
                    true,
                    false,
                    true)));

    mockMvc
        .perform(
            get("/api/v1/absence-types")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SALARIE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(typeId.toString()))
        .andExpect(jsonPath("$[0].code").value("CP"))
        .andExpect(jsonPath("$[0].label").value("Congés payés"))
        .andExpect(jsonPath("$[0].allowsOverlap").doesNotExist())
        .andExpect(jsonPath("$[0].allowedRoles").doesNotExist());
  }

  @Test
  void listActive_sansRoleSalarie_estRefuse() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/absence-types")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INCONNU"))))
        .andExpect(status().isForbidden());
  }
}
