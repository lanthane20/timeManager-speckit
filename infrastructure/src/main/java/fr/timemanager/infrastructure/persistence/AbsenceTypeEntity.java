package fr.timemanager.infrastructure.persistence;

import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.user.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "absence_types")
public class AbsenceTypeEntity {

  @Id
  @Column(columnDefinition = "CHAR(36)")
  private String id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String label;

  private String color;

  @Column(name = "requires_manager_validation", nullable = false)
  private boolean requiresManagerValidation;

  @Column(name = "requires_justification", nullable = false)
  private boolean requiresJustification;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "absence_type_roles",
      joinColumns = @JoinColumn(name = "absence_type_id", columnDefinition = "CHAR(36)"))
  @Column(name = "role")
  @Enumerated(EnumType.STRING)
  private Set<UserRole> allowedRoles = new HashSet<>();

  @Column(name = "is_quantified", nullable = false)
  private boolean quantified;

  @Column(name = "annual_reset", nullable = false)
  private boolean annualReset;

  @Column(name = "allows_overlap", nullable = false)
  private boolean allowsOverlap;

  @Column(nullable = false)
  private boolean active;

  public static AbsenceTypeEntity fromDomain(AbsenceType absenceType) {
    var entity = new AbsenceTypeEntity();
    entity.id = absenceType.getId().toString();
    entity.code = absenceType.getCode();
    entity.label = absenceType.getLabel();
    entity.color = absenceType.getColor();
    entity.requiresManagerValidation = absenceType.isRequiresManagerValidation();
    entity.requiresJustification = absenceType.isRequiresJustification();
    entity.allowedRoles = new HashSet<>(absenceType.getAllowedRoles());
    entity.quantified = absenceType.isQuantified();
    entity.annualReset = absenceType.isAnnualReset();
    entity.allowsOverlap = absenceType.isAllowsOverlap();
    entity.active = absenceType.isActive();
    return entity;
  }

  public AbsenceType toDomain() {
    return new AbsenceType(
        UUID.fromString(id),
        code,
        label,
        color,
        requiresManagerValidation,
        requiresJustification,
        allowedRoles,
        quantified,
        annualReset,
        allowsOverlap,
        active);
  }

  public String getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public boolean isAllowsOverlap() {
    return allowsOverlap;
  }
}
