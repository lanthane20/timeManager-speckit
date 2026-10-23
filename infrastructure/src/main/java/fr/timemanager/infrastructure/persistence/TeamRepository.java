package fr.timemanager.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<TeamEntity, String> {

  List<TeamEntity> findByActiveTrue();

  List<TeamEntity> findByN1ManagerId(String n1ManagerId);

  List<TeamEntity> findByN2ManagerId(String n2ManagerId);

  @Query(value = "SELECT user_id FROM team_members WHERE team_id = :teamId", nativeQuery = true)
  List<String> findMemberUserIds(@Param("teamId") String teamId);

  @Query(value = "SELECT team_id FROM team_members WHERE user_id = :userId", nativeQuery = true)
  List<String> findTeamIdsByMemberId(@Param("userId") String userId);
}
