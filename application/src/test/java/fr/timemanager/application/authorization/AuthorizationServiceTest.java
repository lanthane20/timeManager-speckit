package fr.timemanager.application.authorization;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {

  private AuthorizationService service;

  @BeforeEach
  void setUp() {
    service = new AuthorizationService();
  }

  @Test
  void selfApproval_sameId_throws() {
    UUID userId = UUID.randomUUID();
    assertThatThrownBy(() -> service.assertCannotSelfApprove(userId, userId))
        .isInstanceOf(SelfApprovalException.class);
  }

  @Test
  void selfApproval_differentIds_ok() {
    assertThatCode(() -> service.assertCannotSelfApprove(UUID.randomUUID(), UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void assertHasRole_userHasRole_ok() {
    User manager = buildUser(UserRole.MANAGER);
    assertThatCode(() -> service.assertHasRole(manager, UserRole.MANAGER))
        .doesNotThrowAnyException();
  }

  @Test
  void assertHasRole_userLacksRole_throws() {
    User salarie = buildUser(UserRole.SALARIE);
    assertThatThrownBy(() -> service.assertHasRole(salarie, UserRole.MANAGER))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void assertIsManagerOf_managerInTeam_ok() {
    User manager = buildUser(UserRole.MANAGER);
    UUID employeeId = UUID.randomUUID();
    assertThatCode(() -> service.assertIsManagerOf(manager, employeeId, Set.of(employeeId)))
        .doesNotThrowAnyException();
  }

  @Test
  void assertIsManagerOf_employeeOutsideTeam_throws() {
    User manager = buildUser(UserRole.MANAGER);
    assertThatThrownBy(
            () -> service.assertIsManagerOf(manager, UUID.randomUUID(), Set.of(UUID.randomUUID())))
        .isInstanceOf(UnauthorizedException.class);
  }

  private User buildUser(UserRole role) {
    return new User(
        UUID.randomUUID(),
        "sub_" + UUID.randomUUID(),
        "user@example.com",
        "Prénom",
        "Nom",
        true,
        null,
        false,
        Set.of(role),
        Instant.now());
  }
}
