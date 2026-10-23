package fr.timemanager.infrastructure.persistence.absence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.timemanager.domain.absence.Absence;
import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absence.VersionAbsence;
import fr.timemanager.domain.shared.DateRange;
import fr.timemanager.domain.shared.HalfDayType;
import fr.timemanager.infrastructure.persistence.AbsenceTypeEntity;
import fr.timemanager.infrastructure.persistence.AbsenceTypeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** T056 — Test d'intégration Testcontainers de l'adaptateur de persistance des absences. */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AbsenceRepositoryAdapter.class)
class AbsenceRepositoryIT {

  private static final UUID TYPE_CP = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TYPE_ECOLE = UUID.randomUUID();

  @Container
  static MariaDBContainer<?> mariadb =
      new MariaDBContainer<>("mariadb:11.4")
          .withDatabaseName("timemanager_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mariadb::getJdbcUrl);
    registry.add("spring.datasource.username", mariadb::getUsername);
    registry.add("spring.datasource.password", mariadb::getPassword);
    registry.add("spring.liquibase.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired private AbsenceRepositoryAdapter repository;
  @Autowired private AbsenceTypeRepository absenceTypeRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private UUID employeeId;
  private UUID teamId;

  @BeforeEach
  void seedReferentiel() {
    employeeId = UUID.randomUUID();
    teamId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO users (id, google_sub, email, first_name, last_name, active,
                           recurrence_enabled, created_at)
        VALUES (?, ?, ?, 'Alice', 'Martin', true, false, NOW())
        """,
        employeeId.toString(),
        "sub-" + employeeId,
        employeeId + "@timemanager.fr");
    jdbcTemplate.update(
        """
        INSERT INTO teams (id, name, validation_mode, n1_manager_id, active)
        VALUES (?, 'Equipe Test', 'N1', ?, true)
        """,
        teamId.toString(),
        employeeId.toString());
    if (absenceTypeRepository.findById(TYPE_ECOLE.toString()).isEmpty()) {
      jdbcTemplate.update(
          """
          INSERT INTO absence_types (id, code, label, color, requires_manager_validation,
                                     requires_justification, is_quantified, annual_reset,
                                     allows_overlap, active)
          VALUES (?, ?, 'Ecole', '#FF9800', true, false, true, true, true, true)
          """,
          TYPE_ECOLE.toString(),
          "ECOLE-" + TYPE_ECOLE.toString().substring(0, 8));
    }
  }

  @Test
  void save_puisFindById_restitueLAbsenceEtSaVersionActive() {
    Absence absence = newAbsence(TYPE_CP, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6));

    repository.save(absence);

    var reloaded = repository.findById(absence.getId());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getEmployeeId()).isEqualTo(employeeId);
    assertThat(reloaded.get().getSimplifiedStatus()).isEqualTo(AbsenceSimplifiedStatus.EN_ATTENTE);
    var activeVersion = reloaded.get().getActiveVersion();
    assertThat(activeVersion.getDateRange().startDate()).isEqualTo(LocalDate.of(2026, 3, 2));
    assertThat(activeVersion.getWorkingDays()).isEqualByComparingTo(new BigDecimal("5.0"));
  }

  @Test
  void findByEmployeeId_neRetourneQueLesAbsencesDuSalarie() {
    repository.save(newAbsence(TYPE_CP, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 3)));
    UUID autreSalarie = employeeId;
    employeeId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO users (id, google_sub, email, first_name, last_name, active,
                           recurrence_enabled, created_at)
        VALUES (?, ?, ?, 'Bob', 'Durand', true, false, NOW())
        """,
        employeeId.toString(),
        "sub-" + employeeId,
        employeeId + "@timemanager.fr");
    repository.save(newAbsence(TYPE_CP, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5)));

    assertThat(repository.findByEmployeeId(autreSalarie)).hasSize(1);
    assertThat(repository.findByEmployeeId(employeeId)).hasSize(1);
  }

  @Test
  void findByTeamIdAndStatus_filtreSurLeStatutSimplifie() {
    repository.save(newAbsence(TYPE_CP, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2)));

    assertThat(repository.findByTeamIdAndStatus(teamId, AbsenceSimplifiedStatus.EN_ATTENTE))
        .hasSize(1);
    assertThat(repository.findByTeamIdAndStatus(teamId, AbsenceSimplifiedStatus.VALIDEE)).isEmpty();
  }

  @Test
  void findOverlappingInfo_remonteLesPlagesQuiSeChevauchentAvecLeFlagAllowsOverlap() {
    repository.save(newAbsence(TYPE_CP, LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10)));
    repository.save(newAbsence(TYPE_ECOLE, LocalDate.of(2026, 7, 8), LocalDate.of(2026, 7, 8)));

    var overlaps =
        repository.findOverlappingInfo(
            employeeId, LocalDate.of(2026, 7, 8), LocalDate.of(2026, 7, 9));

    assertThat(overlaps).hasSize(2);
    assertThat(overlaps).anyMatch(info -> !info.allowsOverlap());
    assertThat(overlaps).anyMatch(info -> info.allowsOverlap());
  }

  @Test
  void findOverlappingInfo_ignoreLesPlagesDisjointes() {
    repository.save(newAbsence(TYPE_CP, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7)));

    var overlaps =
        repository.findOverlappingInfo(
            employeeId, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12));

    assertThat(overlaps).isEmpty();
  }

  @Test
  void findById_absenceInexistante_retourneVide() {
    assertThat(repository.findById(UUID.randomUUID())).isEmpty();
  }

  private Absence newAbsence(UUID absenceTypeId, LocalDate start, LocalDate end) {
    Instant now = Instant.now();
    UUID absenceId = UUID.randomUUID();
    var absence =
        new Absence(
            absenceId, employeeId, absenceTypeId, teamId, AbsenceSimplifiedStatus.EN_ATTENTE, now);
    long days = end.toEpochDay() - start.toEpochDay() + 1;
    absence.addVersion(
        new VersionAbsence(
            UUID.randomUUID(),
            absenceId,
            1,
            new DateRange(start, end),
            HalfDayType.NONE,
            HalfDayType.NONE,
            BigDecimal.valueOf(days).setScale(1),
            AbsenceVersionStatus.EN_ATTENTE,
            employeeId,
            true,
            now));
    return absence;
  }

  /** Garde-fou : l'entité de référentiel doit rester chargeable via son repository JPA. */
  @Test
  void absenceTypeRepository_chargeLeTypeCpDeReference() {
    AbsenceTypeEntity cp = absenceTypeRepository.findById(TYPE_CP.toString()).orElseThrow();
    assertThat(cp.getCode()).isEqualTo("CP");
  }
}
