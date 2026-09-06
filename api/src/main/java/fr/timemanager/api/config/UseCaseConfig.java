package fr.timemanager.api.config;

import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.absence.CancelAbsenceRequest;
import fr.timemanager.application.absence.CancelValidatedAbsence;
import fr.timemanager.application.absence.CreateAbsenceForEmployee;
import fr.timemanager.application.absence.CreateAbsenceRequest;
import fr.timemanager.application.absence.GetMyAbsences;
import fr.timemanager.application.absence.GetPendingAbsencesForManager;
import fr.timemanager.application.absence.PublicHolidayRepository;
import fr.timemanager.application.absence.RefuseAbsence;
import fr.timemanager.application.absence.RequestAbsenceModification;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.absence.ValidateAbsence;
import fr.timemanager.application.absence.ValidateAbsenceN2;
import fr.timemanager.application.admin.AdminRepository;
import fr.timemanager.application.admin.ManageAbsenceTypes;
import fr.timemanager.application.admin.ManageTeams;
import fr.timemanager.application.admin.ManageUsers;
import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.application.counter.CounterRepository;
import fr.timemanager.application.counter.GetAbsenceCounters;
import fr.timemanager.application.counter.GetEmployeeCounters;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.application.payroll.CloseMonth;
import fr.timemanager.application.payroll.CorrectAbsence;
import fr.timemanager.application.payroll.CreateSpecialAbsenceByHR;
import fr.timemanager.application.payroll.ExcelExportPort;
import fr.timemanager.application.payroll.GeneratePayrollExport;
import fr.timemanager.application.payroll.GetRegularisations;
import fr.timemanager.application.payroll.PeriodClosureRepository;
import fr.timemanager.application.payroll.RegularisationDetector;
import fr.timemanager.application.payroll.RegularisationRepository;
import fr.timemanager.application.payroll.SearchAbsencesForHR;
import fr.timemanager.application.planning.GetMonthlyPlanning;
import fr.timemanager.application.recurrence.ExcelImportPort;
import fr.timemanager.application.recurrence.GetRecurrenceHistory;
import fr.timemanager.application.recurrence.ImportRecurrenceFromExcel;
import fr.timemanager.application.recurrence.InjectRecurrencePattern;
import fr.timemanager.application.recurrence.RecurrenceRepository;
import fr.timemanager.application.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Câblage des cas d'usage (couche application) en beans Spring. */
@Configuration
public class UseCaseConfig {

  @Bean
  public AuthorizationService authorizationService() {
    return new AuthorizationService();
  }

  @Bean
  public CreateAbsenceRequest createAbsenceRequest(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      PublicHolidayRepository publicHolidayRepository,
      AuditRepository auditRepository,
      NotificationService notificationService,
      TeamRepository teamRepository) {
    return new CreateAbsenceRequest(
        absenceRepository,
        absenceTypeRepository,
        publicHolidayRepository,
        auditRepository,
        notificationService,
        teamRepository);
  }

  @Bean
  public CancelAbsenceRequest cancelAbsenceRequest(
      AbsenceRepository absenceRepository,
      AuditRepository auditRepository,
      NotificationService notificationService) {
    return new CancelAbsenceRequest(absenceRepository, auditRepository, notificationService);
  }

  @Bean
  public GetMyAbsences getMyAbsences(
      AbsenceRepository absenceRepository, AbsenceTypeRepository absenceTypeRepository) {
    return new GetMyAbsences(absenceRepository, absenceTypeRepository);
  }

  @Bean
  public RequestAbsenceModification requestAbsenceModification(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      PublicHolidayRepository publicHolidayRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      NotificationService notificationService) {
    return new RequestAbsenceModification(
        absenceRepository,
        absenceTypeRepository,
        publicHolidayRepository,
        teamRepository,
        auditRepository,
        notificationService);
  }

  @Bean
  public GetAbsenceCounters getAbsenceCounters(
      CounterRepository counterRepository, AbsenceTypeRepository absenceTypeRepository) {
    return new GetAbsenceCounters(counterRepository, absenceTypeRepository);
  }

  @Bean
  public ValidateAbsence validateAbsence(
      AbsenceRepository absenceRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      NotificationService notificationService,
      AuthorizationService authorizationService,
      RegularisationDetector regularisationDetector) {
    return new ValidateAbsence(
        absenceRepository,
        teamRepository,
        auditRepository,
        notificationService,
        authorizationService,
        regularisationDetector);
  }

  @Bean
  public ValidateAbsenceN2 validateAbsenceN2(
      AbsenceRepository absenceRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      NotificationService notificationService,
      AuthorizationService authorizationService,
      RegularisationDetector regularisationDetector) {
    return new ValidateAbsenceN2(
        absenceRepository,
        teamRepository,
        auditRepository,
        notificationService,
        authorizationService,
        regularisationDetector);
  }

  @Bean
  public RefuseAbsence refuseAbsence(
      AbsenceRepository absenceRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      NotificationService notificationService,
      AuthorizationService authorizationService) {
    return new RefuseAbsence(
        absenceRepository,
        teamRepository,
        auditRepository,
        notificationService,
        authorizationService);
  }

  @Bean
  public CancelValidatedAbsence cancelValidatedAbsence(
      AbsenceRepository absenceRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      NotificationService notificationService,
      AuthorizationService authorizationService) {
    return new CancelValidatedAbsence(
        absenceRepository,
        teamRepository,
        auditRepository,
        notificationService,
        authorizationService);
  }

  @Bean
  public GetPendingAbsencesForManager getPendingAbsencesForManager(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      TeamRepository teamRepository,
      AuthorizationService authorizationService) {
    return new GetPendingAbsencesForManager(
        absenceRepository, absenceTypeRepository, teamRepository, authorizationService);
  }

  @Bean
  public CreateAbsenceForEmployee createAbsenceForEmployee(
      CreateAbsenceRequest createAbsenceRequest,
      TeamRepository teamRepository,
      AuthorizationService authorizationService) {
    return new CreateAbsenceForEmployee(createAbsenceRequest, teamRepository, authorizationService);
  }

  @Bean
  public GetEmployeeCounters getEmployeeCounters(
      GetAbsenceCounters getAbsenceCounters,
      TeamRepository teamRepository,
      AuthorizationService authorizationService) {
    return new GetEmployeeCounters(getAbsenceCounters, teamRepository, authorizationService);
  }

  @Bean
  public GetMonthlyPlanning getMonthlyPlanning(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      TeamRepository teamRepository,
      UserRepository userRepository,
      AuthorizationService authorizationService) {
    return new GetMonthlyPlanning(
        absenceRepository,
        absenceTypeRepository,
        teamRepository,
        userRepository,
        authorizationService);
  }

  @Bean
  public RegularisationDetector regularisationDetector(
      PeriodClosureRepository periodClosureRepository,
      RegularisationRepository regularisationRepository) {
    return new RegularisationDetector(periodClosureRepository, regularisationRepository);
  }

  @Bean
  public CloseMonth closeMonth(
      PeriodClosureRepository periodClosureRepository,
      AuditRepository auditRepository,
      AuthorizationService authorizationService) {
    return new CloseMonth(periodClosureRepository, auditRepository, authorizationService);
  }

  @Bean
  public GetRegularisations getRegularisations(
      RegularisationRepository regularisationRepository,
      PeriodClosureRepository periodClosureRepository,
      AuthorizationService authorizationService) {
    return new GetRegularisations(
        regularisationRepository, periodClosureRepository, authorizationService);
  }

  @Bean
  public SearchAbsencesForHR searchAbsencesForHr(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      AuthorizationService authorizationService) {
    return new SearchAbsencesForHR(absenceRepository, absenceTypeRepository, authorizationService);
  }

  @Bean
  public GeneratePayrollExport generatePayrollExport(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      UserRepository userRepository,
      ExcelExportPort excelExportPort,
      AuthorizationService authorizationService) {
    return new GeneratePayrollExport(
        absenceRepository,
        absenceTypeRepository,
        userRepository,
        excelExportPort,
        authorizationService);
  }

  @Bean
  public CreateSpecialAbsenceByHR createSpecialAbsenceByHr(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      PublicHolidayRepository publicHolidayRepository,
      TeamRepository teamRepository,
      AuditRepository auditRepository,
      AuthorizationService authorizationService) {
    return new CreateSpecialAbsenceByHR(
        absenceRepository,
        absenceTypeRepository,
        publicHolidayRepository,
        teamRepository,
        auditRepository,
        authorizationService);
  }

  @Bean
  public CorrectAbsence correctAbsence(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      PublicHolidayRepository publicHolidayRepository,
      AuditRepository auditRepository,
      AuthorizationService authorizationService) {
    return new CorrectAbsence(
        absenceRepository,
        absenceTypeRepository,
        publicHolidayRepository,
        auditRepository,
        authorizationService);
  }

  @Bean
  public InjectRecurrencePattern injectRecurrencePattern(
      AbsenceRepository absenceRepository,
      AbsenceTypeRepository absenceTypeRepository,
      PublicHolidayRepository publicHolidayRepository,
      TeamRepository teamRepository,
      RecurrenceRepository recurrenceRepository) {
    return new InjectRecurrencePattern(
        absenceRepository,
        absenceTypeRepository,
        publicHolidayRepository,
        teamRepository,
        recurrenceRepository);
  }

  @Bean
  public ImportRecurrenceFromExcel importRecurrenceFromExcel(
      ExcelImportPort excelImportPort, InjectRecurrencePattern injectRecurrencePattern) {
    return new ImportRecurrenceFromExcel(excelImportPort, injectRecurrencePattern);
  }

  @Bean
  public GetRecurrenceHistory getRecurrenceHistory(RecurrenceRepository recurrenceRepository) {
    return new GetRecurrenceHistory(recurrenceRepository);
  }

  @Bean
  public ManageUsers manageUsers(
      AdminRepository adminRepository, AuthorizationService authorizationService) {
    return new ManageUsers(adminRepository, authorizationService);
  }

  @Bean
  public ManageTeams manageTeams(
      AdminRepository adminRepository,
      TeamRepository teamRepository,
      AuthorizationService authorizationService) {
    return new ManageTeams(adminRepository, teamRepository, authorizationService);
  }

  @Bean
  public ManageAbsenceTypes manageAbsenceTypes(
      AdminRepository adminRepository,
      AbsenceTypeRepository absenceTypeRepository,
      AuthorizationService authorizationService) {
    return new ManageAbsenceTypes(adminRepository, absenceTypeRepository, authorizationService);
  }
}
