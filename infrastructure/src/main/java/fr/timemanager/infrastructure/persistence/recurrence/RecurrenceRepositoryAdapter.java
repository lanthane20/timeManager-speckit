package fr.timemanager.infrastructure.persistence.recurrence;

import fr.timemanager.domain.recurrence.RecurrenceInjectionLog;
import fr.timemanager.domain.recurrence.RecurrencePattern;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RecurrenceRepositoryAdapter
    implements fr.timemanager.application.recurrence.RecurrenceRepository {

  private final RecurrencePatternJpaRepository patternJpaRepository;
  private final RecurrenceInjectionLogJpaRepository logJpaRepository;

  public RecurrenceRepositoryAdapter(
      RecurrencePatternJpaRepository patternJpaRepository,
      RecurrenceInjectionLogJpaRepository logJpaRepository) {
    this.patternJpaRepository = patternJpaRepository;
    this.logJpaRepository = logJpaRepository;
  }

  @Override
  @Transactional
  public RecurrencePattern savePattern(RecurrencePattern pattern) {
    return patternJpaRepository.save(RecurrencePatternEntity.fromDomain(pattern)).toDomain();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<RecurrencePattern> findPatternById(UUID id) {
    return patternJpaRepository.findById(id.toString()).map(RecurrencePatternEntity::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RecurrencePattern> findPatternsByEmployeeId(UUID employeeId) {
    return patternJpaRepository.findByEmployeeId(employeeId.toString()).stream()
        .map(RecurrencePatternEntity::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<RecurrencePattern> findActivePatterns() {
    return patternJpaRepository.findByActiveTrue().stream()
        .map(RecurrencePatternEntity::toDomain)
        .toList();
  }

  @Override
  @Transactional
  public RecurrenceInjectionLog saveInjectionLog(RecurrenceInjectionLog log) {
    return logJpaRepository.save(RecurrenceInjectionLogEntity.fromDomain(log)).toDomain();
  }

  @Override
  @Transactional(readOnly = true)
  public List<RecurrenceInjectionLog> findInjectionLogs() {
    return logJpaRepository.findAll().stream().map(RecurrenceInjectionLogEntity::toDomain).toList();
  }
}
