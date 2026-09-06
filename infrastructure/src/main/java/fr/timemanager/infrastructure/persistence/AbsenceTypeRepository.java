package fr.timemanager.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbsenceTypeRepository extends JpaRepository<AbsenceTypeEntity, String> {

  Optional<AbsenceTypeEntity> findByCode(String code);

  List<AbsenceTypeEntity> findByActiveTrue();
}
