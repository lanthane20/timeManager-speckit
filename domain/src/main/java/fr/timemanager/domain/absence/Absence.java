package fr.timemanager.domain.absence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Agrégat racine — regroupe toutes les versions d'une demande d'absence. */
public class Absence {

  private final UUID id;
  private final UUID employeeId;
  private final UUID absenceTypeId;
  private final UUID teamId;
  private UUID currentVersionId;
  private AbsenceSimplifiedStatus simplifiedStatus;
  private final List<VersionAbsence> versions = new ArrayList<>();
  private final Instant createdAt;
  private Instant updatedAt;

  public Absence(
      UUID id,
      UUID employeeId,
      UUID absenceTypeId,
      UUID teamId,
      AbsenceSimplifiedStatus simplifiedStatus,
      Instant createdAt) {
    this.id = id;
    this.employeeId = employeeId;
    this.absenceTypeId = absenceTypeId;
    this.teamId = teamId;
    this.simplifiedStatus = simplifiedStatus;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
  }

  public void addVersion(VersionAbsence version) {
    versions.add(version);
    this.currentVersionId = version.getId();
    this.simplifiedStatus = AbsenceSimplifiedStatus.from(version.getStatus());
    this.updatedAt = Instant.now();
  }

  /**
   * EF-007 : ajoute une version soumise à revalidation. Elle n'est pas active — l'ancienne version
   * validée reste la référence jusqu'à son approbation (EF-008).
   */
  public void addPendingVersion(VersionAbsence version) {
    versions.add(version);
    this.updatedAt = Instant.now();
  }

  /** Version en attente de décision, si une revalidation est en cours. */
  public Optional<VersionAbsence> getPendingVersion() {
    return versions.stream()
        .filter(version -> !version.isActive() && version.isUnderReview())
        .max(Comparator.comparingInt(VersionAbsence::getVersionNumber));
  }

  /** Version sur laquelle porte une décision de validation : la revalidation, sinon l'active. */
  public VersionAbsence getVersionUnderReview() {
    return getPendingVersion().orElseGet(this::getActiveVersion);
  }

  /** EF-008 : la version revalidée devient la référence, l'ancienne est archivée. */
  public void promote(VersionAbsence version) {
    versions.stream().filter(VersionAbsence::isActive).forEach(VersionAbsence::deactivate);
    version.activate();
    this.currentVersionId = version.getId();
    this.updatedAt = Instant.now();
  }

  /** Numéro à attribuer à la prochaine version de cette demande. */
  public int nextVersionNumber() {
    return versions.stream().mapToInt(VersionAbsence::getVersionNumber).max().orElse(0) + 1;
  }

  public VersionAbsence getActiveVersion() {
    return versions.stream()
        .filter(VersionAbsence::isActive)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Aucune version active pour " + id));
  }

  public void updateSimplifiedStatus() {
    VersionAbsence active = getActiveVersion();
    this.simplifiedStatus = AbsenceSimplifiedStatus.from(active.getStatus());
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getEmployeeId() {
    return employeeId;
  }

  public UUID getAbsenceTypeId() {
    return absenceTypeId;
  }

  public UUID getTeamId() {
    return teamId;
  }

  public UUID getCurrentVersionId() {
    return currentVersionId;
  }

  public AbsenceSimplifiedStatus getSimplifiedStatus() {
    return simplifiedStatus;
  }

  public List<VersionAbsence> getVersions() {
    return Collections.unmodifiableList(versions);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Reconstitution depuis la persistance — ne déclenche pas les invariants de création. Fabrique
   * équivalente à un constructeur : un paramètre par champ de l'agrégat, d'où la dérogation à
   * ParameterNumber (un builder serait une abstraction non nécessaire — Principe I, YAGNI).
   */
  @SuppressWarnings("checkstyle:ParameterNumber")
  public static Absence load(
      UUID id,
      UUID employeeId,
      UUID absenceTypeId,
      UUID teamId,
      UUID currentVersionId,
      AbsenceSimplifiedStatus simplifiedStatus,
      Instant createdAt,
      Instant updatedAt,
      List<VersionAbsence> versions) {
    var absence = new Absence(id, employeeId, absenceTypeId, teamId, simplifiedStatus, createdAt);
    absence.currentVersionId = currentVersionId;
    absence.updatedAt = updatedAt;
    versions.forEach(absence.versions::add);
    return absence;
  }
}
