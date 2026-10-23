package fr.timemanager.application.payroll;

import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.domain.payroll.Regularisation;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.time.YearMonth;
import java.util.List;

/** Use case EF-026 : consultation des régularisations, filtrable par période. */
public class GetRegularisations {

  private final RegularisationRepository regularisationRepository;
  private final PeriodClosureRepository periodClosureRepository;
  private final AuthorizationService authorizationService;

  public GetRegularisations(
      RegularisationRepository regularisationRepository,
      PeriodClosureRepository periodClosureRepository,
      AuthorizationService authorizationService) {
    this.regularisationRepository = regularisationRepository;
    this.periodClosureRepository = periodClosureRepository;
    this.authorizationService = authorizationService;
  }

  /**
   * @param period période demandée ; {@code null} = toutes les régularisations
   */
  public List<Regularisation> execute(User hrUser, YearMonth period) {
    authorizationService.assertHasRole(hrUser, UserRole.RH);
    if (period == null) {
      return regularisationRepository.findAll();
    }
    return periodClosureRepository
        .findByYearAndMonth(period.getYear(), period.getMonthValue())
        .map(closure -> regularisationRepository.findByPeriodClosureId(closure.getId()))
        .orElseGet(List::of);
  }
}
