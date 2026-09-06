package fr.timemanager.application.payroll;

import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.domain.audit.AuditActionType;
import fr.timemanager.domain.audit.EvenementAudit;
import fr.timemanager.domain.payroll.PeriodeCloture;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Use case EF-024 : la RH clôture une période mensuelle en une seule action. À partir de là, toute
 * validation portant sur cette période devient une régularisation (EF-025).
 */
public class CloseMonth {

  private final PeriodClosureRepository periodClosureRepository;
  private final AuditRepository auditRepository;
  private final AuthorizationService authorizationService;

  public CloseMonth(
      PeriodClosureRepository periodClosureRepository,
      AuditRepository auditRepository,
      AuthorizationService authorizationService) {
    this.periodClosureRepository = periodClosureRepository;
    this.auditRepository = auditRepository;
    this.authorizationService = authorizationService;
  }

  public PeriodeCloture execute(User hrUser, YearMonth period, String correlationId) {
    authorizationService.assertHasRole(hrUser, UserRole.RH);

    PeriodeCloture closure =
        periodClosureRepository
            .findByYearAndMonth(period.getYear(), period.getMonthValue())
            .orElseGet(
                () ->
                    new PeriodeCloture(
                        UUID.randomUUID(), period.getYear(), period.getMonthValue()));

    closure.close(hrUser.getId(), Instant.now());
    PeriodeCloture saved = periodClosureRepository.save(closure);

    auditRepository.save(
        new EvenementAudit(
            UUID.randomUUID(),
            null,
            null,
            AuditActionType.CLOTURE_PERIODE,
            hrUser.getId().toString(),
            "RH",
            null,
            period.toString(),
            UUID.fromString(correlationId),
            Instant.now()));

    return saved;
  }
}
