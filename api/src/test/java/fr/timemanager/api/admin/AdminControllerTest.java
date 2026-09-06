package fr.timemanager.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.timemanager.api.testsupport.WebMvcTestConfig;
import fr.timemanager.application.admin.ManageAbsenceTypes;
import fr.timemanager.application.admin.ManageTeams;
import fr.timemanager.application.admin.ManageUsers;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.team.ValidationMode;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.domain.user.WorkSchedule;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import java.time.DayOfWeek;
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

/** EF-031 à EF-035 : endpoints d'administration technique. */
@WebMvcTest(controllers = AdminController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
@Import(fr.timemanager.api.exception.GlobalExceptionHandler.class)
class AdminControllerTest {

  private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
  private static final SimpleGrantedAuthority ROLE_SALARIE =
      new SimpleGrantedAuthority("ROLE_SALARIE");
  private static final SimpleGrantedAuthority ROLE_RH = new SimpleGrantedAuthority("ROLE_RH");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ManageUsers manageUsers;
  @MockitoBean private ManageTeams manageTeams;
  @MockitoBean private ManageAbsenceTypes manageAbsenceTypes;
  @MockitoBean private CurrentUserProvider currentUserProvider;

  private final UUID adminId = UUID.randomUUID();
  private final UUID targetUserId = UUID.randomUUID();
  private final UUID teamId = UUID.randomUUID();
  private final UUID absenceTypeId = UUID.randomUUID();
  private User admin;

  @BeforeEach
  void setUp() {
    admin = user(adminId, Set.of(UserRole.ADMIN));
    given(currentUserProvider.resolve(any())).willReturn(admin);
  }

  @Test
  void listUsers_retourneLesUtilisateursSansDonneeDAbsence() throws Exception {
    given(manageUsers.list(admin))
        .willReturn(List.of(user(targetUserId, Set.of(UserRole.SALARIE))));

    mockMvc
        .perform(get("/api/v1/admin/users").with(jwt().authorities(ROLE_ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(targetUserId.toString()))
        .andExpect(jsonPath("$[0].absences").doesNotExist())
        .andExpect(jsonPath("$[0].counters").doesNotExist());
  }

  @Test
  void createUser_retourne201() throws Exception {
    given(manageUsers.create(eq(admin), any(), any(), any(), any(), any()))
        .willReturn(user(targetUserId, Set.of(UserRole.SALARIE)));

    mockMvc
        .perform(
            post("/api/v1/admin/users")
                .with(jwt().authorities(ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"googleSub":"sub-1","email":"nina@timemanager.fr","firstName":"Nina",
                     "lastName":"Roche","roles":["SALARIE"]}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(targetUserId + "@timemanager.fr"));
  }

  @Test
  void createUser_sansChampObligatoire_retourne400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/users")
                .with(jwt().authorities(ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"googleSub\":\"\",\"email\":\"\",\"firstName\":\"\",\"lastName\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void assignTeam_retourne204() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/admin/users/{id}/team/{teamId}", targetUserId, teamId)
                .with(jwt().authorities(ROLE_ADMIN)))
        .andExpect(status().isNoContent());

    verify(manageUsers).assignToTeam(admin, targetUserId, teamId);
  }

  @Test
  void defineWorkSchedule_retourne201AvecLesJoursTravailles() throws Exception {
    given(manageUsers.defineWorkSchedule(eq(admin), eq(targetUserId), any(), any(), any()))
        .willReturn(
            new WorkSchedule(
                UUID.randomUUID(),
                targetUserId,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                LocalDate.of(2026, 1, 1),
                null,
                Instant.now()));

    mockMvc
        .perform(
            post("/api/v1/admin/users/{id}/work-schedules", targetUserId)
                .with(jwt().authorities(ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"workingDays":["MONDAY","TUESDAY","THURSDAY","FRIDAY"],
                     "effectiveFrom":"2026-01-01"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.effectiveFrom").value("2026-01-01"));
  }

  @Test
  void createTeam_retourne201AvecLeModeDeValidation() throws Exception {
    given(manageTeams.create(eq(admin), any(), any(), any(), any(), any()))
        .willReturn(
            new Team(
                teamId,
                "Équipe Alpha",
                null,
                ValidationMode.N1_AND_N2,
                adminId,
                UUID.randomUUID(),
                true));

    mockMvc
        .perform(
            post("/api/v1/admin/teams")
                .with(jwt().authorities(ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Équipe Alpha","validationMode":"N1_AND_N2","n1ManagerId":"%s",
                     "n2ManagerId":"%s"}
                    """
                        .formatted(adminId, UUID.randomUUID())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.validationMode").value("N1_AND_N2"));
  }

  @Test
  void createAbsenceType_retourne201EtLeTypeEstActif() throws Exception {
    given(manageAbsenceTypes.create(eq(admin), any()))
        .willReturn(absenceType(absenceTypeId, "TELETRAVAIL", true));

    mockMvc
        .perform(
            post("/api/v1/admin/absence-types")
                .with(jwt().authorities(ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"TELETRAVAIL","label":"Télétravail","color":"#3F51B5",
                     "requiresManagerValidation":true,"requiresJustification":false,
                     "allowedRoles":["SALARIE"],"quantified":true,"annualReset":true,
                     "allowsOverlap":false}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("TELETRAVAIL"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void setAbsenceTypeActive_retourne200() throws Exception {
    given(manageAbsenceTypes.setActive(admin, absenceTypeId, false))
        .willReturn(absenceType(absenceTypeId, "TELETRAVAIL", false));

    mockMvc
        .perform(
            put("/api/v1/admin/absence-types/{id}/active/{active}", absenceTypeId, false)
                .with(jwt().authorities(ROLE_ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void endpointsAdmin_sansRoleAdmin_retournent403() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/users").with(jwt().authorities(ROLE_SALARIE)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/v1/admin/teams").with(jwt().authorities(ROLE_RH)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/v1/admin/absence-types").with(jwt().authorities(ROLE_SALARIE)))
        .andExpect(status().isForbidden());
  }

  private User user(UUID id, Set<UserRole> roles) {
    return new User(
        id,
        "sub_" + id,
        id + "@timemanager.fr",
        "Nina",
        "Roche",
        true,
        null,
        false,
        roles,
        Instant.now());
  }

  private AbsenceType absenceType(UUID id, String code, boolean active) {
    return new AbsenceType(
        id,
        code,
        "Télétravail",
        "#3F51B5",
        true,
        false,
        Set.of(UserRole.SALARIE),
        true,
        true,
        false,
        active);
  }
}
