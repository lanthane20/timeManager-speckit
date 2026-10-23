package fr.timemanager.application.dto.planning;

import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Une absence telle qu'affichée sur le planning mensuel d'équipe (EF-015) : identité du salarié,
 * code et couleur du type, plage de dates et statut simplifié.
 */
public record PlanningEntryDto(
    UUID absenceId,
    UUID employeeId,
    String employeeName,
    UUID teamId,
    String absenceTypeCode,
    String absenceTypeLabel,
    String absenceTypeColor,
    LocalDate startDate,
    LocalDate endDate,
    AbsenceSimplifiedStatus status) {}
