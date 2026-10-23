package fr.timemanager.infrastructure.recurrence;

import fr.timemanager.application.recurrence.ExcelImportPort;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * EF-028 : lecture d'un planning école au format Excel. La première colonne de chaque ligne porte
 * la date ; les lignes illisibles sont ignorées et journalisées plutôt que de faire échouer
 * l'import.
 */
@Component
public class ExcelImportAdapter implements ExcelImportPort {

  private static final Logger LOG = LoggerFactory.getLogger(ExcelImportAdapter.class);
  private static final List<DateTimeFormatter> TEXT_FORMATS =
      List.of(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

  @Override
  public List<LocalDate> readSchoolDates(byte[] workbookBytes) {
    List<LocalDate> dates = new ArrayList<>();
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      for (Row row : sheet) {
        readDate(row.getCell(0)).ifPresent(dates::add);
      }
    } catch (IOException | RuntimeException ex) {
      throw new IllegalArgumentException("Fichier Excel illisible : " + ex.getMessage(), ex);
    }
    return dates;
  }

  private Optional<LocalDate> readDate(Cell cell) {
    if (cell == null) {
      return Optional.empty();
    }
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      return Optional.of(cell.getLocalDateTimeCellValue().toLocalDate());
    }
    return cell.getCellType() == CellType.STRING
        ? parseText(cell.getStringCellValue().trim())
        : Optional.empty();
  }

  private Optional<LocalDate> parseText(String value) {
    for (DateTimeFormatter format : TEXT_FORMATS) {
      Optional<LocalDate> parsed = tryParse(value, format);
      if (parsed.isPresent()) {
        return parsed;
      }
    }
    LOG.debug("ligne ignorée à l'import du planning école : valeur non reconnue « {} »", value);
    return Optional.empty();
  }

  private Optional<LocalDate> tryParse(String value, DateTimeFormatter format) {
    try {
      return Optional.of(LocalDate.parse(value, format));
    } catch (DateTimeParseException ignored) {
      return Optional.empty();
    }
  }
}
