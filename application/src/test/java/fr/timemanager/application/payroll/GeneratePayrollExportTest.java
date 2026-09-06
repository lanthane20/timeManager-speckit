package fr.timemanager.application.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.application.dto.payroll.PayrollExportRow;
import fr.timemanager.application.user.UserRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Scénario 3 de US5 — EF-027 et CS-005 : export Excel annuel des absences validées. */
class GeneratePayrollExportTest extends SimpleScenarioTest<GeneratePayrollExportTest.ExportStage> {

  @Test
  void scenario3_ef027_lExportContientLesAbsencesValideesDeLAnnee() {
    given().des_absences_validees_sur_l_annee(2026, 3);

    when().la_rh_demande_l_export_annuel(2026);

    then()
        .l_export_contient(3)
        .and()
        .les_lignes_portent_le_salarie_le_type_et_la_periode()
        .and()
        .les_lignes_sont_triees_par_salarie_puis_type_puis_date();
  }

  @Test
  void ef027_annneeSansAbsenceValidee_produitUnExportVide() {
    given().des_absences_validees_sur_l_annee(2026, 0);

    when().la_rh_demande_l_export_annuel(2026);

    then().l_export_contient(0);
  }

  @Test
  void seulLeRoleRh_peutDemanderLExport() {
    given().des_absences_validees_sur_l_annee(2026, 1).mais_l_auteur_est_un_simple_manager();

    when().la_rh_demande_l_export_annuel(2026);

    then().l_operation_echoue_avec(UnauthorizedException.class);
  }

  public static class ExportStage extends Stage<ExportStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ExcelExportPort excelExportPort = mock(ExcelExportPort.class);
    private final AuthorizationService authorizationService = new AuthorizationService();

    private final UUID hrUserId = UUID.randomUUID();
    private final UUID absenceTypeId = UUID.randomUUID();
    private User author = user(hrUserId, "Rachel", "Hubert", Set.of(UserRole.RH));

    private List<PayrollExportRow> exportedRows;
    private Class<? extends RuntimeException> thrownExceptionType;

    public ExportStage des_absences_validees_sur_l_annee(int year, int count) {
      List<Absence> absences = new ArrayList<>();
      List<User> employees = new ArrayList<>();
      // Noms décroissants pour vérifier que le tri est bien fait par le use case.
      String[] lastNames = {"Zola", "Martin", "Bernard"};
      for (int index = 0; index < count; index++) {
        UUID employeeId = UUID.randomUUID();
        employees.add(
            user(employeeId, "Prénom" + index, lastNames[index % 3], Set.of(UserRole.SALARIE)));
        absences.add(validatedAbsence(employeeId, year, index));
      }
      Mockito.when(absenceRepository.findValidatedInYear(year)).thenReturn(absences);
      Mockito.when(userRepository.findAllByIds(any())).thenReturn(employees);
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
      Mockito.when(excelExportPort.buildAnnualExport(anyInt(), any())).thenReturn(new byte[] {1});
      return self();
    }

    public ExportStage mais_l_auteur_est_un_simple_manager() {
      author = user(hrUserId, "Manon", "Dubois", Set.of(UserRole.SALARIE, UserRole.MANAGER));
      return self();
    }

    public ExportStage la_rh_demande_l_export_annuel(int year) {
      var useCase =
          new GeneratePayrollExport(
              absenceRepository,
              absenceTypeRepository,
              userRepository,
              excelExportPort,
              authorizationService);
      try {
        useCase.execute(author, year);
        captureRows();
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
      return self();
    }

    public ExportStage l_export_contient(int expected) {
      assertThat(thrownExceptionType).isNull();
      assertThat(exportedRows).hasSize(expected);
      return self();
    }

    public ExportStage les_lignes_portent_le_salarie_le_type_et_la_periode() {
      assertThat(exportedRows)
          .allSatisfy(
              row -> {
                assertThat(row.employeeLastName()).isNotBlank();
                assertThat(row.absenceTypeCode()).isEqualTo("CP");
                assertThat(row.startDate()).isNotNull();
                assertThat(row.endDate()).isNotNull();
                assertThat(row.workingDays()).isPositive();
              });
      return self();
    }

    public ExportStage les_lignes_sont_triees_par_salarie_puis_type_puis_date() {
      assertThat(exportedRows).extracting(PayrollExportRow::employeeLastName).isSorted();
      return self();
    }

    public ExportStage l_operation_echoue_avec(Class<? extends RuntimeException> type) {
      assertThat(thrownExceptionType).isEqualTo(type);
      return self();
    }

    @SuppressWarnings("unchecked")
    private void captureRows() {
      ArgumentCaptor<List<PayrollExportRow>> captor = ArgumentCaptor.forClass(List.class);
      verify(excelExportPort).buildAnnualExport(anyInt(), captor.capture());
      exportedRows = captor.getValue();
    }

    private Absence validatedAbsence(UUID employeeId, int year, int index) {
      UUID absenceId = UUID.randomUUID();
      Instant now = Instant.now();
      LocalDate start = LocalDate.of(year, 1 + index, 5);
      VersionAbsence version =
          new VersionAbsence(
              UUID.randomUUID(),
              absenceId,
              1,
              new DateRange(start, start.plusDays(2)),
              HalfDayType.NONE,
              HalfDayType.NONE,
              new BigDecimal("3.0"),
              AbsenceVersionStatus.VALIDEE,
              employeeId,
              true,
              now);
      Absence absence =
          new Absence(
              absenceId,
              employeeId,
              absenceTypeId,
              UUID.randomUUID(),
              AbsenceSimplifiedStatus.VALIDEE,
              now);
      absence.addVersion(version);
      return absence;
    }

    private User user(UUID id, String firstName, String lastName, Set<UserRole> roles) {
      return new User(
          id,
          "sub_" + id,
          id + "@timemanager.fr",
          firstName,
          lastName,
          true,
          null,
          false,
          roles,
          Instant.now());
    }
  }
}
