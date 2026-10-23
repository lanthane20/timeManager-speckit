package fr.timemanager.domain.counter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Agrégat calculé — non persisté directement. Calculé dynamiquement depuis absence_versions. La
 * "remise à zéro annuelle" est un simple filtre de date dans la requête (EF-034).
 */
public record CompteurAbsence(
    UUID employeeId,
    UUID absenceTypeId,
    BigDecimal daysTakenPast,
    BigDecimal daysValidatedFuture,
    BigDecimal daysPending) {

  public BigDecimal totalValidated() {
    return daysTakenPast.add(daysValidatedFuture);
  }
}
