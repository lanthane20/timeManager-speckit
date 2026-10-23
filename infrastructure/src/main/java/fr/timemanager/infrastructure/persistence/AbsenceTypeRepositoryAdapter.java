package fr.timemanager.infrastructure.persistence;

import fr.timemanager.domain.absencetype.AbsenceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AbsenceTypeRepositoryAdapter
    implements fr.timemanager.application.absence.AbsenceTypeRepository {

  private final AbsenceTypeRepository jpaRepository;

  public AbsenceTypeRepositoryAdapter(AbsenceTypeRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<AbsenceType> findById(UUID id) {
    return jpaRepository.findById(id.toString()).map(AbsenceTypeEntity::toDomain);
  }

  @Override
  public List<AbsenceType> findAllActive() {
    return jpaRepository.findByActiveTrue().stream().map(AbsenceTypeEntity::toDomain).toList();
  }
}
