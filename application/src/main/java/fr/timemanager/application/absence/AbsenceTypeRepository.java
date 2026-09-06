package fr.timemanager.application.absence;

import fr.timemanager.domain.absencetype.AbsenceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port — types d'absence (référentiel). */
public interface AbsenceTypeRepository {

  Optional<AbsenceType> findById(UUID id);

  List<AbsenceType> findAllActive();
}
