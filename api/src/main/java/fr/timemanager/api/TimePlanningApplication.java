package fr.timemanager.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "fr.timemanager")
@EntityScan("fr.timemanager.infrastructure.persistence")
@EnableJpaRepositories("fr.timemanager.infrastructure.persistence")
public class TimePlanningApplication {

  public static void main(String[] args) {
    SpringApplication.run(TimePlanningApplication.class, args);
  }
}
