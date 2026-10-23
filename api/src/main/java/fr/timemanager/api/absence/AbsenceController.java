package fr.timemanager.api.absence;

import fr.timemanager.application.absence.CancelAbsenceRequest;
import fr.timemanager.application.absence.CreateAbsenceRequest;
import fr.timemanager.application.absence.GetMyAbsences;
import fr.timemanager.application.absence.RequestAbsenceModification;
import fr.timemanager.application.absence.TeamRepository;
import fr.timemanager.application.counter.GetAbsenceCounters;
import fr.timemanager.application.dto.absence.AbsenceResponseDto;
import fr.timemanager.application.dto.absence.CreateAbsenceRequestDto;
import fr.timemanager.domain.counter.CompteurAbsence;
import fr.timemanager.domain.user.User;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** EF-001 à EF-006, EF-018 à EF-020 : déclaration et suivi des absences du salarié connecté. */
@RestController
@RequestMapping("/api/v1/absences")
public class AbsenceController {

  private static final String MDC_CORRELATION_ID = "correlation_id";

  private final CreateAbsenceRequest createAbsenceRequest;
  private final CancelAbsenceRequest cancelAbsenceRequest;
  private final GetMyAbsences getMyAbsences;
  private final RequestAbsenceModification requestAbsenceModification;
  private final GetAbsenceCounters getAbsenceCounters;
  private final TeamRepository teamRepository;
  private final CurrentUserProvider currentUserProvider;

  public AbsenceController(
      CreateAbsenceRequest createAbsenceRequest,
      CancelAbsenceRequest cancelAbsenceRequest,
      GetMyAbsences getMyAbsences,
      RequestAbsenceModification requestAbsenceModification,
      GetAbsenceCounters getAbsenceCounters,
      TeamRepository teamRepository,
      CurrentUserProvider currentUserProvider) {
    this.createAbsenceRequest = createAbsenceRequest;
    this.cancelAbsenceRequest = cancelAbsenceRequest;
    this.getMyAbsences = getMyAbsences;
    this.requestAbsenceModification = requestAbsenceModification;
    this.getAbsenceCounters = getAbsenceCounters;
    this.teamRepository = teamRepository;
    this.currentUserProvider = currentUserProvider;
  }

  @PostMapping
  @PreAuthorize("hasRole('SALARIE')")
  public ResponseEntity<AbsenceResponseDto> create(
      @Valid @RequestBody CreateAbsenceRequestDto request, JwtAuthenticationToken authentication) {
    User employee = currentUser(authentication);
    UUID teamId =
        teamRepository
            .findTeamIdByMemberId(employee.getId())
            .orElseThrow(() -> new IllegalStateException("Aucune équipe pour cet utilisateur"));
    AbsenceResponseDto response =
        createAbsenceRequest.execute(employee.getId(), teamId, correlationId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** EF-007 : demande de modification d'une absence validée — crée une version à revalider. */
  @PutMapping("/{id}/modify")
  @PreAuthorize("hasRole('SALARIE')")
  public AbsenceResponseDto modify(
      @PathVariable UUID id,
      @Valid @RequestBody CreateAbsenceRequestDto request,
      JwtAuthenticationToken authentication) {
    User employee = currentUser(authentication);
    return requestAbsenceModification.execute(employee.getId(), id, request, correlationId());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('SALARIE')")
  public ResponseEntity<Void> cancel(@PathVariable UUID id, JwtAuthenticationToken authentication) {
    User employee = currentUser(authentication);
    cancelAbsenceRequest.execute(employee.getId(), id, correlationId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('SALARIE')")
  public List<AbsenceResponseDto> myAbsences(JwtAuthenticationToken authentication) {
    User employee = currentUser(authentication);
    return getMyAbsences.execute(employee.getId());
  }

  @GetMapping("/me/counters")
  @PreAuthorize("hasRole('SALARIE')")
  public List<CompteurAbsence> myCounters(JwtAuthenticationToken authentication) {
    User employee = currentUser(authentication);
    return getAbsenceCounters.execute(employee.getId());
  }

  private User currentUser(JwtAuthenticationToken authentication) {
    Jwt jwt = authentication.getToken();
    return currentUserProvider.resolve(jwt);
  }

  private String correlationId() {
    String correlationId = MDC.get(MDC_CORRELATION_ID);
    return correlationId != null ? correlationId : UUID.randomUUID().toString();
  }
}
