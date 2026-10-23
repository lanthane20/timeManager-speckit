package fr.timemanager.api.recurrence;

import fr.timemanager.application.recurrence.GetRecurrenceHistory;
import fr.timemanager.application.recurrence.ImportRecurrenceFromExcel;
import fr.timemanager.application.recurrence.InjectRecurrencePattern;
import fr.timemanager.application.recurrence.InjectRecurrencePattern.InjectionResult;
import fr.timemanager.domain.recurrence.InjectionSource;
import fr.timemanager.domain.recurrence.RecurrenceInjectionLog;
import fr.timemanager.domain.user.User;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * EF-028 à EF-030 : module récurrences. L'accès effectif est conditionné à l'activation du module
 * pour l'utilisateur (EF-030), vérifiée dans la couche application.
 */
@RestController
@RequestMapping("/api/v1/recurrences")
@PreAuthorize("hasRole('SALARIE')")
public class RecurrenceController {

  private final InjectRecurrencePattern injectRecurrencePattern;
  private final ImportRecurrenceFromExcel importRecurrenceFromExcel;
  private final GetRecurrenceHistory getRecurrenceHistory;
  private final CurrentUserProvider currentUserProvider;

  public RecurrenceController(
      InjectRecurrencePattern injectRecurrencePattern,
      ImportRecurrenceFromExcel importRecurrenceFromExcel,
      GetRecurrenceHistory getRecurrenceHistory,
      CurrentUserProvider currentUserProvider) {
    this.injectRecurrencePattern = injectRecurrencePattern;
    this.importRecurrenceFromExcel = importRecurrenceFromExcel;
    this.getRecurrenceHistory = getRecurrenceHistory;
    this.currentUserProvider = currentUserProvider;
  }

  /** EF-028 : import d'un planning école au format Excel. */
  @PostMapping(path = "/import", consumes = "multipart/form-data")
  public ResponseEntity<InjectionResponseDto> importExcel(
      @RequestParam UUID patternId,
      @RequestPart("file") MultipartFile file,
      JwtAuthenticationToken authentication) {
    InjectionResult result =
        importRecurrenceFromExcel.execute(currentUser(authentication), patternId, readBytes(file));
    return ResponseEntity.status(HttpStatus.CREATED).body(InjectionResponseDto.from(result));
  }

  /** EF-028 : génération d'événements sur une liste de dates (saisie unitaire ou périodique). */
  @PostMapping("/generate")
  public ResponseEntity<InjectionResponseDto> generate(
      @Valid @RequestBody GenerateRequestDto request, JwtAuthenticationToken authentication) {
    InjectionResult result =
        injectRecurrencePattern.execute(
            currentUser(authentication),
            request.patternId(),
            request.dates(),
            request.source() != null ? request.source() : InjectionSource.GENERATION);
    return ResponseEntity.status(HttpStatus.CREATED).body(InjectionResponseDto.from(result));
  }

  /** EF-029 : historique de toutes les injections. */
  @GetMapping("/history")
  public List<RecurrenceInjectionLog> history(JwtAuthenticationToken authentication) {
    return getRecurrenceHistory.execute(currentUser(authentication));
  }

  private User currentUser(JwtAuthenticationToken authentication) {
    return currentUserProvider.resolve(authentication.getToken());
  }

  private byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException ex) {
      throw new UncheckedIOException("Lecture du fichier importé impossible", ex);
    }
  }

  /** Demande de génération : dates à injecter pour un patron donné. */
  public record GenerateRequestDto(
      @NotNull UUID patternId, @NotEmpty List<LocalDate> dates, InjectionSource source) {}

  /**
   * Résultat d'une injection. Les avertissements de chevauchement sont retournés au client sans
   * bloquer l'opération (scénario 3 de US6).
   */
  public record InjectionResponseDto(
      UUID injectionId, int eventsCreatedCount, String absenceTypeCode, List<String> warnings) {

    static InjectionResponseDto from(InjectionResult result) {
      return new InjectionResponseDto(
          result.log().id(),
          result.log().eventsCreatedCount(),
          result.absenceTypeCode(),
          result.warnings());
    }
  }
}
