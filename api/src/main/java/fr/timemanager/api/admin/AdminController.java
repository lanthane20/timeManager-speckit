package fr.timemanager.api.admin;

import fr.timemanager.application.admin.ManageAbsenceTypes;
import fr.timemanager.application.admin.ManageAbsenceTypes.AbsenceTypeDefinition;
import fr.timemanager.application.admin.ManageTeams;
import fr.timemanager.application.admin.ManageUsers;
import fr.timemanager.domain.team.Team;
import fr.timemanager.domain.team.ValidationMode;
import fr.timemanager.domain.user.User;
import fr.timemanager.domain.user.UserRole;
import fr.timemanager.domain.user.WorkSchedule;
import fr.timemanager.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EF-031 à EF-035 : paramétrage réservé à l'admin technique. Aucun endpoint de ce contrôleur
 * n'expose de donnée d'absence individuelle (EF-035).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final ManageUsers manageUsers;
  private final ManageTeams manageTeams;
  private final ManageAbsenceTypes manageAbsenceTypes;
  private final CurrentUserProvider currentUserProvider;

  public AdminController(
      ManageUsers manageUsers,
      ManageTeams manageTeams,
      ManageAbsenceTypes manageAbsenceTypes,
      CurrentUserProvider currentUserProvider) {
    this.manageUsers = manageUsers;
    this.manageTeams = manageTeams;
    this.manageAbsenceTypes = manageAbsenceTypes;
    this.currentUserProvider = currentUserProvider;
  }

  // --- Utilisateurs (EF-031, EF-031b) ---

  @GetMapping("/users")
  public List<UserDto> listUsers(JwtAuthenticationToken authentication) {
    return manageUsers.list(admin(authentication)).stream().map(UserDto::from).toList();
  }

  @PostMapping("/users")
  public ResponseEntity<UserDto> createUser(
      @Valid @RequestBody CreateUserDto request, JwtAuthenticationToken authentication) {
    User created =
        manageUsers.create(
            admin(authentication),
            request.googleSub(),
            request.email(),
            request.firstName(),
            request.lastName(),
            request.roles());
    return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(created));
  }

  @PutMapping("/users/{id}")
  public UserDto updateUser(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateUserDto request,
      JwtAuthenticationToken authentication) {
    return UserDto.from(
        manageUsers.update(
            admin(authentication),
            id,
            request.firstName(),
            request.lastName(),
            request.roles(),
            request.recurrenceEnabled()));
  }

  @PutMapping("/users/{id}/deactivate")
  public UserDto deactivateUser(@PathVariable UUID id, JwtAuthenticationToken authentication) {
    return UserDto.from(manageUsers.deactivate(admin(authentication), id));
  }

  @PutMapping("/users/{id}/team/{teamId}")
  public ResponseEntity<Void> assignTeam(
      @PathVariable UUID id, @PathVariable UUID teamId, JwtAuthenticationToken authentication) {
    manageUsers.assignToTeam(admin(authentication), id, teamId);
    return ResponseEntity.noContent().build();
  }

  /** EF-031b : planning contractuel du salarié. */
  @PostMapping("/users/{id}/work-schedules")
  public ResponseEntity<WorkScheduleDto> defineWorkSchedule(
      @PathVariable UUID id,
      @Valid @RequestBody WorkScheduleRequestDto request,
      JwtAuthenticationToken authentication) {
    WorkSchedule schedule =
        manageUsers.defineWorkSchedule(
            admin(authentication),
            id,
            request.workingDays(),
            request.effectiveFrom(),
            request.effectiveUntil());
    return ResponseEntity.status(HttpStatus.CREATED).body(WorkScheduleDto.from(schedule));
  }

  @GetMapping("/users/{id}/work-schedules")
  public List<WorkScheduleDto> workSchedules(
      @PathVariable UUID id, JwtAuthenticationToken authentication) {
    return manageUsers.workSchedules(admin(authentication), id).stream()
        .map(WorkScheduleDto::from)
        .toList();
  }

  // --- Équipes (EF-032, EF-033) ---

  @GetMapping("/teams")
  public List<TeamDto> listTeams(JwtAuthenticationToken authentication) {
    return manageTeams.list(admin(authentication)).stream().map(TeamDto::from).toList();
  }

  @PostMapping("/teams")
  public ResponseEntity<TeamDto> createTeam(
      @Valid @RequestBody TeamRequestDto request, JwtAuthenticationToken authentication) {
    Team created =
        manageTeams.create(
            admin(authentication),
            request.name(),
            request.parentTeamId(),
            request.validationMode(),
            request.n1ManagerId(),
            request.n2ManagerId());
    return ResponseEntity.status(HttpStatus.CREATED).body(TeamDto.from(created));
  }

  @PutMapping("/teams/{id}")
  public TeamDto updateTeam(
      @PathVariable UUID id,
      @Valid @RequestBody TeamRequestDto request,
      JwtAuthenticationToken authentication) {
    return TeamDto.from(
        manageTeams.update(
            admin(authentication),
            id,
            request.name(),
            request.parentTeamId(),
            request.validationMode(),
            request.n1ManagerId(),
            request.n2ManagerId()));
  }

  @PutMapping("/teams/{id}/deactivate")
  public TeamDto deactivateTeam(@PathVariable UUID id, JwtAuthenticationToken authentication) {
    return TeamDto.from(manageTeams.deactivate(admin(authentication), id));
  }

  // --- Types d'absence (EF-034) ---

  @GetMapping("/absence-types")
  public List<AbsenceTypeDto> listAbsenceTypes(JwtAuthenticationToken authentication) {
    return manageAbsenceTypes.list(admin(authentication)).stream()
        .map(AbsenceTypeDto::from)
        .toList();
  }

  @PostMapping("/absence-types")
  public ResponseEntity<AbsenceTypeDto> createAbsenceType(
      @Valid @RequestBody AbsenceTypeRequestDto request, JwtAuthenticationToken authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            AbsenceTypeDto.from(
                manageAbsenceTypes.create(admin(authentication), request.toDefinition())));
  }

  @PutMapping("/absence-types/{id}")
  public AbsenceTypeDto updateAbsenceType(
      @PathVariable UUID id,
      @Valid @RequestBody AbsenceTypeRequestDto request,
      JwtAuthenticationToken authentication) {
    return AbsenceTypeDto.from(
        manageAbsenceTypes.update(admin(authentication), id, request.toDefinition()));
  }

  @PutMapping("/absence-types/{id}/active/{active}")
  public AbsenceTypeDto setAbsenceTypeActive(
      @PathVariable UUID id, @PathVariable boolean active, JwtAuthenticationToken authentication) {
    return AbsenceTypeDto.from(manageAbsenceTypes.setActive(admin(authentication), id, active));
  }

  private User admin(JwtAuthenticationToken authentication) {
    return currentUserProvider.resolve(authentication.getToken());
  }

  // --- DTO ---

  /** Vue d'un utilisateur pour l'administration — aucune donnée d'absence (EF-035). */
  public record UserDto(
      UUID id,
      String email,
      String firstName,
      String lastName,
      boolean active,
      boolean recurrenceEnabled,
      Set<UserRole> roles) {

    static UserDto from(User user) {
      return new UserDto(
          user.getId(),
          user.getEmail(),
          user.getFirstName(),
          user.getLastName(),
          user.isActive(),
          user.isRecurrenceEnabled(),
          user.getRoles());
    }
  }

  public record CreateUserDto(
      @NotBlank String googleSub,
      @NotBlank String email,
      @NotBlank String firstName,
      @NotBlank String lastName,
      @NotNull Set<UserRole> roles) {}

  public record UpdateUserDto(
      @NotBlank String firstName,
      @NotBlank String lastName,
      @NotNull Set<UserRole> roles,
      boolean recurrenceEnabled) {}

  public record WorkScheduleRequestDto(
      @NotNull Set<DayOfWeek> workingDays,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveUntil) {}

  public record WorkScheduleDto(
      UUID id, Set<DayOfWeek> workingDays, LocalDate effectiveFrom, LocalDate effectiveUntil) {

    static WorkScheduleDto from(WorkSchedule schedule) {
      return new WorkScheduleDto(
          schedule.id(),
          schedule.workingDays(),
          schedule.effectiveFrom(),
          schedule.effectiveUntil());
    }
  }

  public record TeamRequestDto(
      @NotBlank String name,
      UUID parentTeamId,
      @NotNull ValidationMode validationMode,
      @NotNull UUID n1ManagerId,
      UUID n2ManagerId) {}

  public record TeamDto(
      UUID id,
      String name,
      UUID parentTeamId,
      ValidationMode validationMode,
      UUID n1ManagerId,
      UUID n2ManagerId,
      boolean active) {

    static TeamDto from(Team team) {
      return new TeamDto(
          team.getId(),
          team.getName(),
          team.getParentTeamId(),
          team.getValidationMode(),
          team.getN1ManagerId(),
          team.getN2ManagerId(),
          team.isActive());
    }
  }

  public record AbsenceTypeRequestDto(
      @NotBlank String code,
      @NotBlank String label,
      String color,
      boolean requiresManagerValidation,
      boolean requiresJustification,
      Set<UserRole> allowedRoles,
      boolean quantified,
      boolean annualReset,
      boolean allowsOverlap) {

    AbsenceTypeDefinition toDefinition() {
      return new AbsenceTypeDefinition(
          code,
          label,
          color,
          requiresManagerValidation,
          requiresJustification,
          allowedRoles,
          quantified,
          annualReset,
          allowsOverlap);
    }
  }

  public record AbsenceTypeDto(
      UUID id,
      String code,
      String label,
      String color,
      boolean requiresManagerValidation,
      boolean requiresJustification,
      Set<UserRole> allowedRoles,
      boolean quantified,
      boolean annualReset,
      boolean allowsOverlap,
      boolean active) {

    static AbsenceTypeDto from(fr.timemanager.domain.absencetype.AbsenceType type) {
      return new AbsenceTypeDto(
          type.getId(),
          type.getCode(),
          type.getLabel(),
          type.getColor(),
          type.isRequiresManagerValidation(),
          type.isRequiresJustification(),
          type.getAllowedRoles(),
          type.isQuantified(),
          type.isAnnualReset(),
          type.isAllowsOverlap(),
          type.isActive());
    }
  }
}
