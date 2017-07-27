/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.services.domain.aggregator;

import java.net.URL;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.models.domain.EntityType;

/**
 * DTO class representing either a {@link PluginConfiguration}, either a {@link UIPluginConfiguration}
 *
 * @author Xavier-Alexandre Brochard
 */
public class PluginServiceDto {

    private final Long configId;

    private final String label;

    private final URL icon;

    private final Set<ServiceScope> applicationModes;

    private final Set<EntityType> entityTypes;

    private final PluginServiceType type;

    /**
     * Constructor. It is private in order to force the caller to use one of the 'from' builder methods
     * @param pConfigId
     * @param pLabel
     * @param pIcon
     * @param pApplicationModes
     * @param pEntityTypes
     * @param pType
     */
    private PluginServiceDto(Long pConfigId, String pLabel, URL pIcon, Set<ServiceScope> pApplicationModes,
            Set<EntityType> pEntityTypes, PluginServiceType pType) {
        super();
        configId = pConfigId;
        label = pLabel;
        icon = pIcon;
        applicationModes = pApplicationModes;
        entityTypes = pEntityTypes;
        type = pType;
    }

    /**
     * Build a new instance from the given {@link PluginConfiguration}
     * @param pPluginConfiguration
     * @return the new instance
     */
    public static final PluginServiceDto fromPluginConfiguration(PluginConfiguration pPluginConfiguration) {
        Set<ServiceScope> appModes = Sets
                .newHashSet(ServiceScope.valueOf(pPluginConfiguration.getParameterValue("applicationMode")));
        Set<EntityType> entTypes = Sets
                .newHashSet(EntityType.valueOf(pPluginConfiguration.getParameterValue("entityTypes")));

        return new PluginServiceDto(pPluginConfiguration.getId(), pPluginConfiguration.getLabel(),
                pPluginConfiguration.getIconUrl(), appModes, entTypes, PluginServiceType.CATALOG);
    }

    /**
     * Build a new instance from the given {@link UIPluginConfiguration}
     * @param pUiPluginConfiguration
     * @return the new instance
     */
    public static final PluginServiceDto fromUIPluginConfiguration(UIPluginConfiguration pUiPluginConfiguration) {
        return new PluginServiceDto(pUiPluginConfiguration.getId(), pUiPluginConfiguration.getLabel(),
                pUiPluginConfiguration.getPluginDefinition().getIconUrl(),
                pUiPluginConfiguration.getPluginDefinition().getApplicationModes(),
                pUiPluginConfiguration.getPluginDefinition().getEntityTypes(), PluginServiceType.UI);
    }

    /**
     * @return the configId
     */
    public Long getConfigId() {
        return configId;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the icon
     */
    public URL getIcon() {
        return icon;
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

    /**
     * @return the type
     */
    public PluginServiceType getType() {
        return type;
    }

}
