/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.plugins.annotations.Plugin;
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
 * @author cmertz
 */
@Service
public class PluginService implements IPluginService {

    /**
     * Package scanned to find plugins.
     */
    public static final String PLUGINS_PACKAGE = "fr.cnes.regards.plugins";

    /**
     * String to start log of plugin identifier
     */
    private static final String START_ID_LOG = " <";

    /**
     * String to end log of plugin identifier
     */
    private static final String END_ID_LOG = "> ";

    /**
     * Dot to end log message
     */
    private static final String DOT_LOG = ".";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginService.class);

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
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    public PluginService(IPluginConfigurationRepository pPluginConfigurationRepository) throws PluginUtilsException {
        super();
        pluginConfRepository = pPluginConfigurationRepository;
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
        // Scan class path for plugins implementations only once
        if (plugins == null) {
            plugins = PluginUtils.getPlugins(PLUGINS_PACKAGE);
        }
    }

    @Override
    public List<String> getPluginTypes() {
        return PluginInterfaceUtils.getInterfaces(PLUGINS_PACKAGE);
    }

    @Override
    public List<PluginMetaData> getPlugins() {
        return getPluginsByType(null);
    }

    @Override
    public List<PluginMetaData> getPluginsByType(final Class<?> pInterfacePluginType) {
        final List<PluginMetaData> pluginAvailables = new ArrayList<>();

        plugins.forEach((pKey, pValue) -> {
            if (pInterfacePluginType == null || (pInterfacePluginType != null
                    && pInterfacePluginType.isAssignableFrom(pValue.getPluginClass()))) {
                pluginAvailables.add(pValue);
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
            message += START_ID_LOG + pPluginConfiguration.getPluginId() + END_ID_LOG + "without priority order.";
            throwError = true;
        }
        if (!throwError && pPluginConfiguration.getVersion() == null) {
            message += START_ID_LOG + pPluginConfiguration.getPluginId() + END_ID_LOG + "without version.";
            throwError = true;
        }

        if (throwError) {
            LOGGER.error(message);
            throw new PluginUtilsException(message);
        }

        return pluginConfRepository.save(pPluginConfiguration);
    }

    @Override
    public PluginConfiguration getPluginConfiguration(final Long pId) throws PluginUtilsException {
        // Get plugin configuration
        final PluginConfiguration conf = pluginConfRepository.findOne(pId);

        if (conf == null) {
            final String message = "Error getting plugin configuration " + START_ID_LOG + pId + END_ID_LOG + DOT_LOG;
            LOGGER.error(message);
            throw new PluginUtilsException(message);
        }
        return conf;
    }

    @Override
    public PluginConfiguration updatePluginConfiguration(final PluginConfiguration pPlugin)
            throws PluginUtilsException {
        // Check if plugin configuration exists
        getPluginConfiguration(pPlugin.getId());

        return savePluginConfiguration(pPlugin);
    }

    @Override
    public void deletePluginConfiguration(final Long pPluginId) throws PluginUtilsException {
        try {
            pluginConfRepository.delete(pPluginId);
        } catch (final EmptyResultDataAccessException e) {
            throw new PluginUtilsException(
                    "Error deleting plugin configuration " + START_ID_LOG + pPluginId + END_ID_LOG + DOT_LOG, e);
        }
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
        return plugins.get(pPluginImplId);
    }

    @Override
    public <T> T getFirstPluginByType(final Class<?> pInterfacePluginType, final PluginParameter... pPluginParameters)
            throws PluginUtilsException {

        // Get plugins configuration for given type
        final List<PluginConfiguration> confs = getPluginConfigurationsByType(pInterfacePluginType);

        if (confs.isEmpty()) {
            final String message = "No plugin configuration defined for the type " + START_ID_LOG
                    + pInterfacePluginType.getName() + END_ID_LOG + DOT_LOG;
            LOGGER.error(message);
            throw new PluginUtilsException(message);
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
        final PluginMetaData pluginMetadata = plugins.get(pluginConf.getPluginId());

        // Check if plugin version has changed since the last saved configuration of the plugin
        if ((pluginConf.getVersion() != null) && !pluginConf.getVersion().equals(pluginMetadata.getVersion())) {
            LOGGER.warn("Warning plugin version " + START_ID_LOG + pluginConf.getVersion() + END_ID_LOG
                    + "changed since last configuration" + START_ID_LOG + pluginMetadata.getVersion() + END_ID_LOG);
        }

        return PluginUtils.getPlugin(pluginConf, pluginMetadata, pPluginParameters);
    }

}
