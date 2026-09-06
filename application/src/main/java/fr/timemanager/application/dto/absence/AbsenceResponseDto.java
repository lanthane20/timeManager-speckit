package fr.timemanager.application.dto.absence;

import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AbsenceResponseDto(
    UUID id,
    UUID employeeId,
    UUID absenceTypeId,
    String absenceTypeLabel,
    String absenceTypeColor,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal workingDays,
    AbsenceSimplifiedStatus status,
    Instant createdAt) {}
