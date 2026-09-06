package fr.timemanager.domain.team;

import java.util.UUID;

public class Team {

  private final UUID id;
  private final String name;
  private final UUID parentTeamId;
  private final ValidationMode validationMode;
  private final UUID n1ManagerId;
  private final UUID n2ManagerId;
  private boolean active;

  public Team(
      UUID id,
      String name,
      UUID parentTeamId,
      ValidationMode validationMode,
      UUID n1ManagerId,
      UUID n2ManagerId,
      boolean active) {
    this.id = id;
    this.name = name;
    this.parentTeamId = parentTeamId;
    this.validationMode = validationMode;
    this.n1ManagerId = n1ManagerId;
    this.n2ManagerId = n2ManagerId;
    this.active = active;
  }

  public boolean requiresDoubleValidation() {
    return validationMode == ValidationMode.N1_AND_N2;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public UUID getParentTeamId() {
    return parentTeamId;
  }

  public ValidationMode getValidationMode() {
    return validationMode;
  }

  public UUID getN1ManagerId() {
    return n1ManagerId;
  }

  public UUID getN2ManagerId() {
    return n2ManagerId;
  }

  public boolean isActive() {
    return active;
  }
}
