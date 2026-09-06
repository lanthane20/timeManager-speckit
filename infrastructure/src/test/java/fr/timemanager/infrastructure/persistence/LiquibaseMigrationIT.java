package fr.timemanager.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LiquibaseMigrationIT {

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

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void migrations001_usersTableExists() {
    var count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
  }

  @Test
  void migrations002_teamsTableExists() {
    var count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teams", Integer.class);
    assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
  }

  @Test
  void migrations003_absenceTypesPreloaded() {
    var count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM absence_types", Integer.class);
    assertThat(count).isGreaterThanOrEqualTo(3);
  }

  @Test
  void migrations004_publicHolidaysPreloaded() {
    var count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM public_holidays WHERE year = 2026", Integer.class);
    assertThat(count).isEqualTo(11);
  }

  @Test
  void migrations005_auditEventsTableExists() {
    var count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_events", Integer.class);
    assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
  }
}
