package fr.timemanager.domain.absencetype;

import fr.timemanager.domain.user.UserRole;
import java.util.Set;
import java.util.UUID;

public class AbsenceType {

  private final UUID id;
  private final String code;
  private final String label;
  private final String color;
  private final boolean requiresManagerValidation;
  private final boolean requiresJustification;
  private final Set<UserRole> allowedRoles;
  private final boolean quantified;
  private final boolean annualReset;
  private final boolean allowsOverlap;
  private boolean active;

  public AbsenceType(
      UUID id,
      String code,
      String label,
      String color,
      boolean requiresManagerValidation,
      boolean requiresJustification,
      Set<UserRole> allowedRoles,
      boolean quantified,
      boolean annualReset,
      boolean allowsOverlap,
      boolean active) {
    this.id = id;
    this.code = code;
    this.label = label;
    this.color = color;
    this.requiresManagerValidation = requiresManagerValidation;
    this.requiresJustification = requiresJustification;
    this.allowedRoles = Set.copyOf(allowedRoles);
    this.quantified = quantified;
    this.annualReset = annualReset;
    this.allowsOverlap = allowsOverlap;
    this.active = active;
  }

  public UUID getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public String getColor() {
    return color;
  }

  public boolean isRequiresManagerValidation() {
    return requiresManagerValidation;
  }

  public boolean isRequiresJustification() {
    return requiresJustification;
  }

  public Set<UserRole> getAllowedRoles() {
    return allowedRoles;
  }

  public boolean isQuantified() {
    return quantified;
  }

  public boolean isAnnualReset() {
    return annualReset;
  }

  public boolean isAllowsOverlap() {
    return allowsOverlap;
  }

  public boolean isActive() {
    return active;
  }
}
