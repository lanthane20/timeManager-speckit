package fr.timemanager.infrastructure;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Contexte Spring Boot minimal utilisé uniquement par les tests d'intégration du module
 * infrastructure (le module n'expose pas d'application exécutable en production).
 */
@SpringBootApplication
@EntityScan(basePackages = "fr.timemanager.infrastructure.persistence")
@EnableJpaRepositories(basePackages = "fr.timemanager.infrastructure.persistence")
public class InfrastructureTestApplication {}
