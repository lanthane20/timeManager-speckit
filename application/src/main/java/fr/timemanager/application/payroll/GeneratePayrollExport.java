package fr.timemanager.application.payroll;

import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.dto.payroll.PayrollExportRow;
import fr.timemanager.application.user.UserRepository;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case EF-027 : export Excel annuel des absences validées, structuré par salarié, type et
 * période. Les référentiels sont chargés en une passe pour tenir la contrainte CS-005 (moins de 30
 * secondes quelle que soit la volumétrie annuelle).
 */
public class GeneratePayrollExport {

  private final AbsenceRepository absenceRepository;
  private final AbsenceTypeRepository absenceTypeRepository;
  private final UserRepository userRepository;
  private final ExcelExportPort excelExportPort;
  private final AuthorizationService authorizationService;

  public GeneratePayrollExport(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      UserRepository userRepository,
      ExcelExportPort excelExportPort,
      AuthorizationService authorizationService) {
    this.absenceRepository = absenceRepository;
    this.absenceTypeRepository = absenceTypeRepository;
    this.userRepository = userRepository;
    this.excelExportPort = excelExportPort;
    this.authorizationService = authorizationService;
  }

  public byte[] execute(User hrUser, int year) {
    authorizationService.assertHasRole(hrUser, UserRole.RH);
    List<Absence> absences = absenceRepository.findValidatedInYear(year);
    Map<UUID, User> employees = loadEmployees(absences);
    Map<UUID, String[]> types = loadTypeLabels(absences);

    List<PayrollExportRow> rows =
        absences.stream()
            .map(absence -> toRow(absence, employees, types))
            .sorted(
                Comparator.comparing(PayrollExportRow::employeeLastName)
                    .thenComparing(PayrollExportRow::absenceTypeCode)
                    .thenComparing(PayrollExportRow::startDate))
            .toList();

    return excelExportPort.buildAnnualExport(year, rows);
  }

  private Map<UUID, User> loadEmployees(List<Absence> absences) {
    Set<UUID> ids = absences.stream().map(Absence::getEmployeeId).collect(Collectors.toSet());
    return userRepository.findAllByIds(ids).stream()
        .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
  }

  private Map<UUID, String[]> loadTypeLabels(List<Absence> absences) {
    return absences.stream()
        .map(Absence::getAbsenceTypeId)
        .distinct()
        .map(absenceTypeRepository::findById)
        .flatMap(Optional::stream)
        .collect(
            Collectors.toMap(
                type -> type.getId(), type -> new String[] {type.getCode(), type.getLabel()}));
  }

  private PayrollExportRow toRow(
      Absence absence, Map<UUID, User> employees, Map<UUID, String[]> types) {
    VersionAbsence version = absence.getActiveVersion();
    User employee = employees.get(absence.getEmployeeId());
    String[] type = types.getOrDefault(absence.getAbsenceTypeId(), new String[] {"?", "Inconnu"});
    return new PayrollExportRow(
        absence.getEmployeeId(),
        employee != null ? employee.getLastName() : "Inconnu",
        employee != null ? employee.getFirstName() : "",
        type[0],
        type[1],
        version.getDateRange().startDate(),
        version.getDateRange().endDate(),
        version.getWorkingDays(),
        version.isRegularisation());
  }
}
