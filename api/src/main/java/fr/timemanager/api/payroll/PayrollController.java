package fr.timemanager.api.payroll;

import fr.timemanager.application.counter.GetAbsenceCounters;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.application.dto.payroll.HrAbsenceSearchCriteria;
import fr.timemanager.application.payroll.CloseMonth;
import fr.timemanager.application.payroll.CorrectAbsence;
import fr.timemanager.application.payroll.CreateSpecialAbsenceByHR;
import fr.timemanager.application.payroll.GeneratePayrollExport;
import fr.timemanager.application.payroll.GetRegularisations;
import fr.timemanager.application.payroll.SearchAbsencesForHR;
import fr.timemanager.domain.counter.CompteurAbsence;
import fr.timemanager.domain.payroll.PeriodStatus;
import fr.timemanager.domain.payroll.Regularisation;
import fr.timemanager.domain.user.User;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** EF-021 à EF-027 : consultation, saisie, correction, clôture et export paie réservés à la RH. */
@RestController
@RequestMapping("/api/v1/rh")
@PreAuthorize("hasRole('RH')")
public class PayrollController {

  private static final String MDC_CORRELATION_ID = "correlation_id";

  private final CloseMonth closeMonth;
  private final GetRegularisations getRegularisations;
  private final GeneratePayrollExport generatePayrollExport;
  private final SearchAbsencesForHR searchAbsencesForHR;
  private final CreateSpecialAbsenceByHR createSpecialAbsenceByHR;
  private final CorrectAbsence correctAbsence;
  private final GetAbsenceCounters getAbsenceCounters;
  private final CurrentUserProvider currentUserProvider;

  public PayrollController(
      CloseMonth closeMonth,
      GetRegularisations getRegularisations,
      GeneratePayrollExport generatePayrollExport,
      SearchAbsencesForHR searchAbsencesForHR,
      CreateSpecialAbsenceByHR createSpecialAbsenceByHR,
      CorrectAbsence correctAbsence,
      GetAbsenceCounters getAbsenceCounters,
      CurrentUserProvider currentUserProvider) {
    this.closeMonth = closeMonth;
    this.getRegularisations = getRegularisations;
    this.generatePayrollExport = generatePayrollExport;
    this.searchAbsencesForHR = searchAbsencesForHR;
    this.createSpecialAbsenceByHR = createSpecialAbsenceByHR;
    this.correctAbsence = correctAbsence;
    this.getAbsenceCounters = getAbsenceCounters;
    this.currentUserProvider = currentUserProvider;
  }

  /** EF-024 : clôture d'une période mensuelle en une seule action. */
  @PostMapping("/periods/{year}/{month}/close")
  public ResponseEntity<PeriodClosureDto> close(
      @PathVariable int year, @PathVariable int month, JwtAuthenticationToken authentication) {
    var period =
        closeMonth.execute(currentUser(authentication), YearMonth.of(year, month), correlationId());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new PeriodClosureDto(
                period.getId(), period.getYear(), period.getMonth(), period.getStatus()));
  }

  /** EF-026 : régularisations, filtrables par période. */
  @GetMapping("/regularisations")
  public List<Regularisation> regularisations(
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      JwtAuthenticationToken authentication) {
    YearMonth period = year != null && month != null ? YearMonth.of(year, month) : null;
    return getRegularisations.execute(currentUser(authentication), period);
  }

  /** EF-027 : export Excel annuel exploitable pour la paie. */
  @GetMapping("/export/annual/{year}")
  public ResponseEntity<byte[]> exportAnnual(
      @PathVariable int year, JwtAuthenticationToken authentication) {
    byte[] workbook = generatePayrollExport.execute(currentUser(authentication), year);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"absences-" + year + ".xlsx\"")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(workbook);
  }

  /** EF-021 : consultation de toutes les absences avec filtres cumulables. */
  @GetMapping("/absences")
  public List<AbsenceResponseDto> searchAbsences(
      @RequestParam(required = false) UUID employeeId,
      @RequestParam(required = false) UUID teamId,
      @RequestParam(required = false) UUID typeId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      JwtAuthenticationToken authentication) {
    return searchAbsencesForHR.execute(
        currentUser(authentication),
        new HrAbsenceSearchCriteria(employeeId, teamId, typeId, from, to));
  }

  /** EF-022 : saisie d'une absence spéciale pour tout salarié. */
  @PostMapping("/absences")
  public ResponseEntity<AbsenceResponseDto> createSpecialAbsence(
      @RequestParam UUID employeeId,
      @Valid @RequestBody CreateAbsenceRequestDto request,
      JwtAuthenticationToken authentication) {
    AbsenceResponseDto response =
        createSpecialAbsenceByHR.execute(
            currentUser(authentication), employeeId, request, correlationId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** EF-023 : correction d'une absence existante, avec trace avant/après. */
  @PutMapping("/absences/{id}/correct")
  public AbsenceResponseDto correct(
      @PathVariable UUID id,
      @Valid @RequestBody CreateAbsenceRequestDto request,
      JwtAuthenticationToken authentication) {
    return correctAbsence.execute(currentUser(authentication), id, request, correlationId());
  }

  /** EF-020 : compteurs d'un salarié consultés par la RH. */
  @GetMapping("/employees/{employeeId}/counters")
  public List<CompteurAbsence> employeeCounters(@PathVariable UUID employeeId) {
    return getAbsenceCounters.execute(employeeId);
  }

  private User currentUser(JwtAuthenticationToken authentication) {
    return currentUserProvider.resolve(authentication.getToken());
  }

  private String correlationId() {
    String correlationId = MDC.get(MDC_CORRELATION_ID);
    return correlationId != null ? correlationId : UUID.randomUUID().toString();
  }

  /** Vue d'une période de paie après clôture. */
  public record PeriodClosureDto(UUID id, int year, int month, PeriodStatus status) {}
}
