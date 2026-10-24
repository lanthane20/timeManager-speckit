package fr.timemanager.application.absence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.domain.absence.AbsenceOverlapException;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.ExistingAbsenceInfo;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.domain.user.WorkSchedule;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Scénarios d'acceptation 1, 3, 4, 5 de US1 (EF-001 à EF-005). */
class CreateAbsenceRequestTest
    extends SimpleScenarioTest<CreateAbsenceRequestTest.CreateAbsenceStage> {

  @Test
  void scenario1_typeValideEtDatesValides_creeUneDemandeEnAttente() {
    given()
        .un_type_d_absence_valide("Congés payés", false)
        .and()
        .aucune_absence_existante_sur_la_periode()
        .and()
        .aucun_jour_ferie_sur_la_periode();

    when().le_salarie_cree_une_demande_du(LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23));

    then()
        .la_demande_est_creee_avec_le_statut(AbsenceSimplifiedStatus.EN_ATTENTE)
        .and()
        .une_notification_de_creation_est_envoyee();
  }

  @Test
  void scenario3_periodeAvecWeekendEtJourFerie_excluCesJoursDuCalcul() {
    given()
        .un_type_d_absence_valide("Congés payés", false)
        .and()
        .aucune_absence_existante_sur_la_periode()
        .and()
        .un_jour_ferie_le(LocalDate.of(2026, 7, 14));

    when().le_salarie_cree_une_demande_du(LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 20));

    then().le_nombre_de_jours_ouvres_calcule_est(new BigDecimal("5.0"));
  }

  @Test
  void ef031b_salarieAMiTemps_leJourNonTravailleContractuelNestPasDecompte() {
    given()
        .un_type_d_absence_valide("Congés payés", false)
        .and()
        .aucune_absence_existante_sur_la_periode()
        .and()
        .aucun_jour_ferie_sur_la_periode()
        .and()
        .un_planning_contractuel_sans_le_mercredi_depuis(LocalDate.of(2026, 1, 1));

    // Du lundi 22 au vendredi 26 juin 2026 : 5 jours ouvrés, dont le mercredi non travaillé.
    when().le_salarie_cree_une_demande_du(LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 26));

    then().le_nombre_de_jours_ouvres_calcule_est(new BigDecimal("4.0"));
  }

  @Test
  void scenario4_absenceExistanteQuiChevauche_bloqueLaCreation() {
    given()
        .un_type_d_absence_valide("Congés payés", false)
        .and()
        .une_absence_existante_sur_la_periode(
            new DateRange(LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 26)), false)
        .and()
        .aucun_jour_ferie_sur_la_periode();

    when()
        .le_salarie_tente_de_creer_une_demande_du(
            LocalDate.of(2026, 6, 24), LocalDate.of(2026, 6, 28));

    then().la_creation_est_bloquee_par_un_conflit();
  }

  @Test
  void scenario5_typeQuantifie_permetLaCreationEtAlimenteraLeCompteur() {
    given()
        .un_type_d_absence_quantifie("RTT")
        .and()
        .aucune_absence_existante_sur_la_periode()
        .and()
        .aucun_jour_ferie_sur_la_periode();

    when().le_salarie_cree_une_demande_du(LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23));

    then()
        .la_demande_est_creee_avec_le_statut(AbsenceSimplifiedStatus.EN_ATTENTE)
        .and()
        .la_demande_porte_sur_le_type_quantifie();
  }

  public static class CreateAbsenceStage extends Stage<CreateAbsenceStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);
    private final PublicHolidayRepository publicHolidayRepository =
        mock(PublicHolidayRepository.class);
    private final AuditRepository auditRepository = mock(AuditRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final WorkScheduleRepository workScheduleRepository =
        mock(WorkScheduleRepository.class);

    private final UUID employeeId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private UUID absenceTypeId;

    private AbsenceResponseDto result;
    private RuntimeException thrownException;

    public CreateAbsenceStage un_type_d_absence_valide(String label, boolean quantified) {
      absenceTypeId = UUID.randomUUID();
      AbsenceType type =
          new AbsenceType(
              absenceTypeId,
              "CP",
              label,
              "#0000FF",
              false,
              false,
              Set.of(UserRole.SALARIE),
              quantified,
              true,
              false,
              true);
      Mockito.when(absenceTypeRepository.findById(absenceTypeId)).thenReturn(Optional.of(type));
      return self();
    }

    public CreateAbsenceStage un_type_d_absence_quantifie(String label) {
      return un_type_d_absence_valide(label, true);
    }

    public CreateAbsenceStage aucune_absence_existante_sur_la_periode() {
      Mockito.when(absenceRepository.findOverlappingInfo(any(), any(), any()))
          .thenReturn(List.of());
      return self();
    }

    public CreateAbsenceStage une_absence_existante_sur_la_periode(
        DateRange range, boolean allowsOverlap) {
      Mockito.when(absenceRepository.findOverlappingInfo(any(), any(), any()))
          .thenReturn(List.of(new ExistingAbsenceInfo(range, allowsOverlap)));
      return self();
    }

    public CreateAbsenceStage aucun_jour_ferie_sur_la_periode() {
      Mockito.when(publicHolidayRepository.findBetween(any(), any())).thenReturn(List.of());
      return self();
    }

    public CreateAbsenceStage un_jour_ferie_le(LocalDate date) {
      Mockito.when(publicHolidayRepository.findBetween(any(), any())).thenReturn(List.of(date));
      return self();
    }

    public CreateAbsenceStage un_planning_contractuel_sans_le_mercredi_depuis(LocalDate from) {
      var workingDays =
          Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
      Mockito.when(workScheduleRepository.findByUserId(employeeId))
          .thenReturn(
              List.of(
                  new WorkSchedule(
                      UUID.randomUUID(), employeeId, workingDays, from, null, Instant.now())));
      return self();
    }

    public CreateAbsenceStage le_salarie_cree_une_demande_du(LocalDate start, LocalDate end) {
      result = execute(start, end);
      return self();
    }

    public CreateAbsenceStage le_salarie_tente_de_creer_une_demande_du(
        LocalDate start, LocalDate end) {
      try {
        result = execute(start, end);
      } catch (RuntimeException ex) {
        thrownException = ex;
      }
      return self();
    }

    public CreateAbsenceStage la_demande_est_creee_avec_le_statut(AbsenceSimplifiedStatus status) {
      assertThat(result).isNotNull();
      assertThat(result.status()).isEqualTo(status);
      return self();
    }

    public CreateAbsenceStage une_notification_de_creation_est_envoyee() {
      verify(notificationService).notifyAbsenceCreated(eq(employeeId), any());
      return self();
    }

    public CreateAbsenceStage le_nombre_de_jours_ouvres_calcule_est(BigDecimal expected) {
      assertThat(result.workingDays()).isEqualByComparingTo(expected);
      return self();
    }

    public CreateAbsenceStage la_creation_est_bloquee_par_un_conflit() {
      assertThat(thrownException).isInstanceOf(AbsenceOverlapException.class);
      return self();
    }

    public CreateAbsenceStage la_demande_porte_sur_le_type_quantifie() {
      assertThat(result.absenceTypeId()).isEqualTo(absenceTypeId);
      return self();
    }

    private AbsenceResponseDto execute(LocalDate start, LocalDate end) {
      CreateAbsenceRequest useCase =
          new CreateAbsenceRequest(
              absenceRepository,
              absenceTypeRepository,
              publicHolidayRepository,
              auditRepository,
              notificationService,
              teamRepository,
              workScheduleRepository);
      CreateAbsenceRequestDto dto =
          new CreateAbsenceRequestDto(absenceTypeId, start, end, null, null, null);
      return useCase.execute(employeeId, teamId, UUID.randomUUID().toString(), dto);
    }
  }
}
