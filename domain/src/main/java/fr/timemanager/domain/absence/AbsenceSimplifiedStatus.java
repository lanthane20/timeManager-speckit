package fr.timemanager.domain.absence;

public enum AbsenceSimplifiedStatus {
  EN_ATTENTE,
  VALIDEE,
  REFUSEE,
  ANNULEE;

  public static AbsenceSimplifiedStatus from(AbsenceVersionStatus versionStatus) {
    return switch (versionStatus) {
      case EN_ATTENTE, VALIDEE_N1 -> EN_ATTENTE;
      case VALIDEE -> VALIDEE;
      case REFUSEE -> REFUSEE;
      case ANNULEE -> ANNULEE;
    };
  }
}
