package fr.timemanager.application.admin;

import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.WorkSchedule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port — écritures de paramétrage réservées à l'admin technique (EF-031 à EF-034). Ce port n'expose
 * aucune donnée d'absence individuelle (EF-035).
 */
public interface AdminRepository {

  User saveUser(User user);

  Optional<User> findUserById(UUID id);

  List<User> findAllUsers();

  void assignUserToTeam(UUID userId, UUID teamId);

  WorkSchedule saveWorkSchedule(WorkSchedule schedule);

  List<WorkSchedule> findWorkSchedules(UUID userId);

  Team saveTeam(Team team);

  List<Team> findAllTeams();

  AbsenceType saveAbsenceType(AbsenceType absenceType);

  List<AbsenceType> findAllAbsenceTypes();
}
