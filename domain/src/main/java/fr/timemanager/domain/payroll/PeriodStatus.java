package fr.timemanager.domain.payroll;

/** État d'une période de paie mensuelle. */
public enum PeriodStatus {
  /** Période ouverte : les validations alimentent normalement la paie. */
  OPEN,
  /** Période figée (EF-024) : toute validation ultérieure devient une régularisation (EF-025). */
  CLOSED
}
