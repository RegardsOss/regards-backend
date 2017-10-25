/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.catalog.services.domain.dto;

import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.annotations.GetCatalogServicePluginAnnotation;

/**
 * Adds the given applicationModes and the entityTypes to a {@link PluginConfiguration}
 *
 * @author Xavier-Alexandre Brochard
 */
public class PluginConfigurationDto extends PluginConfiguration {

    private final Set<ServiceScope> applicationModes;

    private final Set<EntityType> entityTypes;

    /**
     * Finds the application mode of the given plugin configuration
     */
    private static final Function<PluginConfiguration, CatalogServicePlugin> GET_CATALOG_SERVICE_PLUGIN_ANNOTATION = new GetCatalogServicePluginAnnotation();

    /**
     * For a {@link PluginConfiguration}, return its corresponding DTO, in which we have added fields <code>applicationModes</code>
     * and <code>entityTypes</code>
     *
     * @param pPluginConfiguration
     */
    public PluginConfigurationDto(PluginConfiguration pPluginConfiguration) {
        super(pPluginConfiguration);
        applicationModes = Sets
                .newHashSet(GET_CATALOG_SERVICE_PLUGIN_ANNOTATION.apply(pPluginConfiguration).applicationModes());
        entityTypes = Sets.newHashSet(GET_CATALOG_SERVICE_PLUGIN_ANNOTATION.apply(pPluginConfiguration).entityTypes());
    }

    /**
     * @return the applicationModes
     */
    public Set<ServiceScope> getApplicationModes() {
        return applicationModes;
    }

    /**
     * @return the entityTypes
     */
    public Set<EntityType> getEntityTypes() {
        return entityTypes;
    }

}
