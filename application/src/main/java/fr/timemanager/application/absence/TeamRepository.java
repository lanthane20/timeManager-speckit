package fr.timemanager.application.absence;

import fr.timemanager.domain.team.Team;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Port — équipes. */
public interface TeamRepository {

  Optional<Team> findById(UUID id);

  List<Team> findTeamsByManagerId(UUID managerId);

  Set<UUID> findMemberIdsByTeamId(UUID teamId);

  Optional<UUID> findTeamIdByMemberId(UUID employeeId);
}
