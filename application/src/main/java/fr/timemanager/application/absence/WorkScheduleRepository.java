package fr.timemanager.application.absence;

import fr.timemanager.domain.user.WorkSchedule;
import java.util.List;
import java.util.UUID;

/** Port — plannings contractuels (EF-031b), du plus récent au plus ancien. */
public interface WorkScheduleRepository {

  List<WorkSchedule> findByUserId(UUID userId);
}
