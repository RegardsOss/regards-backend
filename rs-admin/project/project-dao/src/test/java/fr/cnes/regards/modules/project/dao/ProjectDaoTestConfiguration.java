/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.microservices.core.configuration.swagger.SwaggerConfiguration;

/**
 *
 * Class ProjectDaoTestConfiguration
 *
 * Configuration class for DAO tests.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@ComponentScan(basePackages = "fr.cnes.regards",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SwaggerConfiguration.class))
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@PropertySource("classpath:tests.properties")
public class ProjectDaoTestConfiguration {

}
