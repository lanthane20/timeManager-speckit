package fr.timemanager.application.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.payroll.HrAbsenceSearchCriteria;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDayType;
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

/** EF-021 : consultation RH de toutes les absences avec filtres cumulables. */
class SearchAbsencesForHRTest extends SimpleScenarioTest<SearchAbsencesForHRTest.SearchStage> {

  @Test
  void ef021_laRhConsulteLesAbsencesSansFiltre() {
    given().des_absences_existantes(2);

    when().la_rh_recherche_sans_filtre();

    then().le_resultat_contient(2);
  }

  @Test
  void ef021_lesFiltresSontTransmisAuRepository() {
    given().des_absences_existantes(1);

    when().la_rh_recherche_par_salarie_equipe_type_et_periode();

    then().les_filtres_transmis_sont_complets();
  }

  @Test
  void ef021_unSalarieNePeutPasConsulterToutesLesAbsences() {
    given().des_absences_existantes(1).mais_l_auteur_est_un_simple_salarie();

    when().la_rh_recherche_sans_filtre();

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  public static class SearchStage extends Stage<SearchStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);
    private final AuthorizationService authorizationService = new AuthorizationService();

    private final UUID hrUserId = UUID.randomUUID();
    private final UUID absenceTypeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private User author = user(hrUserId, Set.of(UserRole.SALARIE, UserRole.RH));

    private List<AbsenceResponseDto> result;
    private Class<? extends RuntimeException> thrownExceptionType;

    public SearchStage des_absences_existantes(int count) {
      List<Absence> absences =
          java.util.stream.IntStream.range(0, count).mapToObj(index -> absence()).toList();
      Mockito.when(absenceRepository.search(any())).thenReturn(absences);
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
      return self();
    }

    public SearchStage mais_l_auteur_est_un_simple_salarie() {
      author = user(hrUserId, Set.of(UserRole.SALARIE));
      return self();
    }

    public SearchStage la_rh_recherche_sans_filtre() {
      run(HrAbsenceSearchCriteria.none());
      return self();
    }

    public SearchStage la_rh_recherche_par_salarie_equipe_type_et_periode() {
      run(
          new HrAbsenceSearchCriteria(
              employeeId,
              teamId,
              absenceTypeId,
              LocalDate.of(2026, 1, 1),
              LocalDate.of(2026, 12, 31)));
      return self();
    }

    public SearchStage le_resultat_contient(int expected) {
      assertThat(thrownExceptionType).isNull();
      assertThat(result).hasSize(expected);
      return self();
    }

    public SearchStage les_filtres_transmis_sont_complets() {
      ArgumentCaptor<HrAbsenceSearchCriteria> captor =
          ArgumentCaptor.forClass(HrAbsenceSearchCriteria.class);
      verify(absenceRepository).search(captor.capture());
      HrAbsenceSearchCriteria criteria = captor.getValue();
      assertThat(criteria.employeeId()).isEqualTo(employeeId);
      assertThat(criteria.teamId()).isEqualTo(teamId);
      assertThat(criteria.absenceTypeId()).isEqualTo(absenceTypeId);
      assertThat(criteria.from()).isEqualTo(LocalDate.of(2026, 1, 1));
      assertThat(criteria.to()).isEqualTo(LocalDate.of(2026, 12, 31));
      return self();
    }

    public SearchStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
      assertThat(thrownExceptionType).isEqualTo(type);
      return self();
    }

    private void run(HrAbsenceSearchCriteria criteria) {
      var useCase =
          new SearchAbsencesForHR(absenceRepository, absenceTypeRepository, authorizationService);
      try {
        result = useCase.execute(author, criteria);
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
    }

    private Absence absence() {
      UUID absenceId = UUID.randomUUID();
      Instant now = Instant.now();
      VersionAbsence version =
          new VersionAbsence(
              UUID.randomUUID(),
              absenceId,
              1,
              new DateRange(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 6)),
              HalfDayType.NONE,
              HalfDayType.NONE,
              new BigDecimal("3.0"),
              AbsenceVersionStatus.VALIDEE,
              employeeId,
              true,
              now);
      Absence absence =
          new Absence(
              absenceId, employeeId, absenceTypeId, teamId, AbsenceSimplifiedStatus.VALIDEE, now);
      absence.addVersion(version);
      return absence;
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
