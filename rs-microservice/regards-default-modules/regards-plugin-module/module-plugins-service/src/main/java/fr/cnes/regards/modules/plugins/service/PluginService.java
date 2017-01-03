/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.plugins.autoconfigure.PluginUtilsProperties;
import fr.cnes.regards.modules.plugins.annotations.PluginInterface;
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
 * @author Sébastien Binda
 */
@Service
public class PluginService implements IPluginService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginService.class);

    /**
     * The properties of the plugins : the package to scan to find {@link PluginInterface}.
     */
    @Autowired
    private PluginUtilsProperties properties;

    /**
     * The plugin's package to scan
     */
    private List<String> pluginPackage;

    /**
     * {@link PluginConfiguration} JPA Repository
     */
    private final IPluginConfigurationRepository pluginConfRepository;

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
    public PluginService(final IPluginConfigurationRepository pPluginConfigurationRepository) {
        super();
        pluginConfRepository = pPluginConfigurationRepository;
    }

    private Map<String, PluginMetaData> getLoadedPlugins() {
        if (plugins == null) {
            plugins = PluginUtils.getPlugins(getPluginPackage());
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
                if ((pInterfacePluginType == null) || ((pInterfacePluginType != null)
                        && pInterfacePluginType.isAssignableFrom(Class.forName(pValue.getPluginClassName())))) {
                    pluginAvailables.add(pValue);
                }
            } catch (final ClassNotFoundException e) {
                LOGGER.error("cannot instanciate the class : %s" + pValue.getPluginClassName(), e);
            }
        });

        return pluginAvailables;
    }

    @Override
    public PluginConfiguration savePluginConfiguration(final PluginConfiguration pPluginConfiguration)
            throws PluginUtilsException {
        // Check plugin configuration validity
        final StringBuilder msg = new StringBuilder("Impossible to save a plugin configuration");

        boolean throwError = false;

        if (pPluginConfiguration == null) {
            msg.append(". The plugin configuration cannot be null.");
            throwError = true;
        }
        if (!throwError && (pPluginConfiguration.getPluginId() == null)) {
            msg.append(". The unique identifier of the plugin (attribute pluginId) is required.");
            throwError = true;
        }
        if (!throwError && (pPluginConfiguration.getPriorityOrder() == null)) {
            msg.append(String.format(" <%s> without priority order.", pPluginConfiguration.getPluginId()));
            throwError = true;
        }
        if (!throwError && (pPluginConfiguration.getVersion() == null)) {
            msg.append(String.format(" <%s> without version.", pPluginConfiguration.getPluginId()));
            throwError = true;
        }

        if (throwError) {
            throw new PluginUtilsException(msg.toString());
        }

        return pluginConfRepository.save(pPluginConfiguration);
    }

    @Override
    public PluginConfiguration getPluginConfiguration(final Long pId) throws PluginUtilsException {
        // Get plugin configuration
        PluginConfiguration conf;
        try {
            conf = pluginConfRepository.findOne(pId);
        } catch (final NoSuchElementException e) {
            throw new PluginUtilsException(String.format("Error while getting the plugin configuration <%s>.", pId), e);
        }

        return conf;
    }

    @Override
    public PluginConfiguration updatePluginConfiguration(final PluginConfiguration pPlugin)
            throws EntityNotFoundException, PluginUtilsException {
        // Check if plugin configuration exists
        if (!pluginConfRepository.exists(pPlugin.getId())) {
            throw new EntityNotFoundException(pPlugin.getId().toString(), PluginConfiguration.class);
        }
        return savePluginConfiguration(pPlugin);
    }

    @Override
    public void deletePluginConfiguration(final Long pConfId) throws EntityNotFoundException {
        if (!pluginConfRepository.exists(pConfId)) {
            LOGGER.error(String.format("Error while deleting the plugin configuration <%s>.", pConfId));
            throw new EntityNotFoundException(pConfId.toString(), PluginConfiguration.class);
        }
        pluginConfRepository.delete(pConfId);
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(final Class<?> pInterfacePluginType) {
        final List<PluginConfiguration> configurations = new ArrayList<>();

        final List<PluginMetaData> pluginImpls = getPluginsByType(pInterfacePluginType);

        pluginImpls.forEach(pMetaData -> configurations
                .addAll(pluginConfRepository.findByPluginIdOrderByPriorityOrderDesc(pMetaData.getPluginId())));

        return configurations;
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(final String pPluginId) {
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

        return PluginUtils.getPlugin(pluginConf, pluginMetadata, getPluginPackage(), pPluginParameters);
    }

    private List<String> getPluginPackage() {
        if (pluginPackage == null) {
            pluginPackage = new ArrayList<>();
            if (properties != null && properties.getPackagesToScan()!=null) {
                pluginPackage.addAll(properties.getPackagesToScan());
            }
        }
        return pluginPackage;
    }

    @Override
    public void addPluginPackage(final String pPluginPackage) {
        getPluginPackage().add(pPluginPackage);
    }

}
