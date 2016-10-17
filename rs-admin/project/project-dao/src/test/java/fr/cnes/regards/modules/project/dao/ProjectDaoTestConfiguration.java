/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

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
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
public class ProjectDaoTestConfiguration {

}
