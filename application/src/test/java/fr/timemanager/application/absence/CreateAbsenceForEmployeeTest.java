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
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.shared.HalfDayType;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.team.ValidationMode;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** EF-014 : saisie d'une absence par un manager pour un salarié de son périmètre. */
class CreateAbsenceForEmployeeTest
    extends SimpleScenarioTest<CreateAbsenceForEmployeeTest.CreateForEmployeeStage> {

  @Test
  void ef014_managerDuPerimetre_creeLaDemandePourLeSalarie() {
    given().un_salarie_du_perimetre_du_manager();

    when().le_manager_saisit_une_absence_pour_le_salarie();

    then()
        .la_demande_est_creee_au_statut(AbsenceSimplifiedStatus.EN_ATTENTE)
        .and()
        .la_demande_est_rattachee_au_salarie()
        .and()
        .le_salarie_est_notifie();
  }

  @Test
  void ef014_salarieHorsPerimetre_estRefuse() {
    given().un_salarie_hors_du_perimetre_du_manager();

    when().le_manager_saisit_une_absence_pour_le_salarie();

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  @Test
  void ef014_utilisateurSansRoleManager_estRefuse() {
    given().un_salarie_du_perimetre_du_manager().mais_l_auteur_n_est_pas_manager();

    when().le_manager_saisit_une_absence_pour_le_salarie();

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  public static class CreateForEmployeeStage extends Stage<CreateForEmployeeStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);
    private final PublicHolidayRepository publicHolidayRepository =
        mock(PublicHolidayRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final AuditRepository auditRepository = mock(AuditRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuthorizationService authorizationService = new AuthorizationService();

    private final UUID managerId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID absenceTypeId = UUID.randomUUID();

    private User author = user(managerId, Set.of(UserRole.SALARIE, UserRole.MANAGER));
    private AbsenceResponseDto response;
    private Class<? extends RuntimeException> thrownExceptionType;

    public CreateForEmployeeStage un_salarie_du_perimetre_du_manager() {
      seed(Set.of(employeeId));
      return self();
    }

    public CreateForEmployeeStage un_salarie_hors_du_perimetre_du_manager() {
      seed(Set.of(UUID.randomUUID()));
      return self();
    }

    public CreateForEmployeeStage mais_l_auteur_n_est_pas_manager() {
      author = user(managerId, Set.of(UserRole.SALARIE));
      return self();
    }

    public CreateForEmployeeStage le_manager_saisit_une_absence_pour_le_salarie() {
      var createAbsenceRequest =
          new CreateAbsenceRequest(
              absenceRepository,
              absenceTypeRepository,
              publicHolidayRepository,
              auditRepository,
              notificationService,
              teamRepository);
      var useCase =
          new CreateAbsenceForEmployee(createAbsenceRequest, teamRepository, authorizationService);
      var request =
          new CreateAbsenceRequestDto(
              absenceTypeId,
              LocalDate.of(2026, 9, 7),
              LocalDate.of(2026, 9, 8),
              HalfDayType.NONE,
              HalfDayType.NONE,
              managerId);
      try {
        response = useCase.execute(author, employeeId, request, UUID.randomUUID().toString());
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
      return self();
    }

    public CreateForEmployeeStage la_demande_est_creee_au_statut(AbsenceSimplifiedStatus status) {
      assertThat(thrownExceptionType).isNull();
      assertThat(response.status()).isEqualTo(status);
      return self();
    }

    public CreateForEmployeeStage la_demande_est_rattachee_au_salarie() {
      assertThat(response.employeeId()).isEqualTo(employeeId);
      return self();
    }

    public CreateForEmployeeStage le_salarie_est_notifie() {
      verify(notificationService).notifyAbsenceCreated(Mockito.eq(employeeId), any());
      return self();
    }

    public CreateForEmployeeStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
      assertThat(thrownExceptionType).isEqualTo(type);
      return self();
    }

    private void seed(Set<UUID> memberIds) {
      Mockito.when(teamRepository.findTeamIdByMemberId(employeeId)).thenReturn(Optional.of(teamId));
      Mockito.when(teamRepository.findById(teamId))
          .thenReturn(
              Optional.of(
                  new Team(teamId, "Équipe", null, ValidationMode.N1_ONLY, managerId, null, true)));
      Mockito.when(teamRepository.findMemberIdsByTeamId(teamId)).thenReturn(memberIds);
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
      Mockito.when(absenceRepository.findOverlappingInfo(any(), any(), any()))
          .thenReturn(List.of());
      Mockito.when(publicHolidayRepository.findBetween(any(), any())).thenReturn(List.of());
      Mockito.when(absenceRepository.save(any()))
          .thenAnswer(invocation -> invocation.<Absence>getArgument(0));
    }

    private User user(UUID id, Set<UserRole> roles) {
      return new User(
          id,
          "sub_" + id,
          id + "@timemanager.fr",
          "Manon",
          "Dubois",
          true,
          null,
          false,
          roles,
          Instant.now());
    }
  }
}
