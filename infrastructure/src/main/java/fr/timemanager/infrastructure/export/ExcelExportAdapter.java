package fr.timemanager.infrastructure.export;

import fr.timemanager.application.dto.payroll.PayrollExportRow;
import fr.timemanager.application.payroll.ExcelExportPort;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * EF-027 : génération de l'export Excel annuel de paie. Utilise l'écriture en flux (SXSSF) afin de
 * garder une empreinte mémoire constante et de tenir la contrainte CS-005 (moins de 30 secondes)
 * quelle que soit la volumétrie annuelle.
 */
@Component
public class ExcelExportAdapter implements ExcelExportPort {

  private static final String[] HEADERS = {
    "Nom",
    "Prénom",
    "Code type",
    "Type d'absence",
    "Date de début",
    "Date de fin",
    "Jours ouvrés",
    "Régularisation"
  };

  /** Taille de la fenêtre de lignes maintenues en mémoire par SXSSF. */
  private static final int ROW_ACCESS_WINDOW = 500;

  @Override
  public byte[] buildAnnualExport(int year, List<PayrollExportRow> rows) {
    try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Absences " + year);
      writeHeader(workbook, sheet);
      int rowIndex = 1;
      for (PayrollExportRow row : rows) {
        writeRow(sheet.createRow(rowIndex++), row);
      }
      workbook.write(output);
      workbook.dispose();
      return output.toByteArray();
    } catch (IOException ex) {
      throw new UncheckedIOException("Génération de l'export Excel impossible", ex);
    }
  }

  private void writeHeader(SXSSFWorkbook workbook, Sheet sheet) {
    Font bold = workbook.createFont();
    bold.setBold(true);
    CellStyle headerStyle = workbook.createCellStyle();
    headerStyle.setFont(bold);

    Row header = sheet.createRow(0);
    for (int column = 0; column < HEADERS.length; column++) {
      Cell cell = header.createCell(column);
      cell.setCellValue(HEADERS[column]);
      cell.setCellStyle(headerStyle);
    }
  }

  private void writeRow(Row target, PayrollExportRow row) {
    target.createCell(0).setCellValue(row.employeeLastName());
    target.createCell(1).setCellValue(row.employeeFirstName());
    target.createCell(2).setCellValue(row.absenceTypeCode());
    target.createCell(3).setCellValue(row.absenceTypeLabel());
    target.createCell(4).setCellValue(row.startDate().toString());
    target.createCell(5).setCellValue(row.endDate().toString());
    target.createCell(6).setCellValue(row.workingDays().doubleValue());
    target.createCell(7).setCellValue(row.regularisation() ? "Oui" : "Non");
  }
}
