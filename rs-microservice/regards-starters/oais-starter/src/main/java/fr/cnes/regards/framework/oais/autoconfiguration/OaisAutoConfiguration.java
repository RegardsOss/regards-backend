package fr.cnes.regards.framework.oais.autoconfiguration;

import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * AutoConfiguration to give access to any Bean created into this starter. For now, it is only set for spring
 * converters.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@AutoConfiguration(before = GsonAutoConfiguration.class)
public class OaisAutoConfiguration {

}
