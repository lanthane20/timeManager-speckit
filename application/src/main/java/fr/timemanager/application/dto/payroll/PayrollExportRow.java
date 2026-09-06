package fr.timemanager.application.dto.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** EF-027 : une ligne de l'export annuel paie — un salarié, un type d'absence, une période. */
public record PayrollExportRow(
    UUID employeeId,
    String employeeLastName,
    String employeeFirstName,
    String absenceTypeCode,
    String absenceTypeLabel,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal workingDays,
    boolean regularisation) {}
