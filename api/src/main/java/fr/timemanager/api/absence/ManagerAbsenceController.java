package fr.timemanager.api.absence;

import fr.timemanager.application.absence.CancelValidatedAbsence;
import fr.timemanager.application.absence.CreateAbsenceForEmployee;
import fr.timemanager.application.absence.GetPendingAbsencesForManager;
import fr.timemanager.application.absence.RefuseAbsence;
import fr.timemanager.application.absence.ValidateAbsence;
import fr.timemanager.application.absence.ValidateAbsenceN2;
import fr.timemanager.application.counter.GetEmployeeCounters;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.domain.counter.CompteurAbsence;
import fr.timemanager.domain.user.User;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** EF-009 à EF-014 : file de validation et actions du manager sur son périmètre. */
@RestController
@RequestMapping("/api/v1/manager")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerAbsenceController {

  private static final String MDC_CORRELATION_ID = "correlation_id";

  private final GetPendingAbsencesForManager getPendingAbsencesForManager;
  private final ValidateAbsence validateAbsence;
  private final ValidateAbsenceN2 validateAbsenceN2;
  private final RefuseAbsence refuseAbsence;
  private final CancelValidatedAbsence cancelValidatedAbsence;
  private final CreateAbsenceForEmployee createAbsenceForEmployee;
  private final GetEmployeeCounters getEmployeeCounters;
  private final CurrentUserProvider currentUserProvider;

  public ManagerAbsenceController(
      GetPendingAbsencesForManager getPendingAbsencesForManager,
      ValidateAbsence validateAbsence,
      ValidateAbsenceN2 validateAbsenceN2,
      RefuseAbsence refuseAbsence,
      CancelValidatedAbsence cancelValidatedAbsence,
      CreateAbsenceForEmployee createAbsenceForEmployee,
      GetEmployeeCounters getEmployeeCounters,
      CurrentUserProvider currentUserProvider) {
    this.getPendingAbsencesForManager = getPendingAbsencesForManager;
    this.validateAbsence = validateAbsence;
    this.validateAbsenceN2 = validateAbsenceN2;
    this.refuseAbsence = refuseAbsence;
    this.cancelValidatedAbsence = cancelValidatedAbsence;
    this.createAbsenceForEmployee = createAbsenceForEmployee;
    this.getEmployeeCounters = getEmployeeCounters;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping("/absences/pending")
  public List<AbsenceResponseDto> pending(JwtAuthenticationToken authentication) {
    return getPendingAbsencesForManager.execute(currentUser(authentication));
  }

  /** EF-012 : file de second niveau, alimentée par les demandes déjà validées par le N+1. */
  @GetMapping("/absences/pending-n2")
  public List<AbsenceResponseDto> pendingSecondLevel(JwtAuthenticationToken authentication) {
    return getPendingAbsencesForManager.executeForSecondLevel(currentUser(authentication));
  }

  @PutMapping("/absences/{id}/validate")
  public ResponseEntity<Void> validate(
      @PathVariable UUID id, JwtAuthenticationToken authentication) {
    validateAbsence.execute(currentUser(authentication), id, correlationId());
    return ResponseEntity.noContent().build();
  }

  /** EF-012 : validation de second niveau, réservée au N+2 déclaré sur l'équipe. */
  @PutMapping("/absences/{id}/validate-n2")
  public ResponseEntity<Void> validateN2(
      @PathVariable UUID id, JwtAuthenticationToken authentication) {
    validateAbsenceN2.execute(currentUser(authentication), id, correlationId());
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/absences/{id}/refuse")
  public ResponseEntity<Void> refuse(
      @PathVariable UUID id,
      @Valid @RequestBody DecisionReasonDto body,
      JwtAuthenticationToken authentication) {
    refuseAbsence.execute(currentUser(authentication), id, body.reason(), correlationId());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/absences/{id}/cancel")
  public ResponseEntity<Void> cancel(
      @PathVariable UUID id,
      @Valid @RequestBody DecisionReasonDto body,
      JwtAuthenticationToken authentication) {
    cancelValidatedAbsence.execute(currentUser(authentication), id, body.reason(), correlationId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/employees/{employeeId}/absences")
  public ResponseEntity<AbsenceResponseDto> createForEmployee(
      @PathVariable UUID employeeId,
      @Valid @RequestBody CreateAbsenceRequestDto request,
      JwtAuthenticationToken authentication) {
    AbsenceResponseDto response =
        createAbsenceForEmployee.execute(
            currentUser(authentication), employeeId, request, correlationId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/employees/{employeeId}/counters")
  public List<CompteurAbsence> employeeCounters(
      @PathVariable UUID employeeId, JwtAuthenticationToken authentication) {
    return getEmployeeCounters.execute(currentUser(authentication), employeeId);
  }

  private User currentUser(JwtAuthenticationToken authentication) {
    return currentUserProvider.resolve(authentication.getToken());
  }

  private String correlationId() {
    String correlationId = MDC.get(MDC_CORRELATION_ID);
    return correlationId != null ? correlationId : UUID.randomUUID().toString();
  }

  /** Motif obligatoire d'un refus (EF-011) ou d'une annulation manager (EF-013). */
  public record DecisionReasonDto(@NotBlank String reason) {}
}
