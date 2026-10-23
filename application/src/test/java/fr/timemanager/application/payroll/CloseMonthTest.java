package fr.timemanager.application.payroll;

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
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import fr.timemanager.domain.payroll.PeriodStatus;
import fr.timemanager.domain.payroll.PeriodeCloture;
import fr.timemanager.domain.payroll.Regularisation;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDayType;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Scénarios 1 et 2 de US5 — EF-024 (clôture) et EF-025 (régularisation automatique). */
class CloseMonthTest extends SimpleScenarioTest<CloseMonthTest.CloseMonthStage> {

  @Test
  void scenario1_ef024_laRhClotureUnePeriode_enUneSeuleAction() {
    given().une_periode_ouverte(YearMonth.of(2026, 9));

    when().la_rh_cloture_la_periode(YearMonth.of(2026, 9));

    then()
        .la_periode_est_au_statut(PeriodStatus.CLOSED)
        .and()
        .un_evenement_d_audit_de_type(AuditActionType.CLOTURE_PERIODE);
  }

  @Test
  void ef024_uneSecondeClotureDeLaMemePeriode_estRefusee() {
    given().une_periode_deja_cloturee(YearMonth.of(2026, 9));

    when().la_rh_cloture_la_periode(YearMonth.of(2026, 9));

    then().l_operation_echoue_avec(IllegalStateException.class);
  }

  @Test
  void seulLeRoleRh_peutCloturerUnePeriode() {
    given().une_periode_ouverte(YearMonth.of(2026, 9)).mais_l_auteur_est_un_simple_manager();

    when().la_rh_cloture_la_periode(YearMonth.of(2026, 9));

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  @Test
  void scenario2_ef025_uneAbsenceValideeApresCloture_genereUneRegularisation() {
    given().une_periode_deja_cloturee(YearMonth.of(2026, 9));

    when().une_absence_de_cette_periode_est_validee();

    then().une_regularisation_est_enregistree_pour_la_periode();
  }

  @Test
  void ef025_uneAbsenceValideeSurUnePeriodeOuverte_neGenereAucuneRegularisation() {
    given().une_periode_ouverte(YearMonth.of(2026, 9));

    when().une_absence_de_cette_periode_est_validee();

    then().aucune_regularisation_n_est_enregistree();
  }

  public static class CloseMonthStage extends Stage<CloseMonthStage> {

    private final PeriodClosureRepository periodClosureRepository =
        mock(PeriodClosureRepository.class);
    private final RegularisationRepository regularisationRepository =
        mock(RegularisationRepository.class);
    private final AuditRepository auditRepository = mock(AuditRepository.class);
    private final AuthorizationService authorizationService = new AuthorizationService();

    private final UUID hrUserId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private User author = user(hrUserId, Set.of(UserRole.SALARIE, UserRole.RH));

    private PeriodeCloture period;
    private PeriodeCloture savedPeriod;
    private Class<? extends RuntimeException> thrownExceptionType;

    public CloseMonthStage une_periode_ouverte(YearMonth yearMonth) {
      period =
          new PeriodeCloture(UUID.randomUUID(), yearMonth.getYear(), yearMonth.getMonthValue());
      Mockito.when(
              periodClosureRepository.findByYearAndMonth(
                  yearMonth.getYear(), yearMonth.getMonthValue()))
          .thenReturn(Optional.of(period));
      Mockito.when(periodClosureRepository.findClosedPeriodCovering(any()))
          .thenReturn(Optional.empty());
      Mockito.when(periodClosureRepository.save(any()))
          .thenAnswer(invocation -> invocation.<PeriodeCloture>getArgument(0));
      return self();
    }

    public CloseMonthStage une_periode_deja_cloturee(YearMonth yearMonth) {
      une_periode_ouverte(yearMonth);
      period.close(hrUserId, Instant.now());
      Mockito.when(periodClosureRepository.findClosedPeriodCovering(any()))
          .thenReturn(Optional.of(period));
      return self();
    }

    public CloseMonthStage mais_l_auteur_est_un_simple_manager() {
      author = user(hrUserId, Set.of(UserRole.SALARIE, UserRole.MANAGER));
      return self();
    }

    public CloseMonthStage la_rh_cloture_la_periode(YearMonth yearMonth) {
      var useCase = new CloseMonth(periodClosureRepository, auditRepository, authorizationService);
      try {
        savedPeriod = useCase.execute(author, yearMonth, UUID.randomUUID().toString());
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
      return self();
    }

    public CloseMonthStage une_absence_de_cette_periode_est_validee() {
      var detector = new RegularisationDetector(periodClosureRepository, regularisationRepository);
      UUID absenceId = UUID.randomUUID();
      Instant now = Instant.now();
      VersionAbsence version =
          new VersionAbsence(
              UUID.randomUUID(),
              absenceId,
              1,
              new DateRange(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)),
              HalfDayType.NONE,
              HalfDayType.NONE,
              new BigDecimal("2.0"),
              AbsenceVersionStatus.VALIDEE,
              employeeId,
              true,
              now);
      Absence absence =
          new Absence(
              absenceId,
              employeeId,
              UUID.randomUUID(),
              UUID.randomUUID(),
              AbsenceSimplifiedStatus.VALIDEE,
              now);
      absence.addVersion(version);
      detector.detectOnValidation(absence, version);
      return self();
    }

    public CloseMonthStage la_periode_est_au_statut(PeriodStatus status) {
      assertThat(thrownExceptionType).isNull();
      assertThat(savedPeriod.getStatus()).isEqualTo(status);
      assertThat(savedPeriod.getClosedByUserId()).isEqualTo(hrUserId);
      return self();
    }

    public CloseMonthStage un_evenement_d_audit_de_type(AuditActionType actionType) {
      ArgumentCaptor<EvenementAudit> captor = ArgumentCaptor.forClass(EvenementAudit.class);
      verify(auditRepository).save(captor.capture());
      assertThat(captor.getValue().actionType()).isEqualTo(actionType);
      assertThat(captor.getValue().authorRole()).isEqualTo("RH");
      return self();
    }

    public CloseMonthStage une_regularisation_est_enregistree_pour_la_periode() {
      ArgumentCaptor<Regularisation> captor = ArgumentCaptor.forClass(Regularisation.class);
      verify(regularisationRepository).save(captor.capture());
      assertThat(captor.getValue().periodClosureId()).isEqualTo(period.getId());
      assertThat(captor.getValue().employeeId()).isEqualTo(employeeId);
      return self();
    }

    public CloseMonthStage aucune_regularisation_n_est_enregistree() {
      verify(regularisationRepository, never()).save(any());
      return self();
    }

    public CloseMonthStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
      assertThat(thrownExceptionType).isEqualTo(type);
      return self();
    }

    private User user(UUID id, Set<UserRole> roles) {
      return new User(
          id,
          "sub_" + id,
          id + "@timemanager.fr",
          "Rachel",
          "Hubert",
          true,
          null,
          false,
          roles,
          Instant.now());
    }
  }
}
