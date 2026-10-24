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

  /** Équipes et sous-équipes actives, pour le périmètre du planning (EF-017). */
  List<Team> findAllActive();

  Set<UUID> findMemberIdsByTeamId(UUID teamId);

  Optional<UUID> findTeamIdByMemberId(UUID employeeId);
}
