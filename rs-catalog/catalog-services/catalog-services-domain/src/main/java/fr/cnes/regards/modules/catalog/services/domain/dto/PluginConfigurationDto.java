/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.catalog.services.domain.dto;

import java.util.Set;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.models.domain.EntityType;

/**
 * Adds the given applicationModes and the entityTypes to a {@link PluginConfiguration}
 *
 * @author Xavier-Alexandre Brochard
 */
public class PluginConfigurationDto extends PluginConfiguration {

    private final Set<ServiceScope> applicationModes;

    private final Set<EntityType> entityTypes;

    /**
     * Constructor
     * @param pPluginConfiguration
     * @param pApplicationModes
     * @param pEntityTypes
     */
    public PluginConfigurationDto(PluginConfiguration pPluginConfiguration, Set<ServiceScope> pApplicationModes,
            Set<EntityType> pEntityTypes) {
        super(pPluginConfiguration);
        applicationModes = pApplicationModes;
        entityTypes = pEntityTypes;
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
