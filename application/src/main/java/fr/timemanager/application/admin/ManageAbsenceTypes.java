package fr.timemanager.application.admin;

import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.application.authorization.AuthorizationService;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Use case EF-034 : gestion complète du référentiel des types d'absence — code, libellé, couleur,
 * règles de validation et de justificatif, rôles autorisés, quantification, remise à zéro annuelle
 * et statut actif/inactif.
 */
public class ManageAbsenceTypes {

  private final AdminRepository adminRepository;
  private final AbsenceTypeRepository absenceTypeRepository;
  private final AuthorizationService authorizationService;

  public ManageAbsenceTypes(
      AdminRepository adminRepository,
      AbsenceTypeRepository absenceTypeRepository,
      AuthorizationService authorizationService) {
    this.adminRepository = adminRepository;
    this.absenceTypeRepository = absenceTypeRepository;
    this.authorizationService = authorizationService;
  }

  public List<AbsenceType> list(User admin) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    return adminRepository.findAllAbsenceTypes();
  }

  public AbsenceType create(User admin, AbsenceTypeDefinition definition) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    assertCodeAvailable(definition.code(), null);
    return adminRepository.saveAbsenceType(definition.toDomain(UUID.randomUUID(), true));
  }

  public AbsenceType update(User admin, UUID typeId, AbsenceTypeDefinition definition) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    AbsenceType existing = requireType(typeId);
    assertCodeAvailable(definition.code(), existing.getCode());
    return adminRepository.saveAbsenceType(definition.toDomain(typeId, existing.isActive()));
  }

  /** EF-034 : un type désactivé disparaît des options de saisie sans perdre l'historique. */
  public AbsenceType setActive(User admin, UUID typeId, boolean active) {
    authorizationService.assertHasRole(admin, UserRole.ADMIN);
    AbsenceType existing = requireType(typeId);
    return adminRepository.saveAbsenceType(
        new AbsenceType(
            existing.getId(),
            existing.getCode(),
            existing.getLabel(),
            existing.getColor(),
            existing.isRequiresManagerValidation(),
            existing.isRequiresJustification(),
            existing.getAllowedRoles(),
            existing.isQuantified(),
            existing.isAnnualReset(),
            existing.isAllowsOverlap(),
            active));
  }

  private void assertCodeAvailable(String code, String currentCode) {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("Le code du type d'absence est obligatoire");
    }
    boolean taken =
        absenceTypeRepository.findAllActive().stream()
            .anyMatch(type -> type.getCode().equalsIgnoreCase(code) && !code.equals(currentCode));
    if (taken) {
      throw new IllegalArgumentException("Le code " + code + " est déjà utilisé");
    }
  }

  private AbsenceType requireType(UUID typeId) {
    return absenceTypeRepository
        .findById(typeId)
        .orElseThrow(() -> new IllegalArgumentException("Type d'absence introuvable : " + typeId));
  }

  /** Paramètres modifiables d'un type d'absence (EF-034). */
  public record AbsenceTypeDefinition(
      String code,
      String label,
      String color,
      boolean requiresManagerValidation,
      boolean requiresJustification,
      Set<UserRole> allowedRoles,
      boolean quantified,
      boolean annualReset,
      boolean allowsOverlap) {

    AbsenceType toDomain(UUID id, boolean active) {
      return new AbsenceType(
          id,
          code,
          label,
          color,
          requiresManagerValidation,
          requiresJustification,
          allowedRoles == null || allowedRoles.isEmpty() ? Set.of(UserRole.SALARIE) : allowedRoles,
          quantified,
          annualReset,
          allowsOverlap,
          active);
    }
  }
}
