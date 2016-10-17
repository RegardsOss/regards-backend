/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 *
 * Class PluginDaoTestConfig
 *
 * Configuration class for DAO tests.
 *
 * @author cmertz
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
public class PluginDaoTestConfig {

}
