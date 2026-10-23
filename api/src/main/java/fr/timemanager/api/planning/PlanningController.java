package fr.timemanager.api.planning;

import fr.timemanager.application.dto.planning.PlanningEntryDto;
import fr.timemanager.application.planning.GetMonthlyPlanning;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** EF-015 à EF-017 : planning mensuel de l'équipe, filtrable par mois et par équipe. */
@RestController
@RequestMapping("/api/v1/planning")
public class PlanningController {

  private final GetMonthlyPlanning getMonthlyPlanning;
  private final CurrentUserProvider currentUserProvider;

  public PlanningController(
      GetMonthlyPlanning getMonthlyPlanning, CurrentUserProvider currentUserProvider) {
    this.getMonthlyPlanning = getMonthlyPlanning;
    this.currentUserProvider = currentUserProvider;
  }

  /**
   * @param month au format {@code yyyy-MM} ; absent = mois courant (EF-016)
   * @param teamId équipe ciblée ; absent = toutes les équipes du périmètre (EF-017)
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('MANAGER','RH')")
  public List<PlanningEntryDto> monthlyPlanning(
      @RequestParam(required = false) String month,
      @RequestParam(required = false) UUID teamId,
      JwtAuthenticationToken authentication) {
    return getMonthlyPlanning.execute(
        currentUserProvider.resolve(authentication.getToken()), parseMonth(month), teamId);
  }

  private YearMonth parseMonth(String month) {
    if (month == null || month.isBlank()) {
      return null;
    }
    try {
      return YearMonth.parse(month);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Mois invalide (format attendu yyyy-MM) : " + month);
    }
  }
}
