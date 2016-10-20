/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt;

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
@ComponentScan
@PropertySource("classpath:jwt.properties")
public class JwtTestConfiguration {

}
