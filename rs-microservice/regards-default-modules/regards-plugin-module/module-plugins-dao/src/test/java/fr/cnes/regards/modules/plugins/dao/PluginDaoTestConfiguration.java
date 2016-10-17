/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 *
 * Class PluginDaoTestConfiguration
 *
 * Configuration class for DAO tests.
 *
 * @author cmertz
 */
@Configuration
@ComponentScan(basePackages = "fr.cnes.regards.modules.plugins")
@PropertySource("classpath:tests.properties")
@EnableAutoConfiguration
public class PluginDaoTestConfiguration {

}
