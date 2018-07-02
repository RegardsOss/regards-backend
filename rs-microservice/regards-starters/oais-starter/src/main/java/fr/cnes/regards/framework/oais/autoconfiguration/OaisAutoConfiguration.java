package fr.cnes.regards.framework.oais.autoconfiguration;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;

/**
 *
 * AutoConfiguration to give access to any Bean created into this starter. For now, it is only set for spring
 * converters.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
@AutoConfigureBefore(GsonAutoConfiguration.class)
public class OaisAutoConfiguration {

}
