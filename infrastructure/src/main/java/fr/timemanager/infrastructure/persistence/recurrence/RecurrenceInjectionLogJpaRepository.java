package fr.timemanager.infrastructure.persistence.recurrence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrenceInjectionLogJpaRepository
    extends JpaRepository<RecurrenceInjectionLogEntity, String> {}
