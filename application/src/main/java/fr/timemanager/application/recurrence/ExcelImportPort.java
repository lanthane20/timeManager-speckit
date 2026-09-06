package fr.timemanager.application.recurrence;

import java.time.LocalDate;
import java.util.List;

/**
 * Port — lecture d'un planning école au format Excel (EF-028). L'implémentation ne fait que
 * transformer le fichier en dates : aucune règle métier n'est appliquée ici.
 */
public interface ExcelImportPort {

  List<LocalDate> readSchoolDates(byte[] workbook);
}
