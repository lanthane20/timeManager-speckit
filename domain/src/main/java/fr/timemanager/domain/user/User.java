package fr.timemanager.domain.user;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class User {

  private final UUID id;
  private final String googleSub;
  private final String email;
  private final String firstName;
  private final String lastName;
  private boolean active;
  private String nonWorkingDaysPattern;
  private boolean recurrenceEnabled;
  private final Set<UserRole> roles;
  private final Instant createdAt;

  public User(
      UUID id,
      String googleSub,
      String email,
      String firstName,
      String lastName,
      boolean active,
      String nonWorkingDaysPattern,
      boolean recurrenceEnabled,
      Set<UserRole> roles,
      Instant createdAt) {
    this.id = id;
    this.googleSub = googleSub;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.active = active;
    this.nonWorkingDaysPattern = nonWorkingDaysPattern;
    this.recurrenceEnabled = recurrenceEnabled;
    this.roles = Set.copyOf(roles);
    this.createdAt = createdAt;
  }

  public boolean hasRole(UserRole role) {
    return roles.contains(role);
  }

  public UUID getId() {
    return id;
  }

  public String getGoogleSub() {
    return googleSub;
  }

  public String getEmail() {
    return email;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public boolean isActive() {
    return active;
  }

  public String getNonWorkingDaysPattern() {
    return nonWorkingDaysPattern;
  }

  public boolean isRecurrenceEnabled() {
    return recurrenceEnabled;
  }

  public Set<UserRole> getRoles() {
    return roles;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
