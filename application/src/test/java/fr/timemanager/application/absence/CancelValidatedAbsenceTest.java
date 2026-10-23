package fr.timemanager.application.absence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.notification.NotificationService;
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

/** EF-013 : annulation par le manager d'une absence validée, justification obligatoire. */
class CancelValidatedAbsenceTest
    extends SimpleScenarioTest<CancelValidatedAbsenceTest.CancelValidatedStage> {

  @Test
  void ef013_annulationAvecJustification_passeAuStatutAnnulee() {
    given().une_absence_validee_dans_le_perimetre_du_manager();

    when().le_manager_annule_avec_la_justification("Fermeture exceptionnelle du site");

    then()
        .l_absence_passe_au_statut(AbsenceSimplifiedStatus.ANNULEE)
        .and()
        .un_evenement_d_audit_de_type(AuditActionType.ANNULATION_MANAGER)
        .and()
        .le_salarie_est_notifie_de_l_annulation();
  }

  @Test
  void ef013_annulationSansJustification_estRefusee() {
    given().une_absence_validee_dans_le_perimetre_du_manager();

    when().le_manager_annule_avec_la_justification("  ");

    then().l_operation_echoue_avec(IllegalArgumentException.class);
  }

  @Test
  void ef013_annulationHorsPerimetre_estRefusee() {
    given().une_absence_validee_hors_du_perimetre_du_manager();

    when().le_manager_annule_avec_la_justification("Motif quelconque");

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  @Test
  void ef013_annulationDUneDemandeNonValidee_estRefusee() {
    given().une_demande_en_attente_dans_le_perimetre_du_manager();

    when().le_manager_annule_avec_la_justification("Motif quelconque");

    then().l_operation_echoue_avec(IllegalStateException.class);
  }

  public static class CancelValidatedStage extends Stage<CancelValidatedStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final AuditRepository auditRepository = mock(AuditRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuthorizationService authorizationService = new AuthorizationService();

    private final UUID managerId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final User manager =
        new User(
            managerId,
            "sub_" + managerId,
            "manager@timemanager.fr",
            "Manon",
            "Dubois",
            true,
            null,
            false,
            Set.of(UserRole.SALARIE, UserRole.MANAGER),
            Instant.now());

    private UUID absenceId;
    private Absence absence;
    private Class<? extends RuntimeException> thrownExceptionType;

    public CancelValidatedStage une_absence_validee_dans_le_perimetre_du_manager() {
      buildAbsence(AbsenceVersionStatus.VALIDEE, Set.of(employeeId));
      return self();
    }

    public CancelValidatedStage une_absence_validee_hors_du_perimetre_du_manager() {
      buildAbsence(AbsenceVersionStatus.VALIDEE, Set.of(UUID.randomUUID()));
      return self();
    }

    public CancelValidatedStage une_demande_en_attente_dans_le_perimetre_du_manager() {
      buildAbsence(AbsenceVersionStatus.EN_ATTENTE, Set.of(employeeId));
      return self();
    }

    public CancelValidatedStage le_manager_annule_avec_la_justification(String reason) {
      var useCase =
          new CancelValidatedAbsence(
              absenceRepository,
              teamRepository,
              auditRepository,
              notificationService,
              authorizationService);
      try {
        useCase.execute(manager, absenceId, reason, UUID.randomUUID().toString());
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
      return self();
    }

    public CancelValidatedStage l_absence_passe_au_statut(AbsenceSimplifiedStatus status) {
      assertThat(thrownExceptionType).isNull();
      assertThat(absence.getSimplifiedStatus()).isEqualTo(status);
      return self();
    }

    public CancelValidatedStage un_evenement_d_audit_de_type(AuditActionType actionType) {
      ArgumentCaptor<EvenementAudit> captor = ArgumentCaptor.forClass(EvenementAudit.class);
      verify(auditRepository).save(captor.capture());
      assertThat(captor.getValue().actionType()).isEqualTo(actionType);
      return self();
    }

    public CancelValidatedStage le_salarie_est_notifie_de_l_annulation() {
      verify(notificationService)
          .notifyAbsenceCancelledByManager(Mockito.eq(employeeId), Mockito.eq(absenceId), any());
      return self();
    }

    public CancelValidatedStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
      assertThat(thrownExceptionType).isEqualTo(type);
      return self();
    }

    private void buildAbsence(AbsenceVersionStatus status, Set<UUID> memberIds) {
      absenceId = UUID.randomUUID();
      Instant now = Instant.now();
      VersionAbsence version =
          new VersionAbsence(
              UUID.randomUUID(),
              absenceId,
              1,
              new DateRange(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10)),
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
              Optional.of(
                  new Team(teamId, "Équipe", null, ValidationMode.N1_ONLY, managerId, null, true)));
      Mockito.when(teamRepository.findMemberIdsByTeamId(teamId)).thenReturn(memberIds);
    }
  }
}
