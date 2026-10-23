package fr.timemanager.application.counter;

import java.util.List;
import java.util.UUID;

/** Port — lecture des versions d'absence actives servant au calcul des compteurs (EF-018). */
public interface CounterRepository {

  List<AbsenceCounterEntry> findCounterEntries(UUID employeeId);
}
