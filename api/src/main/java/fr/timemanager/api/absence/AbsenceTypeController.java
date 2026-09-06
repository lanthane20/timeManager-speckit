package fr.timemanager.api.absence;

import fr.timemanager.application.absence.AbsenceTypeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Référentiel en lecture seule des types d'absence actifs — nécessaire au formulaire de création
 * d'une demande (US1). L'administration du référentiel reste dans {@code /api/v1/admin}.
 */
@RestController
@RequestMapping("/api/v1/absence-types")
public class AbsenceTypeController {

  private final AbsenceTypeRepository absenceTypeRepository;

  public AbsenceTypeController(AbsenceTypeRepository absenceTypeRepository) {
    this.absenceTypeRepository = absenceTypeRepository;
  }

  @GetMapping
  @PreAuthorize("hasRole('SALARIE')")
  public List<AbsenceTypeSummaryDto> listActive() {
    return absenceTypeRepository.findAllActive().stream()
        .map(t -> new AbsenceTypeSummaryDto(t.getId(), t.getCode(), t.getLabel(), t.getColor()))
        .toList();
  }

  /** Vue publique d'un type d'absence — n'expose aucune règle de gestion interne. */
  public record AbsenceTypeSummaryDto(UUID id, String code, String label, String color) {}
}
