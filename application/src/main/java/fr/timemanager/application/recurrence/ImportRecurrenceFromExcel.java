package fr.timemanager.application.recurrence;

import fr.timemanager.application.recurrence.InjectRecurrencePattern.InjectionResult;
import fr.timemanager.domain.recurrence.InjectionSource;
import fr.timemanager.domain.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Use case EF-028 : import d'un planning école au format Excel. Le parsing est délégué au port
 * {@link ExcelImportPort} ; l'injection réutilise les règles de {@link InjectRecurrencePattern}.
 */
public class ImportRecurrenceFromExcel {

  private final ExcelImportPort excelImportPort;
  private final InjectRecurrencePattern injectRecurrencePattern;

  public ImportRecurrenceFromExcel(
      ExcelImportPort excelImportPort, InjectRecurrencePattern injectRecurrencePattern) {
    this.excelImportPort = excelImportPort;
    this.injectRecurrencePattern = injectRecurrencePattern;
  }

  public InjectionResult execute(User author, UUID patternId, byte[] workbook) {
    List<LocalDate> dates = excelImportPort.readSchoolDates(workbook);
    if (dates.isEmpty()) {
      throw new IllegalArgumentException("Le fichier importé ne contient aucune date exploitable");
    }
    return injectRecurrencePattern.execute(author, patternId, dates, InjectionSource.IMPORT);
  }
}
