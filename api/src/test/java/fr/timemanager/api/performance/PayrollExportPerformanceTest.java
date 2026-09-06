package fr.timemanager.api.performance;

import static org.assertj.core.api.Assertions.assertThat;

import fr.timemanager.application.dto.payroll.PayrollExportRow;
import fr.timemanager.infrastructure.export.ExcelExportAdapter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * CS-005 : l'export annuel paie doit être généré en moins de 30 secondes quelle que soit la
 * volumétrie annuelle. La volumétrie de référence du projet est inférieure à 10 000 demandes par an
 * (plan.md §Échelle) — le test prend une marge d'un facteur 5.
 */
class PayrollExportPerformanceTest {

  private static final int ROWS = 50_000;
  private static final Duration BUDGET = Duration.ofSeconds(30);

  private final ExcelExportAdapter adapter = new ExcelExportAdapter();

  @Test
  void cs005_lExportDe50000LignesResteSousTrenteSecondes() {
    List<PayrollExportRow> rows = buildRows();

    long startedAt = System.nanoTime();
    byte[] workbook = adapter.buildAnnualExport(2026, rows);
    Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

    assertThat(workbook).isNotEmpty();
    assertThat(elapsed)
        .as("génération de %d lignes en %d ms", ROWS, elapsed.toMillis())
        .isLessThan(BUDGET);
  }

  @Test
  void cs005_unExportVideEstGenereInstantanement() {
    long startedAt = System.nanoTime();
    byte[] workbook = adapter.buildAnnualExport(2026, List.of());
    Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

    assertThat(workbook).isNotEmpty();
    assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
  }

  private List<PayrollExportRow> buildRows() {
    List<PayrollExportRow> rows = new ArrayList<>(ROWS);
    for (int index = 0; index < ROWS; index++) {
      LocalDate start = LocalDate.of(2026, 1 + index % 12, 1 + index % 28);
      rows.add(
          new PayrollExportRow(
              UUID.randomUUID(),
              "Nom" + index,
              "Prénom" + index,
              index % 2 == 0 ? "CP" : "RTT",
              index % 2 == 0 ? "Congés payés" : "RTT",
              start,
              start.plusDays(2),
              new BigDecimal("3.0"),
              index % 100 == 0));
    }
    return rows;
  }
}
