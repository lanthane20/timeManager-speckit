package fr.timemanager.infrastructure.persistence;

import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

  @Id
  @Column(columnDefinition = "CHAR(36)")
  private String id;

  @Column(name = "google_sub", nullable = false, unique = true)
  private String googleSub;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "non_working_days_pattern")
  private String nonWorkingDaysPattern;

  @Column(name = "recurrence_enabled", nullable = false)
  private boolean recurrenceEnabled;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id", columnDefinition = "CHAR(36)"))
  @Column(name = "role")
  @Enumerated(EnumType.STRING)
  private Set<UserRole> roles = new HashSet<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public static UserEntity fromDomain(User user) {
    var entity = new UserEntity();
    entity.id = user.getId().toString();
    entity.googleSub = user.getGoogleSub();
    entity.email = user.getEmail();
    entity.firstName = user.getFirstName();
    entity.lastName = user.getLastName();
    entity.active = user.isActive();
    entity.nonWorkingDaysPattern = user.getNonWorkingDaysPattern();
    entity.recurrenceEnabled = user.isRecurrenceEnabled();
    entity.roles = new HashSet<>(user.getRoles());
    entity.createdAt = user.getCreatedAt();
    return entity;
  }

  public User toDomain() {
    return new User(
        UUID.fromString(id),
        googleSub,
        email,
        firstName,
        lastName,
        active,
        nonWorkingDaysPattern,
        recurrenceEnabled,
        roles,
        createdAt);
  }

  public String getId() {
    return id;
  }

  public String getGoogleSub() {
    return googleSub;
  }

  public Set<UserRole> getRoles() {
    return roles;
  }
}
