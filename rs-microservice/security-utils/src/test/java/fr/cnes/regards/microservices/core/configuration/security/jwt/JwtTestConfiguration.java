/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.configuration.security.jwt;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 *
 * JWT test configuration
 *
 * @author msordi
 *
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.security.utils.jwt" })
@PropertySource("classpath:jwt.properties")
public class JwtTestConfiguration {

}
