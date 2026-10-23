package fr.timemanager.application.dto.absence;

import fr.timemanager.domain.shared.HalfDayType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAbsenceRequestDto(
    @NotNull UUID absenceTypeId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    HalfDayType halfDayStart,
    HalfDayType halfDayEnd,
    UUID submittedByManagerId) {}
