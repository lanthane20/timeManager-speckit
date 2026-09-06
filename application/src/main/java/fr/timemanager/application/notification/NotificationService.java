package fr.timemanager.application.notification;

import java.util.UUID;

/** Port — envoi de notifications (email). Implémentation désactivable via NOTIFICATION_ENABLED. */
public interface NotificationService {

  void notifyAbsenceCreated(UUID employeeId, UUID absenceId);

  void notifyAbsenceValidated(UUID employeeId, UUID absenceId);

  void notifyAbsenceRefused(UUID employeeId, UUID absenceId, String reason);

  void notifyAbsenceCancelledByManager(UUID employeeId, UUID absenceId, String reason);

  void notifyAbsenceModified(UUID employeeId, UUID absenceId);

  void notifyPendingValidationToManager(UUID managerId, UUID absenceId);
}
