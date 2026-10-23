package fr.timemanager.application.absence;

import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.team.Team;

/**
 * EF-009, EF-012 : route une décision de validation selon le mode configuré pour l'équipe. En
 * {@code N1_ONLY} la validation du N+1 est finale ; en {@code N1_AND_N2} elle place la demande en
 * attente du N+2, que celui-ci peut aussi valider directement sans attendre le N+1.
 */
public class WorkflowEngine {

  /** Statut atteint lorsque le N+1 valide la demande. */
  public AbsenceVersionStatus statusAfterN1Validation(Team team) {
    return team.requiresDoubleValidation()
        ? AbsenceVersionStatus.VALIDEE_N1
        : AbsenceVersionStatus.VALIDEE;
  }

  /** Le N+2 n'intervient que sur les équipes configurées en double validation. */
  public boolean allowsN2Validation(Team team) {
    return team.requiresDoubleValidation();
  }
}
