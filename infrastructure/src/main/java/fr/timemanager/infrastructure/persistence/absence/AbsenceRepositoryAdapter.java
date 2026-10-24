package fr.timemanager.infrastructure.persistence.absence;

import fr.timemanager.application.absence.AbsenceRepository;
import fr.timemanager.application.dto.payroll.HrAbsenceSearchCriteria;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.ExistingAbsenceInfo;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.infrastructure.persistence.AbsenceTypeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AbsenceRepositoryAdapter implements AbsenceRepository {

  private final AbsenceJpaRepository absenceJpaRepository;
  private final AbsenceTypeRepository absenceTypeRepository;

  public AbsenceRepositoryAdapter(
      AbsenceJpaRepository absenceJpaRepository, AbsenceTypeRepository absenceTypeRepository) {
    this.absenceJpaRepository = absenceJpaRepository;
    this.absenceTypeRepository = absenceTypeRepository;
  }

  @Override
  @Transactional
  public Absence save(Absence absence) {
    var entity = AbsenceEntity.fromDomain(absence);
    var persisted = absenceJpaRepository.findById(absence.getId());
    // Le domaine ne porte pas les compteurs @Version : on reprend ceux de la base pour que la
    // fusion d'une absence rechargée ne soit pas prise pour une modification concurrente.
    persisted.ifPresent(entity::keepLockVersionOf);
    absence.getVersions().forEach(v -> entity.getVersions().add(toEntity(v, entity, persisted)));
    // absences.current_version_id référence absence_versions, qui référence absences :
    // la version courante n'est renseignée qu'après l'enregistrement des versions.
    entity.setCurrentVersionId(null);
    var savedEntity = absenceJpaRepository.save(entity);
    savedEntity.setCurrentVersionId(absence.getCurrentVersionId());
    return savedEntity.toDomain();
  }

  private static AbsenceVersionEntity toEntity(
      VersionAbsence version, AbsenceEntity absenceEntity, Optional<AbsenceEntity> persisted) {
    var entity = AbsenceVersionEntity.fromDomain(version, absenceEntity);
    persisted.stream()
        .flatMap(p -> p.getVersions().stream())
        .filter(v -> v.getId().equals(version.getId()))
        .findFirst()
        .ifPresent(entity::keepLockVersionOf);
    return entity;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Absence> findById(UUID id) {
    return absenceJpaRepository.findById(id).map(AbsenceEntity::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Absence> findByEmployeeId(UUID employeeId) {
    return absenceJpaRepository.findByEmployeeId(employeeId).stream()
        .map(AbsenceEntity::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Absence> findByTeamIdAndStatus(UUID teamId, AbsenceSimplifiedStatus status) {
    return absenceJpaRepository.findByTeamIdAndSimplifiedStatus(teamId, status).stream()
        .map(AbsenceEntity::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ExistingAbsenceInfo> findOverlappingInfo(
      UUID employeeId, LocalDate start, LocalDate end) {
    return absenceJpaRepository.findOverlapping(employeeId, start, end).stream()
        .map(entity -> toExistingAbsenceInfo(entity))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Absence> search(HrAbsenceSearchCriteria criteria) {
    return absenceJpaRepository
        .search(
            criteria.employeeId(),
            criteria.teamId(),
            criteria.absenceTypeId(),
            criteria.from(),
            criteria.to())
        .stream()
        .map(AbsenceEntity::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Absence> findValidatedInYear(int year) {
    return absenceJpaRepository
        .findValidatedBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
        .stream()
        .map(AbsenceEntity::toDomain)
        .toList();
  }

  private ExistingAbsenceInfo toExistingAbsenceInfo(AbsenceEntity entity) {
    var activeVersion =
        entity.getVersions().stream()
            .filter(AbsenceVersionEntity::isActive)
            .findFirst()
            .orElseThrow();
    var dateRange = new DateRange(activeVersion.getStartDate(), activeVersion.getEndDate());
    boolean allowsOverlap =
        absenceTypeRepository
            .findById(entity.getAbsenceTypeId().toString())
            .map(t -> t.isAllowsOverlap())
            .orElse(false);
    return new ExistingAbsenceInfo(dateRange, allowsOverlap);
  }
}
