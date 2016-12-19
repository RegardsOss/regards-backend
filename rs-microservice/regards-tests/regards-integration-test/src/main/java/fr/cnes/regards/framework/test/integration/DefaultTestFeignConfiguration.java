/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:feign.properties")
public class DefaultTestFeignConfiguration {

}
