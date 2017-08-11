package fr.cnes.regards.framework.urn;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.urn.converters.StringToUrn;

/**
 *
 * AutoConfiguration to give access to any Bean created into this starter. For now, it is only set for spring converters.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
@ComponentScan(basePackageClasses = StringToUrn.class)
public class OaisAutoConfiguration {

}
