package fr.timemanager.domain.payroll;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** EF-024 : période mensuelle de paie. Une fois close, les données de paie sont figées. */
public class PeriodeCloture {

  private final UUID id;
  private final int year;
  private final int month;
  private Instant closedAt;
  private UUID closedByUserId;
  private PeriodStatus status;

  public PeriodeCloture(UUID id, int year, int month) {
    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("Mois invalide : " + month);
    }
    this.id = id;
    this.year = year;
    this.month = month;
    this.status = PeriodStatus.OPEN;
  }

  /** EF-024 : la clôture est une action unique et non répétable. */
  public void close(UUID closedByUserId, Instant closedAt) {
    if (status == PeriodStatus.CLOSED) {
      throw new IllegalStateException("La période " + yearMonth() + " est déjà clôturée");
    }
    this.status = PeriodStatus.CLOSED;
    this.closedByUserId = closedByUserId;
    this.closedAt = closedAt;
  }

  public boolean isClosed() {
    return status == PeriodStatus.CLOSED;
  }

  public YearMonth yearMonth() {
    return YearMonth.of(year, month);
  }

  /** Une absence appartient à la période si elle en chevauche au moins un jour. */
  public boolean covers(LocalDate startDate, LocalDate endDate) {
    YearMonth period = yearMonth();
    return !endDate.isBefore(period.atDay(1)) && !startDate.isAfter(period.atEndOfMonth());
  }

  public UUID getId() {
    return id;
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public UUID getClosedByUserId() {
    return closedByUserId;
  }

  public PeriodStatus getStatus() {
    return status;
  }

  /** Reconstitution depuis la persistance. */
  public static PeriodeCloture load(
      UUID id, int year, int month, Instant closedAt, UUID closedByUserId, PeriodStatus status) {
    var period = new PeriodeCloture(id, year, month);
    period.closedAt = closedAt;
    period.closedByUserId = closedByUserId;
    period.status = status;
    return period;
  }
}
