package fr.timemanager.application.authorization;

public class SelfApprovalException extends RuntimeException {
  public SelfApprovalException(String message) {
    super(message);
  }
}
