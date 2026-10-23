package fr.timemanager.application.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.absence.PublicHolidayRepository;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import fr.timemanager.domain.shared.HalfDayType;
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

/** EF-022 : saisie par la RH d'une absence spéciale pour tout salarié. */
class CreateSpecialAbsenceByHRTest
    extends SimpleScenarioTest<CreateSpecialAbsenceByHRTest.SpecialAbsenceStage> {

  @Test
  void ef022_laRhSaisitUnArretMaladie_directementValide() {
    given().un_salarie_et_un_type_arret_maladie();

    when().la_rh_saisit_l_absence_speciale();

    then()
        .l_absence_est_creee_au_statut(AbsenceSimplifiedStatus.VALIDEE)
        .and()
        .l_absence_est_rattachee_au_salarie()
        .and()
        .un_evenement_d_audit_de_type(AuditActionType.CREATION_RH);
  }

  @Test
  void ef022_unManagerNePeutPasSaisirUneAbsenceSpeciale() {
    given().un_salarie_et_un_type_arret_maladie().mais_l_auteur_est_un_simple_manager();

    when().la_rh_saisit_l_absence_speciale();

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  @Test
  void ef022_unSalarieNePeutPasSaisirUneAbsenceSpeciale() {
    given().un_salarie_et_un_type_arret_maladie().mais_l_auteur_est_un_simple_salarie();

    when().la_rh_saisit_l_absence_speciale();

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  public static class SpecialAbsenceStage extends Stage<SpecialAbsenceStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);
    private final PublicHolidayRepository publicHolidayRepository =
        mock(PublicHolidayRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final AuditRepository auditRepository = mock(AuditRepository.class);
    private final AuthorizationService authorizationService = new AuthorizationService();

    private final UUID hrUserId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID absenceTypeId = UUID.randomUUID();
    private User author = user(hrUserId, Set.of(UserRole.SALARIE, UserRole.RH));

    private AbsenceResponseDto response;
    private Class<? extends RuntimeException> thrownExceptionType;

    public SpecialAbsenceStage un_salarie_et_un_type_arret_maladie() {
      Mockito.when(absenceTypeRepository.findById(absenceTypeId))
          .thenReturn(
              Optional.of(
                  new AbsenceType(
                      absenceTypeId,
                      "MALADIE",
                      "Arrêt maladie",
                      "#F44336",
                      false,
                      true,
                      Set.of(UserRole.RH),
                      true,
                      false,
                      true,
                      true)));
      Mockito.when(teamRepository.findTeamIdByMemberId(employeeId)).thenReturn(Optional.of(teamId));
      Mockito.when(publicHolidayRepository.findBetween(any(), any())).thenReturn(List.of());
      Mockito.when(absenceRepository.save(any()))
          .thenAnswer(invocation -> invocation.<Absence>getArgument(0));
      return self();
    }

    public SpecialAbsenceStage mais_l_auteur_est_un_simple_manager() {
      author = user(hrUserId, Set.of(UserRole.SALARIE, UserRole.MANAGER));
      return self();
    }

    public SpecialAbsenceStage mais_l_auteur_est_un_simple_salarie() {
      author = user(hrUserId, Set.of(UserRole.SALARIE));
      return self();
    }

    public SpecialAbsenceStage la_rh_saisit_l_absence_speciale() {
      var useCase =
          new CreateSpecialAbsenceByHR(
              absenceRepository,
              absenceTypeRepository,
              publicHolidayRepository,
              teamRepository,
              auditRepository,
              authorizationService);
      var request =
          new CreateAbsenceRequestDto(
              absenceTypeId,
              LocalDate.of(2026, 10, 5),
              LocalDate.of(2026, 10, 9),
              HalfDayType.NONE,
              HalfDayType.NONE,
              null);
      try {
        response = useCase.execute(author, employeeId, request, UUID.randomUUID().toString());
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
      return self();
    }

    public SpecialAbsenceStage l_absence_est_creee_au_statut(AbsenceSimplifiedStatus status) {
      assertThat(thrownExceptionType).isNull();
      assertThat(response.status()).isEqualTo(status);
      return self();
    }

    public SpecialAbsenceStage l_absence_est_rattachee_au_salarie() {
      assertThat(response.employeeId()).isEqualTo(employeeId);
      assertThat(response.absenceTypeLabel()).isEqualTo("Arrêt maladie");
      return self();
    }

    public SpecialAbsenceStage un_evenement_d_audit_de_type(AuditActionType actionType) {
      ArgumentCaptor<EvenementAudit> captor = ArgumentCaptor.forClass(EvenementAudit.class);
      verify(auditRepository).save(captor.capture());
      assertThat(captor.getValue().actionType()).isEqualTo(actionType);
      assertThat(captor.getValue().authorRole()).isEqualTo("RH");
      return self();
    }

    public SpecialAbsenceStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
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
