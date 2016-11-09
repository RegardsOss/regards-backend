/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.plugins.utils.PluginInterfaceUtils;
import fr.cnes.regards.plugins.utils.PluginUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * The implementation of {@link IPluginService}.
 *
 * @author Christophe Mertz
 */
@Service
public class PluginService implements IPluginService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginService.class);

    /**
     * The plugin's package to scan
     */
    private List<String> pluginPackage = Arrays.asList( "fr.cnes.regards.plugins", "fr.cnes.regards.contrib" );

    /**
     * {@link PluginConfiguration} JPA Repository
     */
    private IPluginConfigurationRepository pluginConfRepository;

    /**
     * Plugins implementation list sorted by plugin id. Plugin id, is the id of the "@PluginMetaData" annotation of the
     * implementation class.
     */
    private Map<String, PluginMetaData> plugins;

    /**
     * A constructor with the {@link IPluginConfigurationRepository}.
     * 
     * @param pPluginConfigurationRepository
     *            {@link PluginConfiguration} JPA repository
     */
    public PluginService(IPluginConfigurationRepository pPluginConfigurationRepository) {
        super();
        pluginConfRepository = pPluginConfigurationRepository;
    }

    private Map<String, PluginMetaData> getLoadedPlugins() {
        if (plugins == null) {
            try {
                plugins = PluginUtils.getPlugins(getPluginPackage());
            } catch (PluginUtilsException e) {
                LOGGER.warn(e.getMessage());
            }
        }
        return plugins;
    }

    @Override
    public List<String> getPluginTypes() {
        return PluginInterfaceUtils.getInterfaces(getPluginPackage());
    }

    @Override
    public List<PluginMetaData> getPlugins() {
        return getPluginsByType(null);
    }

    @Override
    public List<PluginMetaData> getPluginsByType(final Class<?> pInterfacePluginType) {
        final List<PluginMetaData> pluginAvailables = new ArrayList<>();

        getLoadedPlugins().forEach((pKey, pValue) -> {
            try {
                if (pInterfacePluginType == null || (pInterfacePluginType != null
                        && pInterfacePluginType.isAssignableFrom(Class.forName(pValue.getPluginClassName())))) {
                    pluginAvailables.add(pValue);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error("cannot instanciate the class : %s" + pValue.getPluginClassName());
            }
        });

        return pluginAvailables;
    }

    @Override
    public PluginConfiguration savePluginConfiguration(final PluginConfiguration pPluginConfiguration)
            throws PluginUtilsException {
        // Check plugin configuration validity
        String message = "Impossible to save a plugin configuration";

        boolean throwError = false;

        if (pPluginConfiguration == null) {
            message += ". The plugin configuration cannot be null.";
            throwError = true;
        }
        if (!throwError && pPluginConfiguration.getPluginId() == null) {
            message += ". The unique identifier of the plugin (attribute pluginId) is required.";
            throwError = true;
        }
        if (!throwError && pPluginConfiguration.getPriorityOrder() == null) {
            message += String.format(" <%s> without priority order.", pPluginConfiguration.getPluginId());
            throwError = true;
        }
        if (!throwError && pPluginConfiguration.getVersion() == null) {
            message = String.format(" <%s> without version.", pPluginConfiguration.getPluginId());
            throwError = true;
        }

        if (throwError) {
            throw new PluginUtilsException(message);
        }

        return pluginConfRepository.save(pPluginConfiguration);
    }

    @Override
    public PluginConfiguration getPluginConfiguration(final Long pId) throws PluginUtilsException {
        // Get plugin configuration
        final PluginConfiguration conf = pluginConfRepository.findOne(pId);

        if (conf == null) {
            throw new PluginUtilsException(String.format("Error while getting the plugin configuration <%s>.", pId));
        }
        return conf;
    }

    @Override
    public PluginConfiguration updatePluginConfiguration(final PluginConfiguration pPlugin)
            throws PluginUtilsException {
        // Check if plugin configuration exists
        if (!pluginConfRepository.exists(pPlugin.getId())) {
            throw new PluginUtilsException(
                    String.format("Error while updating the plugin configuration <%s>.", pPlugin.getId()));
        }

        return savePluginConfiguration(pPlugin);
    }

    @Override
    public void deletePluginConfiguration(final Long pPluginId) throws PluginUtilsException {
        if (!pluginConfRepository.exists(pPluginId)) {
            LOGGER.error(String.format("Error while deleting the plugin configuration <%s>.", pPluginId));
            throw new PluginUtilsException(pPluginId.toString());
        }
        pluginConfRepository.delete(pPluginId);
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(final Class<?> pInterfacePluginType) {
        final List<PluginConfiguration> configurations = new ArrayList<>();

        final List<PluginMetaData> pluginImpls = getPluginsByType(pInterfacePluginType);

        pluginImpls.forEach(p -> configurations
                .addAll(pluginConfRepository.findByPluginIdOrderByPriorityOrderDesc(p.getPluginId())));

        return configurations;
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(String pPluginId) {
        return pluginConfRepository.findByPluginIdOrderByPriorityOrderDesc(pPluginId);
    }

    @Override
    public PluginMetaData getPluginMetaDataById(final String pPluginImplId) {
        return getLoadedPlugins().get(pPluginImplId);
    }

    @Override
    public <T> T getFirstPluginByType(final Class<?> pInterfacePluginType, final PluginParameter... pPluginParameters)
            throws PluginUtilsException {

        // Get plugins configuration for given type
        final List<PluginConfiguration> confs = getPluginConfigurationsByType(pInterfacePluginType);

        if (confs.isEmpty()) {
            throw new PluginUtilsException(String.format("No plugin configuration defined for the type <%s>.",
                                                         pInterfacePluginType.getName()));
        }

        // Search the configuration the most priority
        PluginConfiguration configuration = null;

        for (final PluginConfiguration conf : confs) {
            if ((configuration == null) || (conf.getPriorityOrder() < configuration.getPriorityOrder())) {
                configuration = conf;
            }
        }

        // Get the plugin associated to this configuration
        T resultPlugin = null;

        if (configuration != null) {
            resultPlugin = getPlugin(configuration.getId(), pPluginParameters);
        }

        return resultPlugin;
    }

    @Override
    public <T> T getPlugin(final Long pPluginConfigurationId, final PluginParameter... pPluginParameters)
            throws PluginUtilsException {

        // Get last saved plugin configuration
        final PluginConfiguration pluginConf = getPluginConfiguration(pPluginConfigurationId);

        // Get the plugin implementation associated
        final PluginMetaData pluginMetadata = getLoadedPlugins().get(pluginConf.getPluginId());

        // Check if plugin version has changed since the last saved configuration of the plugin
        if ((pluginConf.getVersion() != null) && !pluginConf.getVersion().equals(pluginMetadata.getVersion())) {
            LOGGER.warn(String.format("Plugin version <%s> changed since last configuration <%s>.",
                                      pluginConf.getVersion(), pluginMetadata.getVersion()));
        }

        return PluginUtils.getPlugin(pluginConf, pluginMetadata, pPluginParameters);
    }

    public List<String> getPluginPackage() {
        return pluginPackage;
    }

    public void addPluginPackage(String pPluginPackage) {
        this.pluginPackage.add(pPluginPackage);
    }

}
