package fr.timemanager.api.exception;

import fr.timemanager.application.authorization.SelfApprovalException;
import fr.timemanager.application.authorization.UnauthorizedException;
import fr.timemanager.domain.absence.AbsenceOverlapException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduit les exceptions métier en réponses HTTP cohérentes (RFC simplifiée). */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Requête invalide", ex.getMessage()));
  }

  @ExceptionHandler(AbsenceOverlapException.class)
  public ResponseEntity<ErrorResponse> handleOverlap(AbsenceOverlapException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "Conflit d'absence", ex.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "État invalide", ex.getMessage()));
  }

  @ExceptionHandler({UnauthorizedException.class, SelfApprovalException.class})
  public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Accès refusé", ex.getMessage()));
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Accès refusé", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " : " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation échouée");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Validation échouée", message));
  }
}
