package fr.timemanager.application.counter;

import fr.timemanager.domain.absence.AbsenceVersionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Ligne brute de calcul des compteurs (EF-018) : une version d'absence active d'un salarié. Les
 * règles d'agrégation et le filtre de remise à zéro annuelle sont appliqués côté application.
 */
public record AbsenceCounterEntry(
    UUID absenceTypeId,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal workingDays,
    AbsenceVersionStatus status) {}
