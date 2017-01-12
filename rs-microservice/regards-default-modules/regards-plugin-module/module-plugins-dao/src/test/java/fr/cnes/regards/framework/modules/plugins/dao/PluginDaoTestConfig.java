/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.dao;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration class for unit testing of plugin's DAO.
 *
 * @author Christophe Mertz
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
public class PluginDaoTestConfig {

}
