package fr.timemanager.api.testsupport;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Contexte minimal partagé par les tests {@code @WebMvcTest} : évite que TimePlanningApplication
 * (avec @EnableJpaRepositories) ne serve de configuration et n'exige un DataSource.
 *
 * <p>Les filtres d'exclusion reproduisent ceux de {@code @SpringBootApplication} : sans eux, le
 * scan ignorerait le {@code TypeExcludeFilter} posé par {@code @WebMvcTest} et instancierait tous
 * les contrôleurs, pas seulement celui sous test.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableMethodSecurity(prePostEnabled = true)
@ComponentScan(
    basePackages = "fr.timemanager.api",
    excludeFilters = {
      @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
      @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
    })
public class WebMvcTestConfig {}
