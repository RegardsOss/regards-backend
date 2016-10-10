/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.domain.IPluginType;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.plugins.utils.AbstractPluginUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * TODO description
 *
 * @author cmertz
 */
@Service
public class PluginService implements IPluginService {

    /**
     * Package scanned to find plugins.
     */
    public static final String PLUGINS_PACKAGE = "fr.cnes.regards.plugins";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginService.class);

    /**
     * Plugin Configuration JPA Repository
     */
    @Autowired
    private IPluginConfigurationRepository pluginConfRepository;

    /**
     * Plugins implementation list sorted by plugin id. Plugin id, is the id of the "@PluginMetaData" annotation of the
     * implementation class.
     */
    private Map<String, PluginMetaData> plugins;

    public PluginService() throws PluginUtilsException {
        super();
        this.loadPlugins();
    }

    /**
     *
     * Load from the classpath the plugins implementations of plugin annotation : {@link Plugin}
     *
     * @param parameters
     *
     * @throws PluginUtilsException
     *             Error when loading the plugins
     */
    private void loadPlugins() throws PluginUtilsException {

        // Scan class spath for plugins implementations only once
        if (plugins == null) {
            plugins = AbstractPluginUtils.getPlugins(PLUGINS_PACKAGE);
        }
    }

    @Override
    public List<String> getPluginTypes() {
        // final List<String> pluginTypes = new ArrayList<>();
        // for (final PluginType type : PluginType.values()) {
        // pluginTypes.add(type.getName());
        // }
        // TODO récupérer les interfaces avec l'annotation PluginInterface
        return null;
    }

    @Override
    public List<PluginMetaData> getPluginsByType(IPluginType pType) {

        final List<PluginMetaData> pluginAvailables = new ArrayList<>();
        // Check fo each plugin implementation, ones which implements pClassType
        for (final String pluginId : plugins.keySet()) {
            final PluginMetaData plugin = plugins.get(pluginId);
            if (pType != null && pType.getClassType().isAssignableFrom(plugin.getPluginClass())) {
                pluginAvailables.add(plugin);
            } else
                if (pType == null) {
                    // Return all plugins
                    pluginAvailables.add(plugin);
                }
        }
        return pluginAvailables;
    }

    @Override
    public <T> T getPlugin(Long pPluginConfigurationId, Class<T> pReturnInterfaceType) throws PluginUtilsException {

        // Get last saved plugin configuration
        final PluginConfiguration pluginConf = getPluginConfiguration(pPluginConfigurationId);

        // Get the plugin implementation associated
        final PluginMetaData pluginMetadata = plugins.get(pluginConf.getPluginId());

        // Check if plugin version has changed since the last saved configuration of the plugin
        if (pluginConf.getVersion() != null && !pluginConf.getVersion().equals(pluginMetadata.getVersion())) {
            LOGGER.warn("Warning plugin version changed since last configuration");
        }

        return AbstractPluginUtils.getPlugin(pluginConf, pluginMetadata);
    }

    @Override
    public PluginConfiguration savePluginConfiguration(PluginConfiguration pPluginConfiguration)
            throws PluginUtilsException {

        // Check plugin configuration validity
        if (pPluginConfiguration == null) {
            final String message = "Impossible to save null plugin configuration.";
            LOGGER.error(message);
            throw new PluginUtilsException(message);
        }
        if (plugins.get(pPluginConfiguration.getPluginId()) == null) {
            final String message = "Impossible to save plugin configuration. Plugin implementation with id="
                    + pPluginConfiguration.getPluginId() + " does not exists";
            LOGGER.error(message);
            throw new PluginUtilsException(message);
        }
        if (pPluginConfiguration.getPriorityOrder() == null) {
            final String message = "Impossible to save plugin configuration without priority order.";
            LOGGER.error(message);
            throw new PluginUtilsException(message);
        }

        if (pPluginConfiguration.getVersion() == null) {
            final String message = "Impossible to save plugin configuration without version.";
            LOGGER.error(message);
            throw new PluginUtilsException(message);
        }

        final PluginConfiguration pluginSaved = pluginConfRepository.save(pPluginConfiguration);

        return pluginSaved;
    }

    /**
     *
     * Return the plugin configuration for the given id
     *
     * @param pPluginConfigurationId
     * @param pType
     * @return
     * @throws RegardsServiceException
     * @since 1.0
     */
    @Override
    public PluginConfiguration getPluginConfiguration(Long pId) throws PluginUtilsException {
        // Get plugin configuration from dataBase
        final PluginConfiguration conf = pluginConfRepository.findOne(pId);
        if (conf == null) {
            final String message = "Error getting plugin configuration id=" + pId;
            LOGGER.error(message);
            throw new PluginUtilsException(message);
        }
        return conf;
    }

    /**
     * Overridden method
     *
     * @throws RegardsServiceException
     *
     * @see fr.cs.regards.services.pluginmanagement.IPluginManagerService#deletePluginConfiguration(fr.cs.regards.model.request.RequestInfo,
     *      java.lang.String)
     * @since 1.0
     */
    @Override
    public void deletePluginConfiguration(Long pPluginId) throws PluginUtilsException {
        try {
            pluginConfRepository.delete(pPluginId);
        } catch (final EmptyResultDataAccessException e) {
            throw new PluginUtilsException("Error deleting plugin configuration with id=" + pPluginId + ".", e);
        }
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(IPluginType pType) {

        final List<PluginMetaData> pluginImpls = this.getPluginsByType(pType);

        final List<PluginConfiguration> configurations = new ArrayList<>();

        for (final PluginMetaData pluginImpl : pluginImpls) {
            configurations.addAll(pluginConfRepository
                    .findByPluginIdAndTenantOrderByPriorityOrderDesc(pluginImpl.getPluginId()));
        }

        return configurations;
    }

    @Override
    public PluginMetaData getPluginMetaDataById(String pPluginImplId) {
        return plugins.get(pPluginImplId);
    }

    /**
     * Update a plugin configuration
     *
     * @param pRequestInfo
     * @param pPlugin
     * @return
     * @throws RegardsServiceException
     * @since 1.0
     */
    @Override
    public PluginConfiguration updatePluginConfiguration(PluginConfiguration pPlugin) throws PluginUtilsException {
        // Check if plugin configuration exists
        PluginConfiguration conf = getPluginConfiguration(pPlugin.getId());
        if (conf == null) {
            final String message = "Error updating plugin configuration with id=" + pPlugin.getId()
                    + ". Plugin configuration does not exists";
            LOGGER.error(message);
            throw new PluginUtilsException(message);
        } else {
            conf = savePluginConfiguration(pPlugin);
        }
        return conf;
    }

    @Override
    public <T> T getFirstPluginByType(IPluginType pType, Class<T> pReturnInterfaceType) throws PluginUtilsException {

        // Get plugins configuration for given type
        final List<PluginConfiguration> confs = this.getPluginConfigurationsByType(pType);

        if (confs == null || confs.isEmpty()) {
            final String message = "No plugin configuration defined for the type : " + pType.getName();
            LOGGER.error(message);
            throw new PluginUtilsException(message);
        }

        PluginConfiguration configuration = null;

        for (final PluginConfiguration conf : confs) {
            if (configuration == null || conf.getPriorityOrder() < configuration.getPriorityOrder()) {
                configuration = conf;
            }
        }
        return getPlugin(configuration.getId(), pReturnInterfaceType);

    }

    @Override
    public List<PluginMetaData> getPlugins() {
        // TODO Auto-generated method stub
        return null;
    }

}
