package fr.timemanager.application.absence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.junit5.SimpleScenarioTest;
import fr.timemanager.application.audit.AuditRepository;
import fr.timemanager.application.notification.NotificationService;
import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDayType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Scénario d'acceptation 2 de US1 (EF-006). */
class CancelAbsenceRequestTest
    extends SimpleScenarioTest<CancelAbsenceRequestTest.CancelAbsenceStage> {

  @Test
  void scenario2_demandeEnAttente_annuleeParLeSalarie_passeAuStatutAnnulee() {
    given().une_demande_en_attente_du_salarie();

    when().le_salarie_demande_l_annulation();

    then()
        .la_demande_passe_au_statut(AbsenceSimplifiedStatus.ANNULEE)
        .and()
        .un_evenement_d_audit_d_annulation_est_enregistre();
  }

  @Test
  void annulation_demandeInconnue_estRefusee() {
    given().aucune_demande_pour_cet_identifiant();

    when().le_salarie_demande_l_annulation();

    then().l_annulation_echoue_avec(IllegalArgumentException.class);
  }

  @Test
  void annulation_demandeDUnAutreSalarie_estRefusee() {
    given().une_demande_en_attente_d_un_autre_salarie();

    when().le_salarie_demande_l_annulation();

    then().l_annulation_echoue_avec(SecurityException.class);
  }

  @Test
  void annulation_demandeDejaValidee_estRefusee() {
    given().une_demande_deja_validee_du_salarie();

    when().le_salarie_demande_l_annulation();

    then().l_annulation_echoue_avec(IllegalStateException.class);
  }

  public static class CancelAbsenceStage extends Stage<CancelAbsenceStage> {

    private final AbsenceRepository absenceRepository = mock(AbsenceRepository.class);
    private final AuditRepository auditRepository = mock(AuditRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final UUID employeeId = UUID.randomUUID();
    private UUID absenceId;
    private Absence absence;

    private Class<? extends RuntimeException> thrownExceptionType;

    public CancelAbsenceStage une_demande_en_attente_du_salarie() {
      buildAbsence(employeeId, AbsenceVersionStatus.EN_ATTENTE);
      return self();
    }

    public CancelAbsenceStage une_demande_deja_validee_du_salarie() {
      buildAbsence(employeeId, AbsenceVersionStatus.VALIDEE);
      return self();
    }

    public CancelAbsenceStage une_demande_en_attente_d_un_autre_salarie() {
      buildAbsence(UUID.randomUUID(), AbsenceVersionStatus.EN_ATTENTE);
      return self();
    }

    public CancelAbsenceStage aucune_demande_pour_cet_identifiant() {
      absenceId = UUID.randomUUID();
      Mockito.when(absenceRepository.findById(absenceId)).thenReturn(Optional.empty());
      return self();
    }

    public CancelAbsenceStage le_salarie_demande_l_annulation() {
      CancelAbsenceRequest useCase =
          new CancelAbsenceRequest(absenceRepository, auditRepository, notificationService);
      try {
        useCase.execute(employeeId, absenceId, UUID.randomUUID().toString());
      } catch (RuntimeException ex) {
        thrownExceptionType = ex.getClass();
      }
      return self();
    }

    public CancelAbsenceStage la_demande_passe_au_statut(AbsenceSimplifiedStatus status) {
      assertThat(thrownExceptionType).isNull();
      assertThat(absence.getSimplifiedStatus()).isEqualTo(status);
      return self();
    }

    public CancelAbsenceStage un_evenement_d_audit_d_annulation_est_enregistre() {
      verify(auditRepository).save(any());
      return self();
    }

    public CancelAbsenceStage l_annulation_echoue_avec(
        Class<? extends RuntimeException> exceptionType) {
      assertThat(thrownExceptionType).isEqualTo(exceptionType);
      return self();
    }

    private void buildAbsence(UUID ownerId, AbsenceVersionStatus status) {
      absenceId = UUID.randomUUID();
      UUID versionId = UUID.randomUUID();
      Instant now = Instant.now();
      VersionAbsence version =
          new VersionAbsence(
              versionId,
              absenceId,
              1,
              new DateRange(LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23)),
              HalfDayType.NONE,
              HalfDayType.NONE,
              new BigDecimal("2.0"),
              status,
              ownerId,
              true,
              now);
      absence =
          new Absence(
              absenceId,
              ownerId,
              UUID.randomUUID(),
              UUID.randomUUID(),
              AbsenceSimplifiedStatus.from(status),
              now);
      absence.addVersion(version);
      Mockito.when(absenceRepository.findById(absenceId)).thenReturn(Optional.of(absence));
      Mockito.when(absenceRepository.save(any())).thenReturn(absence);
    }
  }
}
