package fr.timemanager.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.admin.ManageAbsenceTypes.AbsenceTypeDefinition;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.team.ValidationMode;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.domain.user.WorkSchedule;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Scénarios 1 à 3 de US7 — EF-031 à EF-035 : paramétrage par l'admin technique. */
class AdminUseCasesTest extends SimpleScenarioTest<AdminUseCasesTest.AdminStage> {

  @Test
  void scenario1_ef031_lAdminCreeUnUtilisateurEtLAffecteAUneEquipe() {
    given().un_admin_technique();

    when().l_admin_cree_un_utilisateur().puis_l_affecte_a_une_equipe();

    then().l_utilisateur_est_enregistre().and().l_affectation_a_l_equipe_est_enregistree();
  }

  @Test
  void ef031_laDesactivationRemplaceLaSuppression() {
    given().un_admin_technique().et_un_utilisateur_existant();

    when().l_admin_desactive_l_utilisateur();

    then().l_utilisateur_enregistre_est_inactif();
  }

  @Test
  void ef031b_lAdminConfigureLePlanningContractuelDUnSalarie() {
    given().un_admin_technique().et_un_utilisateur_existant();

    when().l_admin_definit_un_planning_a_quatre_cinquiemes();

    then().le_planning_exclut_le_mercredi();
  }

  @Test
  void scenario2_ef032_ef033_lAdminCreeUneEquipeEnDoubleValidation() {
    given().un_admin_technique();

    when().l_admin_cree_une_equipe(ValidationMode.N1_AND_N2, true);

    then().l_equipe_est_enregistree_en_mode(ValidationMode.N1_AND_N2);
  }

  @Test
  void ef033_uneEquipeEnDoubleValidationSansN2_estRefusee() {
    given().un_admin_technique();

    when().l_admin_cree_une_equipe(ValidationMode.N1_AND_N2, false);

    then().l_operation_echoue_avec(IllegalArgumentException.class);
  }

  @Test
  void scenario3_ef034_lAdminCreeUnTypeDAbsenceDisponibleALaSaisie() {
    given().un_admin_technique();

    when().l_admin_cree_un_type_d_absence("TELETRAVAIL");

    then().le_type_est_enregistre_et_actif("TELETRAVAIL");
  }

  @Test
  void ef034_unCodeDeTypeDejaUtilise_estRefuse() {
    given().un_admin_technique().et_un_type_existant("CP");

    when().l_admin_cree_un_type_d_absence("CP");

    then().l_operation_echoue_avec(IllegalArgumentException.class);
  }

  @Test
  void ef035_unUtilisateurSansRoleAdmin_nAccedeAAucunParametrage() {
    given().un_manager_sans_role_admin();

    when().l_admin_cree_un_utilisateur();

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  public static class AdminStage extends Stage<AdminStage> {

    private final AdminRepository adminRepository = mock(AdminRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);
    private final AuthorizationService authorizationService = new AuthorizationService();

    private final UUID adminId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID n1ManagerId = UUID.randomUUID();
    private final UUID n2ManagerId = UUID.randomUUID();

    private User author;
    private Class<? extends RuntimeException> thrownExceptionType;
    private WorkSchedule savedSchedule;

    public AdminStage un_admin_technique() {
      author = user(adminId, Set.of(UserRole.ADMIN));
      Mockito.when(adminRepository.saveUser(any()))
          .thenAnswer(invocation -> invocation.<User>getArgument(0));
      Mockito.when(adminRepository.saveTeam(any()))
          .thenAnswer(invocation -> invocation.<Team>getArgument(0));
      Mockito.when(adminRepository.saveAbsenceType(any()))
          .thenAnswer(invocation -> invocation.<AbsenceType>getArgument(0));
      Mockito.when(adminRepository.saveWorkSchedule(any()))
          .thenAnswer(invocation -> invocation.<WorkSchedule>getArgument(0));
      Mockito.when(absenceTypeRepository.findAllActive()).thenReturn(List.of());
      return self();
    }

    public AdminStage un_manager_sans_role_admin() {
      un_admin_technique();
      author = user(adminId, Set.of(UserRole.SALARIE, UserRole.MANAGER));
      return self();
    }

    public AdminStage et_un_utilisateur_existant() {
      Mockito.when(adminRepository.findUserById(targetUserId))
          .thenReturn(Optional.of(user(targetUserId, Set.of(UserRole.SALARIE))));
      return self();
    }

    public AdminStage et_un_type_existant(String code) {
      Mockito.when(absenceTypeRepository.findAllActive())
          .thenReturn(List.of(absenceType(UUID.randomUUID(), code)));
      return self();
    }

    public AdminStage l_admin_cree_un_utilisateur() {
      run(
          () ->
              manageUsers()
                  .create(
                      author,
                      "sub-nouveau",
                      "nouveau@timemanager.fr",
                      "Nina",
                      "Roche",
                      Set.of(UserRole.SALARIE)));
      return self();
    }

    public AdminStage puis_l_affecte_a_une_equipe() {
      et_un_utilisateur_existant();
      run(() -> manageUsers().assignToTeam(author, targetUserId, teamId));
      return self();
    }

    public AdminStage l_admin_desactive_l_utilisateur() {
      run(() -> manageUsers().deactivate(author, targetUserId));
      return self();
    }

    public AdminStage l_admin_definit_un_planning_a_quatre_cinquiemes() {
      run(
          () ->
              savedSchedule =
                  manageUsers()
                      .defineWorkSchedule(
                          author,
                          targetUserId,
                          Set.of(
                              DayOfWeek.MONDAY,
                              DayOfWeek.TUESDAY,
                              DayOfWeek.THURSDAY,
                              DayOfWeek.FRIDAY),
                          LocalDate.of(2026, 1, 1),
                          null));
      return self();
    }

    public AdminStage l_admin_cree_une_equipe(ValidationMode mode, boolean withN2) {
      run(
          () ->
              new ManageTeams(adminRepository, teamRepository, authorizationService)
                  .create(
                      author,
                      "Équipe Alpha",
                      null,
                      mode,
                      n1ManagerId,
                      withN2 ? n2ManagerId : null));
      return self();
    }

    public AdminStage l_admin_cree_un_type_d_absence(String code) {
      run(
          () ->
              new ManageAbsenceTypes(adminRepository, absenceTypeRepository, authorizationService)
                  .create(
                      author,
                      new AbsenceTypeDefinition(
                          code,
                          "Libellé " + code,
                          "#3F51B5",
                          true,
                          false,
                          Set.of(UserRole.SALARIE),
                          true,
                          true,
                          false)));
      return self();
    }

    public AdminStage l_utilisateur_est_enregistre() {
      assertThat(thrownExceptionType).isNull();
      ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
      verify(adminRepository).saveUser(captor.capture());
      assertThat(captor.getValue().getEmail()).isEqualTo("nouveau@timemanager.fr");
      assertThat(captor.getValue().isActive()).isTrue();
      return self();
    }

    public AdminStage l_affectation_a_l_equipe_est_enregistree() {
      verify(adminRepository).assignUserToTeam(targetUserId, teamId);
      return self();
    }

    public AdminStage l_utilisateur_enregistre_est_inactif() {
      ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
      verify(adminRepository).saveUser(captor.capture());
      assertThat(captor.getValue().isActive()).isFalse();
      return self();
    }

    public AdminStage le_planning_exclut_le_mercredi() {
      assertThat(thrownExceptionType).isNull();
      assertThat(savedSchedule.nonWorkingDays()).containsExactly(DayOfWeek.WEDNESDAY);
      return self();
    }

    public AdminStage l_equipe_est_enregistree_en_mode(ValidationMode mode) {
      assertThat(thrownExceptionType).isNull();
      ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
      verify(adminRepository).saveTeam(captor.capture());
      assertThat(captor.getValue().getValidationMode()).isEqualTo(mode);
      assertThat(captor.getValue().getN2ManagerId()).isEqualTo(n2ManagerId);
      return self();
    }

    public AdminStage le_type_est_enregistre_et_actif(String code) {
      assertThat(thrownExceptionType).isNull();
      ArgumentCaptor<AbsenceType> captor = ArgumentCaptor.forClass(AbsenceType.class);
      verify(adminRepository).saveAbsenceType(captor.capture());
      assertThat(captor.getValue().getCode()).isEqualTo(code);
      assertThat(captor.getValue().isActive()).isTrue();
      return self();
    }

    public AdminStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
      assertThat(thrownExceptionType).isEqualTo(type);
      return self();
    }

    private ManageUsers manageUsers() {
      return new ManageUsers(adminRepository, authorizationService);
    }

    private void run(Runnable action) {
      try {
        action.run();
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
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

    private AbsenceType absenceType(UUID id, String code) {
      return new AbsenceType(
          id,
          code,
          "Libellé " + code,
          "#4CAF50",
          true,
          false,
          Set.of(UserRole.SALARIE),
          true,
          true,
          false,
          true);
    }
  }
}
