package fr.timemanager.infrastructure.notification;

import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.infrastructure.persistence.UserEntity;
import fr.timemanager.infrastructure.persistence.UserRepository;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * EF-036, EF-037 : envoi des notifications email via templates Thymeleaf. Les envois sont
 * asynchrones et un échec n'interrompt jamais la transaction métier (contrat notifications.md).
 */
@Component
public class EmailNotificationService implements NotificationService {

  private static final Logger LOG = LoggerFactory.getLogger(EmailNotificationService.class);
  private static final String SUBJECT_PREFIX = "[TimePlanning] ";

  private final boolean enabled;
  private final String fromAddress;
  private final String baseUrl;
  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final UserRepository userRepository;

  public EmailNotificationService(
      @Value("${timemanager.notification.enabled:true}") boolean enabled,
      @Value("${timemanager.notification.from:noreply@timemanager.fr}") String fromAddress,
      @Value("${timemanager.app.base-url:http://localhost:4200}") String baseUrl,
      JavaMailSender mailSender,
      TemplateEngine templateEngine,
      UserRepository userRepository) {
    this.enabled = enabled;
    this.fromAddress = fromAddress;
    this.baseUrl = baseUrl;
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.userRepository = userRepository;
  }

  @Override
  public void notifyAbsenceCreated(UUID employeeId, UUID absenceId) {
    send(
        employeeId,
        absenceId,
        "ABSENCE_CREATED",
        "Votre demande d'absence a été enregistrée",
        "email/absence-created",
        Map.of());
  }

  @Override
  public void notifyAbsenceValidated(UUID employeeId, UUID absenceId) {
    send(
        employeeId,
        absenceId,
        "ABSENCE_VALIDATED",
        "Votre demande d'absence a été validée",
        "email/absence-validated",
        Map.of());
  }

  @Override
  public void notifyAbsenceRefused(UUID employeeId, UUID absenceId, String reason) {
    send(
        employeeId,
        absenceId,
        "ABSENCE_REFUSED",
        "Votre demande d'absence a été refusée",
        "email/absence-refused",
        Map.of("reason", reason));
  }

  @Override
  public void notifyAbsenceCancelledByManager(UUID employeeId, UUID absenceId, String reason) {
    send(
        employeeId,
        absenceId,
        "ABSENCE_CANCELLED_BY_MANAGER",
        "Votre absence validée a été annulée",
        "email/absence-cancelled",
        Map.of("reason", reason));
  }

  @Override
  public void notifyAbsenceModified(UUID employeeId, UUID absenceId) {
    send(
        employeeId,
        absenceId,
        "ABSENCE_MODIFICATION_REQUESTED",
        "Votre demande de modification a été enregistrée",
        "email/absence-modified",
        Map.of());
  }

  @Override
  public void notifyPendingValidationToManager(UUID managerId, UUID absenceId) {
    send(
        managerId,
        absenceId,
        "ABSENCE_PENDING_VALIDATION",
        "Une demande d'absence attend votre validation",
        "email/pending-validation",
        Map.of());
  }

  @Async
  void send(
      UUID recipientId,
      UUID absenceId,
      String emailType,
      String subject,
      String template,
      Map<String, Object> variables) {
    if (!enabled) {
      LOG.debug("notifications désactivées — email {} non envoyé", emailType);
    } else {
      Optional<UserEntity> recipient = userRepository.findById(recipientId.toString());
      String result =
          recipient
              .map(entity -> trySend(entity, absenceId, emailType, subject, template, variables))
              .orElse("FAILURE");
      logResult(emailType, absenceId, recipientId, result);
    }
  }

  /** Contrat notifications.md : un échec d'envoi ne fait pas échouer la transaction métier. */
  private String trySend(
      UserEntity recipient,
      UUID absenceId,
      String emailType,
      String subject,
      String template,
      Map<String, Object> variables) {
    try {
      mailSender.send(buildMessage(recipient, absenceId, subject, template, variables));
      return "SUCCESS";
    } catch (RuntimeException ex) {
      LOG.error("échec d'envoi de l'email {} pour l'absence {}", emailType, absenceId, ex);
      return "FAILURE";
    }
  }

  private MimeMessage buildMessage(
      UserEntity recipient,
      UUID absenceId,
      String subject,
      String template,
      Map<String, Object> variables) {
    Context context = new Context();
    context.setVariable("firstName", recipient.toDomain().getFirstName());
    context.setVariable("absenceUrl", baseUrl + "/my-absences?absence=" + absenceId);
    variables.forEach(context::setVariable);

    MimeMessage message = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(recipient.toDomain().getEmail());
      helper.setSubject(SUBJECT_PREFIX + subject);
      helper.setText(templateEngine.process(template, context), true);
    } catch (jakarta.mail.MessagingException ex) {
      throw new IllegalStateException("Construction de l'email impossible", ex);
    }
    return message;
  }

  private void logResult(String emailType, UUID absenceId, UUID recipientId, String result) {
    LOG.info(
        "action=EMAIL_SENT emailType={} resource=absence/{} user_id={} result={} correlation_id={}",
        emailType,
        absenceId,
        recipientId,
        result,
        MDC.get("correlation_id"));
  }
}
