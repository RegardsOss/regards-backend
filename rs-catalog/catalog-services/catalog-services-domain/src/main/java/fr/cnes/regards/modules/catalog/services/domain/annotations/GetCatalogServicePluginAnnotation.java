/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.catalog.services.domain.annotations;

import java.util.function.Function;

import org.springframework.core.annotation.AnnotationUtils;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.plugins.utils.PluginUtilsRuntimeException;

/**
 * Function returning the {@link CatalogServicePlugin} annotation on given {@link PluginConfiguration}
 *
 * @author Xavier-Alexandre Brochard
 */
public class GetCatalogServicePluginAnnotation implements Function<PluginConfiguration, CatalogServicePlugin> {

    @Override
    public CatalogServicePlugin apply(PluginConfiguration pPluginConfiguration) {
        try {
            return AnnotationUtils.findAnnotation(Class.forName(pPluginConfiguration.getPluginClassName()),
                                                  CatalogServicePlugin.class);
        } catch (ClassNotFoundException e) {
            // No exception should occurs there. If any occurs it should set the application into maintenance mode so we
            // can safely rethrow as a runtime
            throw new PluginUtilsRuntimeException("Could not instanciate plugin", e);
        }
    }

}
