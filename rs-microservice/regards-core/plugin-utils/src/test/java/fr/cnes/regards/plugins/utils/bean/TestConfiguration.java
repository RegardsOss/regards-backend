/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

/**
 *
 * Class TestConfiguration
 *
 * Plugin utils Spring context configuration class
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@PropertySource("application-test.properties")
@ComponentScan
public class TestConfiguration {

}
