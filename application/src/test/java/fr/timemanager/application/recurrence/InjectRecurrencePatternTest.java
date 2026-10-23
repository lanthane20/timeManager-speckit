package fr.timemanager.application.recurrence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.absence.PublicHolidayRepository;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.recurrence.InjectRecurrencePattern.InjectionResult;
import fr.timemanager.domain.absence.ExistingAbsenceInfo;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.recurrence.InjectionSource;
import fr.timemanager.domain.recurrence.RecurrenceInjectionLog;
import fr.timemanager.domain.recurrence.RecurrencePattern;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Scénarios 1 à 4 de US6 — EF-028, EF-029, EF-030. */
class InjectRecurrencePatternTest
    extends SimpleScenarioTest<InjectRecurrencePatternTest.RecurrenceStage> {

  @Test
  void scenario1_ef028_lInjectionCreeUneAbsenceParJourOuvre() {
    given().un_patron_ecole_actif_pour_un_alternant();

    when()
        .les_dates_sont_injectees(
            List.of(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)), InjectionSource.IMPORT);

    then().des_absences_sont_creees(2).and().l_injection_est_journalisee(InjectionSource.IMPORT, 2);
  }

  @Test
  void scenario2_ef028_laGenerationPeriodiqueCouvreToutesLesDatesDeLaPeriode() {
    given().un_patron_ecole_actif_pour_un_alternant();

    when()
        .les_dates_sont_injectees(
            List.of(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 21)),
            InjectionSource.GENERATION);

    then()
        .des_absences_sont_creees(3)
        .and()
        .la_periode_journalisee_va_du(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 21));
  }

  @Test
  void scenario3_leChevauchementEstSignaleMaisNeBloquePasLInjection() {
    given().un_patron_ecole_actif_pour_un_alternant().et_une_absence_existante_sur_la_date();

    when().les_dates_sont_injectees(List.of(LocalDate.of(2026, 9, 7)), InjectionSource.SAISIE);

    then().des_absences_sont_creees(1).and().un_avertissement_de_chevauchement_est_remonte();
  }

  @Test
  void lesJoursNonOuvres_neProduisentAucuneAbsence() {
    given().un_patron_ecole_actif_pour_un_alternant();

    // 12 et 13 septembre 2026 tombent un samedi et un dimanche.
    when()
        .les_dates_sont_injectees(
            List.of(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 13)),
            InjectionSource.GENERATION);

    then()
        .des_absences_sont_creees(0)
        .and()
        .l_injection_est_journalisee(InjectionSource.GENERATION, 0);
  }

  @Test
  void ef030_moduleNonActivePourLUtilisateur_estRefuse() {
    given()
        .un_patron_ecole_actif_pour_un_alternant()
        .mais_le_module_n_est_pas_active_pour_l_auteur();

    when().les_dates_sont_injectees(List.of(LocalDate.of(2026, 9, 7)), InjectionSource.SAISIE);

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  @Test
  void ef028_unPatronDesactive_neProduitPlusDInjection() {
    given().un_patron_ecole_desactive();

    when().les_dates_sont_injectees(List.of(LocalDate.of(2026, 9, 7)), InjectionSource.SAISIE);

    then().l_operation_echoue_avec(IllegalStateException.class);
  }

  public static class RecurrenceStage extends Stage<RecurrenceStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);
    private final PublicHolidayRepository publicHolidayRepository =
        mock(PublicHolidayRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final RecurrenceRepository recurrenceRepository = mock(RecurrenceRepository.class);

    private final UUID authorId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID absenceTypeId = UUID.randomUUID();
    private final UUID patternId = UUID.randomUUID();

    private User author = user(authorId, true);
    private InjectionResult result;
    private Class<? extends RuntimeException> thrownExceptionType;

    public RecurrenceStage un_patron_ecole_actif_pour_un_alternant() {
      seedPattern(true);
      return self();
    }

    public RecurrenceStage un_patron_ecole_desactive() {
      seedPattern(false);
      return self();
    }

    public RecurrenceStage et_une_absence_existante_sur_la_date() {
      Mockito.when(absenceRepository.findOverlappingInfo(any(), any(), any()))
          .thenReturn(
              List.of(
                  new ExistingAbsenceInfo(
                      new DateRange(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 7)), false)));
      return self();
    }

    public RecurrenceStage mais_le_module_n_est_pas_active_pour_l_auteur() {
      author = user(authorId, false);
      return self();
    }

    public RecurrenceStage les_dates_sont_injectees(List<LocalDate> dates, InjectionSource source) {
      var useCase =
          new InjectRecurrencePattern(
              absenceRepository,
              absenceTypeRepository,
              publicHolidayRepository,
              teamRepository,
              recurrenceRepository);
      try {
        result = useCase.execute(author, patternId, dates, source);
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
      return self();
    }

    public RecurrenceStage des_absences_sont_creees(int expected) {
      assertThat(thrownExceptionType).isNull();
      verify(absenceRepository, times(expected)).save(any());
      return self();
    }

    public RecurrenceStage l_injection_est_journalisee(InjectionSource source, int eventsCreated) {
      ArgumentCaptor<RecurrenceInjectionLog> captor =
          ArgumentCaptor.forClass(RecurrenceInjectionLog.class);
      verify(recurrenceRepository).saveInjectionLog(captor.capture());
      assertThat(captor.getValue().source()).isEqualTo(source);
      assertThat(captor.getValue().eventsCreatedCount()).isEqualTo(eventsCreated);
      assertThat(captor.getValue().injectedByUserId()).isEqualTo(authorId);
      return self();
    }

    public RecurrenceStage la_periode_journalisee_va_du(LocalDate start, LocalDate end) {
      ArgumentCaptor<RecurrenceInjectionLog> captor =
          ArgumentCaptor.forClass(RecurrenceInjectionLog.class);
      verify(recurrenceRepository).saveInjectionLog(captor.capture());
      assertThat(captor.getValue().periodStart()).isEqualTo(start);
      assertThat(captor.getValue().periodEnd()).isEqualTo(end);
      return self();
    }

    public RecurrenceStage un_avertissement_de_chevauchement_est_remonte() {
      assertThat(result.warnings()).hasSize(1);
      assertThat(result.warnings().getFirst()).contains("Chevauchement");
      return self();
    }

    public RecurrenceStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
      assertThat(thrownExceptionType).isEqualTo(type);
      return self();
    }

    private void seedPattern(boolean active) {
      Mockito.when(recurrenceRepository.findPatternById(patternId))
          .thenReturn(
              Optional.of(
                  new RecurrencePattern(
                      patternId,
                      employeeId,
                      absenceTypeId,
                      "Planning école alternance",
                      active,
                      Instant.now())));
      Mockito.when(absenceTypeRepository.findById(absenceTypeId))
          .thenReturn(
              Optional.of(
                  new AbsenceType(
                      absenceTypeId,
                      "ECOLE",
                      "École",
                      "#FF9800",
                      false,
                      false,
                      Set.of(UserRole.SALARIE),
                      true,
                      true,
                      true,
                      true)));
      Mockito.when(teamRepository.findTeamIdByMemberId(employeeId)).thenReturn(Optional.of(teamId));
      Mockito.when(publicHolidayRepository.findBetween(any(), any())).thenReturn(List.of());
      Mockito.when(absenceRepository.findOverlappingInfo(any(), any(), any()))
          .thenReturn(List.of());
      Mockito.when(recurrenceRepository.saveInjectionLog(any()))
          .thenAnswer(invocation -> invocation.<RecurrenceInjectionLog>getArgument(0));
    }

    private User user(UUID id, boolean recurrenceEnabled) {
      return new User(
          id,
          "sub_" + id,
          id + "@timemanager.fr",
          "Alex",
          "Petit",
          true,
          null,
          recurrenceEnabled,
          Set.of(UserRole.SALARIE),
          Instant.now());
    }
  }
}
