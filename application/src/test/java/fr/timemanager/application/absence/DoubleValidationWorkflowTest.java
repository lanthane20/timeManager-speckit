package fr.timemanager.application.absence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.application.payroll.PeriodClosureRepository;
import fr.timemanager.application.payroll.RegularisationDetector;
import fr.timemanager.application.payroll.RegularisationRepository;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDayType;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.team.ValidationMode;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Scénarios 1 à 4 de US3 — workflow N+1 puis N+2 (EF-009, EF-012, EF-037). */
class DoubleValidationWorkflowTest
    extends SimpleScenarioTest<DoubleValidationWorkflowTest.DoubleValidationStage> {

  @Test
  void scenario1_equipeEnDoubleValidation_laValidationN1_placeEnAttenteDuN2() {
    given().une_demande_en_attente_dans_une_equipe(ValidationMode.N1_AND_N2);

    when().le_n1_valide_la_demande();

    then()
        .la_version_active_est_au_statut(AbsenceVersionStatus.VALIDEE_N1)
        .and()
        .un_evenement_d_audit_de_type(AuditActionType.VALIDATION_N1)
        .and()
        .le_n2_est_notifie_de_la_demande_en_attente()
        .and()
        .le_salarie_n_est_pas_encore_notifie_de_la_validation();
  }

  @Test
  void scenario2_leN2_valide_apres_le_n1_la_demande_devient_validee() {
    given()
        .une_demande_en_attente_dans_une_equipe(ValidationMode.N1_AND_N2)
        .deja_validee_par_le_n1();

    when().le_n2_valide_la_demande();

    then()
        .la_version_active_est_au_statut(AbsenceVersionStatus.VALIDEE)
        .and()
        .un_evenement_d_audit_de_type(AuditActionType.VALIDATION_N2)
        .and()
        .le_salarie_est_notifie_de_la_validation();
  }

  @Test
  void scenario3_ef012_leN2_peut_court_circuiter_le_n1() {
    given().une_demande_en_attente_dans_une_equipe(ValidationMode.N1_AND_N2);

    when().le_n2_valide_la_demande();

    then()
        .la_version_active_est_au_statut(AbsenceVersionStatus.VALIDEE)
        .and()
        .un_evenement_d_audit_de_type(AuditActionType.VALIDATION_N2);
  }

  @Test
  void scenario4_equipeEnValidationSimple_laValidationN1_estFinale() {
    given().une_demande_en_attente_dans_une_equipe(ValidationMode.N1_ONLY);

    when().le_n1_valide_la_demande();

    then()
        .la_version_active_est_au_statut(AbsenceVersionStatus.VALIDEE)
        .and()
        .un_evenement_d_audit_de_type(AuditActionType.VALIDATION_DIRECTE)
        .and()
        .le_salarie_est_notifie_de_la_validation();
  }

  @Test
  void leStatutVuParLeSalarie_resteEnAttente_tantQueLeN2_naPasValide() {
    given().une_demande_en_attente_dans_une_equipe(ValidationMode.N1_AND_N2);

    when().le_n1_valide_la_demande();

    then().le_statut_simplifie_est(AbsenceSimplifiedStatus.EN_ATTENTE);
  }

  @Test
  void validationN2_surUneEquipeEnValidationSimple_estRefusee() {
    given().une_demande_en_attente_dans_une_equipe(ValidationMode.N1_ONLY);

    when().le_n2_valide_la_demande();

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  @Test
  void validationN2_parUnManagerQuiNestPasLeN2_estRefusee() {
    given().une_demande_en_attente_dans_une_equipe(ValidationMode.N1_AND_N2);

    when().le_n1_tente_une_validation_n2();

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  @Test
  void lesFilesDeValidation_separent_le_premier_et_le_second_niveau() {
    given()
        .une_demande_en_attente_dans_une_equipe(ValidationMode.N1_AND_N2)
        .deja_validee_par_le_n1();

    when().les_managers_consultent_leurs_files();

    then().la_file_n1_contient(0).and().la_file_n2_contient(1);
  }

  public static class DoubleValidationStage extends Stage<DoubleValidationStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final AuditRepository auditRepository = mock(AuditRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuthorizationService authorizationService = new AuthorizationService();
    private final PeriodClosureRepository periodClosureRepository =
        mock(PeriodClosureRepository.class);
    private final RegularisationDetector regularisationDetector =
        new RegularisationDetector(periodClosureRepository, mock(RegularisationRepository.class));

    private final UUID n1ManagerId = UUID.randomUUID();
    private final UUID n2ManagerId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final User n1Manager = manager(n1ManagerId);
    private final User n2Manager = manager(n2ManagerId);

    private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);

    private UUID absenceId;
    private Absence absence;
    private int n1QueueSize;
    private int n2QueueSize;
    private Class<? extends RuntimeException> thrownExceptionType;

    public DoubleValidationStage une_demande_en_attente_dans_une_equipe(ValidationMode mode) {
      buildAbsence(AbsenceVersionStatus.EN_ATTENTE, mode);
      return self();
    }

    public DoubleValidationStage deja_validee_par_le_n1() {
      absence.getActiveVersion().validateN1();
      absence.updateSimplifiedStatus();
      return self();
    }

    public DoubleValidationStage le_n1_valide_la_demande() {
      run(() -> validateAbsence().execute(n1Manager, absenceId, UUID.randomUUID().toString()));
      return self();
    }

    public DoubleValidationStage le_n2_valide_la_demande() {
      run(() -> validateAbsenceN2().execute(n2Manager, absenceId, UUID.randomUUID().toString()));
      return self();
    }

    public DoubleValidationStage le_n1_tente_une_validation_n2() {
      run(() -> validateAbsenceN2().execute(n1Manager, absenceId, UUID.randomUUID().toString()));
      return self();
    }

    public DoubleValidationStage les_managers_consultent_leurs_files() {
      Mockito.when(teamRepository.findTeamsByManagerId(any()))
          .thenReturn(
              java.util.List.of(
                  new Team(
                      teamId,
                      "Équipe",
                      null,
                      ValidationMode.N1_AND_N2,
                      n1ManagerId,
                      n2ManagerId,
                      true)));
      Mockito.when(
              absenceRepository.findByTeamIdAndStatus(teamId, AbsenceSimplifiedStatus.EN_ATTENTE))
          .thenReturn(java.util.List.of(absence));
      Mockito.when(absenceTypeRepository.findById(any()))
          .thenReturn(
              Optional.of(
                  new fr.timemanager.domain.absencetype.AbsenceType(
                      UUID.randomUUID(),
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

      var useCase =
          new GetPendingAbsencesForManager(
              absenceRepository, absenceTypeRepository, teamRepository, authorizationService);
      n1QueueSize = useCase.execute(n1Manager).size();
      n2QueueSize = useCase.executeForSecondLevel(n2Manager).size();
      return self();
    }

    public DoubleValidationStage la_file_n1_contient(int expected) {
      assertThat(n1QueueSize).isEqualTo(expected);
      return self();
    }

    public DoubleValidationStage la_file_n2_contient(int expected) {
      assertThat(n2QueueSize).isEqualTo(expected);
      return self();
    }

    public DoubleValidationStage la_version_active_est_au_statut(AbsenceVersionStatus status) {
      assertThat(thrownExceptionType).isNull();
      assertThat(absence.getActiveVersion().getStatus()).isEqualTo(status);
      return self();
    }

    public DoubleValidationStage le_statut_simplifie_est(AbsenceSimplifiedStatus status) {
      assertThat(absence.getSimplifiedStatus()).isEqualTo(status);
      return self();
    }

    public DoubleValidationStage un_evenement_d_audit_de_type(AuditActionType actionType) {
      ArgumentCaptor<EvenementAudit> captor = ArgumentCaptor.forClass(EvenementAudit.class);
      verify(auditRepository).save(captor.capture());
      assertThat(captor.getValue().actionType()).isEqualTo(actionType);
      return self();
    }

    public DoubleValidationStage le_n2_est_notifie_de_la_demande_en_attente() {
      verify(notificationService).notifyPendingValidationToManager(n2ManagerId, absenceId);
      return self();
    }

    public DoubleValidationStage le_salarie_est_notifie_de_la_validation() {
      verify(notificationService).notifyAbsenceValidated(employeeId, absenceId);
      return self();
    }

    public DoubleValidationStage le_salarie_n_est_pas_encore_notifie_de_la_validation() {
      verify(notificationService, never()).notifyAbsenceValidated(employeeId, absenceId);
      return self();
    }

    public DoubleValidationStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
      assertThat(thrownExceptionType).isEqualTo(type);
      return self();
    }

    private ValidateAbsence validateAbsence() {
      return new ValidateAbsence(
          absenceRepository,
          teamRepository,
          auditRepository,
          notificationService,
          authorizationService,
          regularisationDetector);
    }

    private ValidateAbsenceN2 validateAbsenceN2() {
      return new ValidateAbsenceN2(
          absenceRepository,
          teamRepository,
          auditRepository,
          notificationService,
          authorizationService,
          regularisationDetector);
    }

    private void run(Runnable action) {
      try {
        action.run();
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
    }

    private void buildAbsence(AbsenceVersionStatus status, ValidationMode mode) {
      absenceId = UUID.randomUUID();
      Instant now = Instant.now();
      VersionAbsence version =
          new VersionAbsence(
              UUID.randomUUID(),
              absenceId,
              1,
              new DateRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9)),
              HalfDayType.NONE,
              HalfDayType.NONE,
              new BigDecimal("5.0"),
              status,
              employeeId,
              true,
              now);
      absence =
          new Absence(
              absenceId,
              employeeId,
              UUID.randomUUID(),
              teamId,
              AbsenceSimplifiedStatus.from(status),
              now);
      absence.addVersion(version);

      Mockito.when(absenceRepository.findById(absenceId)).thenReturn(Optional.of(absence));
      Mockito.when(absenceRepository.save(any())).thenReturn(absence);
      Mockito.when(teamRepository.findById(teamId))
          .thenReturn(
              Optional.of(new Team(teamId, "Équipe", null, mode, n1ManagerId, n2ManagerId, true)));
      Mockito.when(teamRepository.findMemberIdsByTeamId(teamId)).thenReturn(Set.of(employeeId));
    }

    private User manager(UUID id) {
      return new User(
          id,
          "sub_" + id,
          id + "@timemanager.fr",
          "Manon",
          "Dubois",
          true,
          null,
          false,
          Set.of(UserRole.SALARIE, UserRole.MANAGER),
          Instant.now());
    }
  }
}
