package fr.timemanager.infrastructure.persistence;

import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.team.ValidationMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "teams")
public class TeamEntity {

  @Id
  @Column(columnDefinition = "CHAR(36)")
  private String id;

  @Column(nullable = false)
  private String name;

  @Column(name = "parent_team_id", columnDefinition = "CHAR(36)")
  private String parentTeamId;

  @Column(name = "validation_mode", nullable = false)
  @Enumerated(EnumType.STRING)
  private ValidationMode validationMode;

  @Column(name = "n1_manager_id", nullable = false, columnDefinition = "CHAR(36)")
  private String n1ManagerId;

  @Column(name = "n2_manager_id", columnDefinition = "CHAR(36)")
  private String n2ManagerId;

  @Column(nullable = false)
  private boolean active;

  public static TeamEntity fromDomain(Team team) {
    var entity = new TeamEntity();
    entity.id = team.getId().toString();
    entity.name = team.getName();
    entity.parentTeamId = team.getParentTeamId() != null ? team.getParentTeamId().toString() : null;
    entity.validationMode = team.getValidationMode();
    entity.n1ManagerId = team.getN1ManagerId().toString();
    entity.n2ManagerId = team.getN2ManagerId() != null ? team.getN2ManagerId().toString() : null;
    entity.active = team.isActive();
    return entity;
  }

  public Team toDomain() {
    return new Team(
        UUID.fromString(id),
        name,
        parentTeamId != null ? UUID.fromString(parentTeamId) : null,
        validationMode,
        UUID.fromString(n1ManagerId),
        n2ManagerId != null ? UUID.fromString(n2ManagerId) : null,
        active);
  }

  public String getId() {
    return id;
  }
}
