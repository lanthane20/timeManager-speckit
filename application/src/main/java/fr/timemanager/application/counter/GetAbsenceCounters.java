package fr.timemanager.application.counter;

import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.counter.CompteurAbsence;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case EF-018, EF-019, EF-020 : compteurs de consommation calculés dynamiquement — jours pris,
 * jours validés à venir, jours en attente. Aucun solde de droits acquis n'est exposé (EF-019).
 *
 * <p>EF-034 : lorsque le type d'absence est en remise à zéro annuelle, seules les absences de
 * l'année civile en cours sont comptabilisées. C'est un simple filtre de date — il n'existe ni état
 * persistant, ni job de remise à zéro.
 */
public class GetAbsenceCounters {

  private final CounterRepository counterRepository;
  private final AbsenceTypeRepository absenceTypeRepository;

  public GetAbsenceCounters(
      CounterRepository counterRepository, AbsenceTypeRepository absenceTypeRepository) {
    this.counterRepository = counterRepository;
    this.absenceTypeRepository = absenceTypeRepository;
  }

  public List<CompteurAbsence> execute(UUID employeeId) {
    return execute(employeeId, LocalDate.now());
  }

  /** Variante testable : la date de référence sert de « aujourd'hui » et d'année civile. */
  public List<CompteurAbsence> execute(UUID employeeId, LocalDate today) {
    Map<UUID, Accumulator> byType = new LinkedHashMap<>();
    for (AbsenceCounterEntry entry : counterRepository.findCounterEntries(employeeId)) {
      Optional<AbsenceType> type = absenceTypeRepository.findById(entry.absenceTypeId());
      if (type.isEmpty() || !type.get().isQuantified() || isOutOfScope(entry, type.get(), today)) {
        continue;
      }
      byType.computeIfAbsent(entry.absenceTypeId(), id -> new Accumulator()).add(entry, today);
    }
    return byType.entrySet().stream()
        .map(entry -> entry.getValue().toCompteur(employeeId, entry.getKey()))
        .toList();
  }

  /**
   * EF-034 : une absence est imputée à l'année de sa date de début. Sur un type en remise à zéro
   * annuelle, seules les absences de l'année civile en cours entrent dans le compteur.
   */
  private boolean isOutOfScope(AbsenceCounterEntry entry, AbsenceType type, LocalDate today) {
    return type.isAnnualReset() && entry.startDate().getYear() != today.getYear();
  }

  /** Cumul des trois valeurs distinctes exposées par un compteur (EF-018). */
  private static final class Accumulator {

    private BigDecimal daysTakenPast = BigDecimal.ZERO;
    private BigDecimal daysValidatedFuture = BigDecimal.ZERO;
    private BigDecimal daysPending = BigDecimal.ZERO;

    void add(AbsenceCounterEntry entry, LocalDate today) {
      if (entry.status() == AbsenceVersionStatus.VALIDEE) {
        if (entry.endDate().isBefore(today)) {
          daysTakenPast = daysTakenPast.add(entry.workingDays());
        } else {
          daysValidatedFuture = daysValidatedFuture.add(entry.workingDays());
        }
      } else if (entry.status() == AbsenceVersionStatus.EN_ATTENTE
          || entry.status() == AbsenceVersionStatus.VALIDEE_N1) {
        daysPending = daysPending.add(entry.workingDays());
      }
    }

    CompteurAbsence toCompteur(UUID employeeId, UUID absenceTypeId) {
      return new CompteurAbsence(
          employeeId, absenceTypeId, daysTakenPast, daysValidatedFuture, daysPending);
    }
  }
}
