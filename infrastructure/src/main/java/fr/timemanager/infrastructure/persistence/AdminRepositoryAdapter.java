package fr.timemanager.infrastructure.persistence;

import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.WorkSchedule;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptateur des écritures de paramétrage (EF-031 à EF-034). */
@Component
public class AdminRepositoryAdapter implements fr.timemanager.application.admin.AdminRepository {

  private final UserRepository userJpaRepository;
  private final TeamRepository teamJpaRepository;
  private final AbsenceTypeRepository absenceTypeJpaRepository;
  private final WorkScheduleJpaRepository workScheduleJpaRepository;
  private final EntityManager entityManager;

  public AdminRepositoryAdapter(
      UserRepository userJpaRepository,
      TeamRepository teamJpaRepository,
      AbsenceTypeRepository absenceTypeJpaRepository,
      WorkScheduleJpaRepository workScheduleJpaRepository,
      EntityManager entityManager) {
    this.userJpaRepository = userJpaRepository;
    this.teamJpaRepository = teamJpaRepository;
    this.absenceTypeJpaRepository = absenceTypeJpaRepository;
    this.workScheduleJpaRepository = workScheduleJpaRepository;
    this.entityManager = entityManager;
  }

  @Override
  @Transactional
  public User saveUser(User user) {
    return userJpaRepository.save(UserEntity.fromDomain(user)).toDomain();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findUserById(UUID id) {
    return userJpaRepository.findById(id.toString()).map(UserEntity::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findAllUsers() {
    return userJpaRepository.findAll().stream().map(UserEntity::toDomain).toList();
  }

  /** La table de liaison n'a pas d'entité dédiée : l'affectation est idempotente. */
  @Override
  @Transactional
  public void assignUserToTeam(UUID userId, UUID teamId) {
    entityManager
        .createNativeQuery(
            """
            INSERT INTO team_members (team_id, user_id)
            SELECT :teamId, :userId FROM DUAL
            WHERE NOT EXISTS (
              SELECT 1 FROM team_members WHERE team_id = :teamId AND user_id = :userId)
            """)
        .setParameter("teamId", teamId.toString())
        .setParameter("userId", userId.toString())
        .executeUpdate();
  }

  @Override
  @Transactional
  public WorkSchedule saveWorkSchedule(WorkSchedule schedule) {
    return workScheduleJpaRepository.save(WorkScheduleEntity.fromDomain(schedule)).toDomain();
  }

  @Override
  @Transactional(readOnly = true)
  public List<WorkSchedule> findWorkSchedules(UUID userId) {
    return workScheduleJpaRepository
        .findByUserIdOrderByEffectiveFromDesc(userId.toString())
        .stream()
        .map(WorkScheduleEntity::toDomain)
        .toList();
  }

  @Override
  @Transactional
  public Team saveTeam(Team team) {
    return teamJpaRepository.save(TeamEntity.fromDomain(team)).toDomain();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Team> findAllTeams() {
    return teamJpaRepository.findAll().stream().map(TeamEntity::toDomain).toList();
  }

  @Override
  @Transactional
  public AbsenceType saveAbsenceType(AbsenceType absenceType) {
    return absenceTypeJpaRepository.save(AbsenceTypeEntity.fromDomain(absenceType)).toDomain();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AbsenceType> findAllAbsenceTypes() {
    return absenceTypeJpaRepository.findAll().stream().map(AbsenceTypeEntity::toDomain).toList();
  }
}
