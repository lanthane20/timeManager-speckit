package fr.timemanager.domain.absence;

public class AbsenceOverlapException extends RuntimeException {
  public AbsenceOverlapException(String message) {
    super(message);
  }
}
