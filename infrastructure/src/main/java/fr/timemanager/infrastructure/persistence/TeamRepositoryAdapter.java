package fr.timemanager.infrastructure.persistence;

import fr.timemanager.domain.team.Team;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TeamRepositoryAdapter implements fr.timemanager.application.absence.TeamRepository {

  private final TeamRepository jpaRepository;

  public TeamRepositoryAdapter(TeamRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<Team> findById(UUID id) {
    return jpaRepository.findById(id.toString()).map(TeamEntity::toDomain);
  }

  @Override
  public List<Team> findTeamsByManagerId(UUID managerId) {
    String managerIdStr = managerId.toString();
    var teams =
        new java.util.ArrayList<>(
            jpaRepository.findByN1ManagerId(managerIdStr).stream()
                .map(TeamEntity::toDomain)
                .toList());
    teams.addAll(
        jpaRepository.findByN2ManagerId(managerIdStr).stream().map(TeamEntity::toDomain).toList());
    return teams;
  }

  @Override
  public Set<UUID> findMemberIdsByTeamId(UUID teamId) {
    return jpaRepository.findMemberUserIds(teamId.toString()).stream()
        .map(UUID::fromString)
        .collect(Collectors.toSet());
  }

  @Override
  public Optional<UUID> findTeamIdByMemberId(UUID employeeId) {
    return jpaRepository.findTeamIdsByMemberId(employeeId.toString()).stream()
        .findFirst()
        .map(UUID::fromString);
  }
}
