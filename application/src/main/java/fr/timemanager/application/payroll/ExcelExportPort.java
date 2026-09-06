package fr.timemanager.application.payroll;

import fr.timemanager.application.dto.payroll.PayrollExportRow;
import java.util.List;

/**
 * Port — génération de l'export Excel annuel de paie (EF-027). L'implémentation doit rester sous 30
 * s quelle que soit la volumétrie annuelle (CS-005).
 */
public interface ExcelExportPort {

  byte[] buildAnnualExport(int year, List<PayrollExportRow> rows);
}
