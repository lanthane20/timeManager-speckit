package fr.timemanager.application.absence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.application.payroll.PeriodClosureRepository;
import fr.timemanager.application.payroll.RegularisationDetector;
import fr.timemanager.application.payroll.RegularisationRepository;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.absencetype.AbsenceType;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Scénarios 1 à 3 de US4 — EF-007, EF-008 : modification d'une absence validée. */
class AbsenceModificationTest
    extends SimpleScenarioTest<AbsenceModificationTest.ModificationStage> {

  @Test
  void scenario1_modificationDUneAbsenceValidee_creeUneVersionEnAttente() {
    given().une_absence_validee_du_salarie();

    when()
        .le_salarie_demande_la_modification_du(
            LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13));

    then()
        .une_nouvelle_version_en_attente_existe()
        .and()
        .l_ancienne_version_reste_active_et_validee()
        .and()
        .un_evenement_d_audit_de_type(AuditActionType.MODIFICATION_DEMANDEE);
  }

  @Test
  void scenario2_validationDeLaNouvelleVersion_archiveLAncienne() {
    given()
        .une_absence_validee_du_salarie()
        .puis_une_demande_de_modification(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13));

    when().le_manager_valide_la_demande();

    then()
        .la_nouvelle_version_est_la_reference_active()
        .and()
        .l_ancienne_version_est_archivee()
        .and()
        .le_statut_simplifie_est(AbsenceSimplifiedStatus.VALIDEE);
  }

  @Test
  void scenario3_refusDeLaNouvelleVersion_laisseLAncienneActive() {
    given()
        .une_absence_validee_du_salarie()
        .puis_une_demande_de_modification(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13));

    when().le_manager_refuse_la_demande_avec_le_motif("Effectif insuffisant");

    then()
        .l_ancienne_version_reste_active_et_validee()
        .and()
        .le_statut_simplifie_est(AbsenceSimplifiedStatus.VALIDEE);
  }

  @Test
  void ef007_modificationDUneDemandeNonValidee_estRefusee() {
    given().une_demande_en_attente_du_salarie();

    when()
        .le_salarie_demande_la_modification_du(
            LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13));

    then().l_operation_echoue_avec(IllegalStateException.class);
  }

  @Test
  void ef007_deuxiemeModificationConcurrente_estRefusee() {
    given()
        .une_absence_validee_du_salarie()
        .puis_une_demande_de_modification(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13));

    when()
        .le_salarie_demande_la_modification_du(
            LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 17));

    then().l_operation_echoue_avec(IllegalStateException.class);
  }

  @Test
  void modificationDeLAbsenceDUnAutreSalarie_estRefusee() {
    given().une_absence_validee_d_un_autre_salarie();

    when()
        .le_salarie_demande_la_modification_du(
            LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13));

    then().l_operation_echoue_avec(SecurityException.class);
  }

  public static class ModificationStage extends Stage<ModificationStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);
    private final PublicHolidayRepository publicHolidayRepository =
        mock(PublicHolidayRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final AuditRepository auditRepository = mock(AuditRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuthorizationService authorizationService = new AuthorizationService();
    private final PeriodClosureRepository periodClosureRepository =
        mock(PeriodClosureRepository.class);
    private final RegularisationDetector regularisationDetector =
        new RegularisationDetector(periodClosureRepository, mock(RegularisationRepository.class));

    private final UUID employeeId = UUID.randomUUID();
    private final UUID managerId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final User manager = manager();

    private UUID absenceId;
    private Absence absence;
    private VersionAbsence originalVersion;
    private Class<? extends RuntimeException> thrownExceptionType;

    public ModificationStage une_absence_validee_du_salarie() {
      buildAbsence(employeeId, AbsenceVersionStatus.VALIDEE);
      return self();
    }

    public ModificationStage une_demande_en_attente_du_salarie() {
      buildAbsence(employeeId, AbsenceVersionStatus.EN_ATTENTE);
      return self();
    }

    public ModificationStage une_absence_validee_d_un_autre_salarie() {
      buildAbsence(UUID.randomUUID(), AbsenceVersionStatus.VALIDEE);
      return self();
    }

    public ModificationStage puis_une_demande_de_modification(LocalDate start, LocalDate end) {
      requestModification(start, end);
      Mockito.reset(auditRepository, notificationService);
      return self();
    }

    public ModificationStage le_salarie_demande_la_modification_du(LocalDate start, LocalDate end) {
      requestModification(start, end);
      return self();
    }

    public ModificationStage le_manager_valide_la_demande() {
      run(
          () ->
              new ValidateAbsence(
                      absenceRepository,
                      teamRepository,
                      auditRepository,
                      notificationService,
                      authorizationService,
                      regularisationDetector)
                  .execute(manager, absenceId, UUID.randomUUID().toString()));
      return self();
    }

    public ModificationStage le_manager_refuse_la_demande_avec_le_motif(String reason) {
      run(
          () ->
              new RefuseAbsence(
                      absenceRepository,
                      teamRepository,
                      auditRepository,
                      notificationService,
                      authorizationService)
                  .execute(manager, absenceId, reason, UUID.randomUUID().toString()));
      return self();
    }

    public ModificationStage une_nouvelle_version_en_attente_existe() {
      assertThat(thrownExceptionType).isNull();
      assertThat(absence.getPendingVersion()).isPresent();
      assertThat(absence.getPendingVersion().orElseThrow().getVersionNumber()).isEqualTo(2);
      return self();
    }

    public ModificationStage l_ancienne_version_reste_active_et_validee() {
      assertThat(originalVersion.isActive()).isTrue();
      assertThat(originalVersion.getStatus()).isEqualTo(AbsenceVersionStatus.VALIDEE);
      return self();
    }

    public ModificationStage la_nouvelle_version_est_la_reference_active() {
      VersionAbsence active = absence.getActiveVersion();
      assertThat(active.getVersionNumber()).isEqualTo(2);
      assertThat(active.getStatus()).isEqualTo(AbsenceVersionStatus.VALIDEE);
      return self();
    }

    public ModificationStage l_ancienne_version_est_archivee() {
      assertThat(originalVersion.isActive()).isFalse();
      return self();
    }

    public ModificationStage le_statut_simplifie_est(AbsenceSimplifiedStatus status) {
      assertThat(absence.getSimplifiedStatus()).isEqualTo(status);
      return self();
    }

    public ModificationStage un_evenement_d_audit_de_type(AuditActionType actionType) {
      ArgumentCaptor<EvenementAudit> captor = ArgumentCaptor.forClass(EvenementAudit.class);
      verify(auditRepository).save(captor.capture());
      assertThat(captor.getValue().actionType()).isEqualTo(actionType);
      assertThat(captor.getValue().valueBefore()).isNotBlank();
      assertThat(captor.getValue().valueAfter()).isNotBlank();
      return self();
    }

    public ModificationStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
      assertThat(thrownExceptionType).isEqualTo(type);
      return self();
    }

    private void requestModification(LocalDate start, LocalDate end) {
      var useCase =
          new RequestAbsenceModification(
              absenceRepository,
              absenceTypeRepository,
              publicHolidayRepository,
              teamRepository,
              auditRepository,
              notificationService);
      var request =
          new CreateAbsenceRequestDto(
              absence.getAbsenceTypeId(), start, end, HalfDayType.NONE, HalfDayType.NONE, null);
      run(() -> useCase.execute(employeeId, absenceId, request, UUID.randomUUID().toString()));
    }

    private void run(Runnable action) {
      try {
        action.run();
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
    }

    private void buildAbsence(UUID ownerId, AbsenceVersionStatus status) {
      absenceId = UUID.randomUUID();
      UUID absenceTypeId = UUID.randomUUID();
      Instant now = Instant.now();
      originalVersion =
          new VersionAbsence(
              UUID.randomUUID(),
              absenceId,
              1,
              new DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6)),
              HalfDayType.NONE,
              HalfDayType.NONE,
              new BigDecimal("5.0"),
              status,
              ownerId,
              true,
              now);
      absence =
          new Absence(
              absenceId, ownerId, absenceTypeId, teamId, AbsenceSimplifiedStatus.from(status), now);
      absence.addVersion(originalVersion);

      Mockito.when(absenceRepository.findById(absenceId)).thenReturn(Optional.of(absence));
      Mockito.when(absenceRepository.save(any())).thenReturn(absence);
      Mockito.when(publicHolidayRepository.findBetween(any(), any())).thenReturn(List.of());
      Mockito.when(absenceTypeRepository.findById(absenceTypeId))
          .thenReturn(
              Optional.of(
                  new AbsenceType(
                      absenceTypeId,
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
      Mockito.when(teamRepository.findById(teamId))
          .thenReturn(
              Optional.of(
                  new Team(teamId, "Équipe", null, ValidationMode.N1_ONLY, managerId, null, true)));
      Mockito.when(teamRepository.findMemberIdsByTeamId(teamId)).thenReturn(Set.of(ownerId));
    }

    private User manager() {
      return new User(
          managerId,
          "sub_" + managerId,
          managerId + "@timemanager.fr",
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
