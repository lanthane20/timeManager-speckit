package fr.timemanager.application.absence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.authorization.SelfApprovalException;
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

/** Scénarios d'acceptation 1, 2 et 3 de US2 — EF-009, EF-010, EF-011. */
class ValidationUseCasesTest extends SimpleScenarioTest<ValidationUseCasesTest.ValidationStage> {

  @Test
  void scenario1_demandeEnAttente_valideeParLeManager_passeAuStatutValidee() {
    given().une_demande_en_attente_dans_le_perimetre_du_manager();

    when().le_manager_valide_la_demande();

    then()
        .la_demande_passe_au_statut(AbsenceSimplifiedStatus.VALIDEE)
        .and()
        .un_evenement_d_audit_de_type(AuditActionType.VALIDATION_DIRECTE)
        .and()
        .le_salarie_est_notifie_de_la_validation();
  }

  @Test
  void scenario2_refusAvecMotif_passeAuStatutRefusee() {
    given().une_demande_en_attente_dans_le_perimetre_du_manager();

    when().le_manager_refuse_la_demande_avec_le_motif("Effectif insuffisant sur la période");

    then()
        .la_demande_passe_au_statut(AbsenceSimplifiedStatus.REFUSEE)
        .and()
        .un_evenement_d_audit_de_type(AuditActionType.REFUS)
        .and()
        .le_salarie_est_notifie_du_refus();
  }

  @Test
  void ef011_refusSansMotif_estRefuseParLeSysteme() {
    given().une_demande_en_attente_dans_le_perimetre_du_manager();

    when().le_manager_refuse_la_demande_avec_le_motif("   ");

    then().l_operation_echoue_avec(IllegalArgumentException.class);
  }

  @Test
  void ef010_managerNePeutPasValiderSaPropreDemande() {
    given().une_demande_du_manager_lui_meme();

    when().le_manager_valide_la_demande();

    then().l_operation_echoue_avec(SelfApprovalException.class);
  }

  @Test
  void scenario3_demandeHorsPerimetre_estRefusee() {
    given().une_demande_hors_du_perimetre_du_manager();

    when().le_manager_valide_la_demande();

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  @Test
  void fileDeValidation_neRemonteQueLesDemandesEnAttenteDuPerimetre() {
    given().une_demande_en_attente_dans_le_perimetre_du_manager();

    when().le_manager_consulte_sa_file_de_validation();

    then().la_file_contient(1);
  }

  public static class ValidationStage extends Stage<ValidationStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final AuditRepository auditRepository = mock(AuditRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuthorizationService authorizationService = new AuthorizationService();
    private final PeriodClosureRepository periodClosureRepository =
        mock(PeriodClosureRepository.class);
    private final RegularisationDetector regularisationDetector =
        new RegularisationDetector(periodClosureRepository, mock(RegularisationRepository.class));

    private final UUID managerId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final User manager = TestUsers.manager(managerId);

    private UUID employeeId;
    private UUID absenceId;
    private Absence absence;
    private int queueSize;
    private Class<? extends RuntimeException> thrownExceptionType;

    public ValidationStage une_demande_en_attente_dans_le_perimetre_du_manager() {
      employeeId = UUID.randomUUID();
      buildAbsence(employeeId, AbsenceVersionStatus.EN_ATTENTE, Set.of(employeeId));
      return self();
    }

    public ValidationStage une_demande_du_manager_lui_meme() {
      employeeId = managerId;
      buildAbsence(employeeId, AbsenceVersionStatus.EN_ATTENTE, Set.of(employeeId));
      return self();
    }

    public ValidationStage une_demande_hors_du_perimetre_du_manager() {
      employeeId = UUID.randomUUID();
      buildAbsence(employeeId, AbsenceVersionStatus.EN_ATTENTE, Set.of(UUID.randomUUID()));
      return self();
    }

    public ValidationStage le_manager_valide_la_demande() {
      var useCase =
          new ValidateAbsence(
              absenceRepository,
              teamRepository,
              auditRepository,
              notificationService,
              authorizationService,
              regularisationDetector);
      run(() -> useCase.execute(manager, absenceId, UUID.randomUUID().toString()));
      return self();
    }

    public ValidationStage le_manager_refuse_la_demande_avec_le_motif(String reason) {
      var useCase =
          new RefuseAbsence(
              absenceRepository,
              teamRepository,
              auditRepository,
              notificationService,
              authorizationService);
      run(() -> useCase.execute(manager, absenceId, reason, UUID.randomUUID().toString()));
      return self();
    }

    public ValidationStage le_manager_consulte_sa_file_de_validation() {
      Mockito.when(teamRepository.findTeamsByManagerId(managerId))
          .thenReturn(java.util.List.of(team(ValidationMode.N1_ONLY)));
      Mockito.when(
              absenceRepository.findByTeamIdAndStatus(teamId, AbsenceSimplifiedStatus.EN_ATTENTE))
          .thenReturn(java.util.List.of(absence));
      Mockito.when(absenceTypeRepository.findById(any()))
          .thenReturn(Optional.of(TestUsers.absenceTypeCp()));

      var useCase =
          new GetPendingAbsencesForManager(
              absenceRepository, absenceTypeRepository, teamRepository, authorizationService);
      queueSize = useCase.execute(manager).size();
      return self();
    }

    public ValidationStage la_file_contient(int expected) {
      assertThat(thrownExceptionType).isNull();
      assertThat(queueSize).isEqualTo(expected);
      return self();
    }

    public ValidationStage la_demande_passe_au_statut(AbsenceSimplifiedStatus status) {
      assertThat(thrownExceptionType).isNull();
      assertThat(absence.getSimplifiedStatus()).isEqualTo(status);
      return self();
    }

    public ValidationStage un_evenement_d_audit_de_type(AuditActionType actionType) {
      ArgumentCaptor<EvenementAudit> captor = ArgumentCaptor.forClass(EvenementAudit.class);
      verify(auditRepository).save(captor.capture());
      assertThat(captor.getValue().actionType()).isEqualTo(actionType);
      assertThat(captor.getValue().authorId()).isEqualTo(managerId.toString());
      return self();
    }

    public ValidationStage le_salarie_est_notifie_de_la_validation() {
      verify(notificationService).notifyAbsenceValidated(employeeId, absenceId);
      return self();
    }

    public ValidationStage le_salarie_est_notifie_du_refus() {
      verify(notificationService)
          .notifyAbsenceRefused(Mockito.eq(employeeId), Mockito.eq(absenceId), any());
      return self();
    }

    public ValidationStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
      assertThat(thrownExceptionType).isEqualTo(type);
      return self();
    }

    private void run(Runnable action) {
      try {
        action.run();
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
    }

    private Team team(ValidationMode mode) {
      return new Team(teamId, "Équipe Test", null, mode, managerId, null, true);
    }

    private void buildAbsence(UUID ownerId, AbsenceVersionStatus status, Set<UUID> memberIds) {
      absenceId = UUID.randomUUID();
      Instant now = Instant.now();
      VersionAbsence version =
          new VersionAbsence(
              UUID.randomUUID(),
              absenceId,
              1,
              new DateRange(LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23)),
              HalfDayType.NONE,
              HalfDayType.NONE,
              new BigDecimal("2.0"),
              status,
              ownerId,
              true,
              now);
      absence =
          new Absence(
              absenceId,
              ownerId,
              UUID.randomUUID(),
              teamId,
              AbsenceSimplifiedStatus.from(status),
              now);
      absence.addVersion(version);

      Mockito.when(absenceRepository.findById(absenceId)).thenReturn(Optional.of(absence));
      Mockito.when(absenceRepository.save(any())).thenReturn(absence);
      Mockito.when(teamRepository.findById(teamId))
          .thenReturn(Optional.of(team(ValidationMode.N1_ONLY)));
      Mockito.when(teamRepository.findMemberIdsByTeamId(teamId)).thenReturn(memberIds);
    }
  }

  /** Fabriques partagées par les scénarios de validation. */
  static final class TestUsers {

    private TestUsers() {}

    static User manager(UUID id) {
      return new User(
          id,
          "sub_" + id,
          "manager@timemanager.fr",
          "Manon",
          "Dubois",
          true,
          null,
          false,
          Set.of(UserRole.SALARIE, UserRole.MANAGER),
          Instant.now());
    }

    static fr.timemanager.domain.absencetype.AbsenceType absenceTypeCp() {
      return new fr.timemanager.domain.absencetype.AbsenceType(
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
          true);
    }
  }
}
