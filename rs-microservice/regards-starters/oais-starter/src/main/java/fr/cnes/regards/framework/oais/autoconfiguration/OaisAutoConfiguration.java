package fr.cnes.regards.framework.oais.autoconfiguration;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import fr.cnes.regards.framework.oais.adapter.InformationPackageMapTypeAdapter;
import fr.cnes.regards.framework.oais.urn.converters.StringToUrn;

/**
 *
 * AutoConfiguration to give access to any Bean created into this starter. For now, it is only set for spring converters.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
@ComponentScan(basePackageClasses = { StringToUrn.class, InformationPackageMapTypeAdapter.class })
@AutoConfigureBefore(GsonAutoConfiguration.class)
public class OaisAutoConfiguration {

}
