/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.modules.plugins.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
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
@MultitenantTransactional
public class PluginService implements IPluginService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginService.class);

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
     * A {@link Map} with all the {@link Plugin} currently instantiate.</br>
     * This Map is used because for a {@link PluginConfiguration}, one and only one {@link Plugin} should be
     * instantiate.
     */
    private Map<Long, Object> instantiatePlugins = new HashMap<Long, Object>();

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

    public PluginService(final IPluginConfigurationRepository pPluginConfigurationRepository,
            List<String> pPackagesToScan) {
        super();
        pluginConfRepository = pPluginConfigurationRepository;
        if ((pPackagesToScan != null) && !pPackagesToScan.isEmpty()) {
            if (pluginPackage == null) {
                pluginPackage = new ArrayList<>();
            }
            pluginPackage.addAll(pPackagesToScan);
        }
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
                LOGGER.error("cannot instantiate the class : %s" + pValue.getPluginClassName(), e);
            }
        });

        return pluginAvailables;
    }

    @Override
    public PluginConfiguration savePluginConfiguration(final PluginConfiguration pPluginConfiguration)
            throws ModuleException {
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
            throw new ModuleException(msg.toString());
        }

        getLoadedPlugins().forEach((pKey, pValue) -> {
            if (pValue.getPluginClassName().equals(pPluginConfiguration.getPluginClassName())) {
                pPluginConfiguration.setInterfaceName(pValue.getInterfaceName());
            }
        });

        return pluginConfRepository.save(pPluginConfiguration);
    }

    @Override
    public PluginConfiguration getPluginConfiguration(final Long pId) throws ModuleException {
        if (!pluginConfRepository.exists(pId)) {
            LOGGER.error(String.format("Error while getting the plugin configuration <%s>.", pId));
            throw new EntityNotFoundException(pId, PluginConfiguration.class);
        }
        return pluginConfRepository.findOne(pId);
    }

    @Override
    public PluginConfiguration updatePluginConfiguration(final PluginConfiguration pPluginConf) throws ModuleException {
        if (!pluginConfRepository.exists(pPluginConf.getId())) {
            LOGGER.error(String.format("Error while updating the plugin configuration <%d>.", pPluginConf.getId()));
            throw new EntityNotFoundException(pPluginConf.getId().toString(), PluginConfiguration.class);
        }
        PluginConfiguration newPLuginConfiguration = savePluginConfiguration(pPluginConf);

        /**
         * Remove the PluginConfiguratin from the map
         */
        instantiatePlugins.remove(pPluginConf.getId());

        return newPLuginConfiguration;
    }

    @Override
    public void deletePluginConfiguration(final Long pConfId) throws ModuleException {
        if (!pluginConfRepository.exists(pConfId)) {
            LOGGER.error(String.format("Error while deleting the plugin configuration <%d>.", pConfId));
            throw new EntityNotFoundException(pConfId.toString(), PluginConfiguration.class);
        }
        pluginConfRepository.delete(pConfId);

        /**
         * Remove the PluginConfiguratin from the map
         */
        instantiatePlugins.remove(pConfId);
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
    public List<PluginConfiguration> getPluginConfigurations(final String pPluginId) {
        return pluginConfRepository.findByPluginIdOrderByPriorityOrderDesc(pPluginId);
    }

    @Override
    public PluginMetaData getPluginMetaDataById(final String pPluginImplId) {
        return getLoadedPlugins().get(pPluginImplId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getFirstPluginByType(final Class<?> pInterfacePluginType, final PluginParameter... pPluginParameters)
            throws ModuleException {

        // Get plugins configuration for given type
        final List<PluginConfiguration> confs = getPluginConfigurationsByType(pInterfacePluginType);

        if (confs.isEmpty()) {
            throw new ModuleException(String.format("No plugin configuration defined for the type <%s>.",
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
            if (!instantiatePlugins.containsKey(configuration.getId())
                    || (instantiatePlugins.containsKey(configuration.getId()) && (pPluginParameters.length > 0))) {

                resultPlugin = getPlugin(configuration.getId(), pPluginParameters);

                // Put in the map, only if there is no dynamic parameters
                if (pPluginParameters.length == 0) {
                    instantiatePlugins.put(configuration.getId(), resultPlugin);
                }

            } else {
                resultPlugin = (T) instantiatePlugins.get(configuration.getId());
            }
        }

        return resultPlugin;
    }

    @Override
    public <T> T getPlugin(PluginConfiguration pPluginConfiguration) throws ModuleException {
        return getPlugin(pPluginConfiguration.getId(),
                         pPluginConfiguration.getParameters().toArray(new PluginParameter[0]));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getPlugin(final Long pPluginConfigurationId, final PluginParameter... pPluginParameters)
            throws ModuleException {
        // Get the plugin associated to this configuration
        T resultPlugin;

        if (!instantiatePlugins.containsKey(pPluginConfigurationId)
                || (instantiatePlugins.containsKey(pPluginConfigurationId) && (pPluginParameters.length > 0))) {

            // Get last saved plugin configuration
            final PluginConfiguration pluginConf = getPluginConfiguration(pPluginConfigurationId);

            // Get the plugin implementation associated
            final PluginMetaData pluginMetadata = getLoadedPlugins().get(pluginConf.getPluginId());

            // Check if plugin version has changed since the last saved configuration of the plugin
            if ((pluginConf.getVersion() != null) && !pluginConf.getVersion().equals(pluginMetadata.getVersion())) {
                LOGGER.warn(String.format("Plugin version <%s> changed since last configuration <%s>.",
                                          pluginConf.getVersion(), pluginMetadata.getVersion()));
            }

            try {
                resultPlugin = PluginUtils.getPlugin(pluginConf, pluginMetadata, getPluginPackage(), pPluginParameters);
            } catch (PluginUtilsException e) {
                throw new ModuleException(e);
            }

            // Put in the map, only if there is no dynamic parameters
            if (pPluginParameters.length == 0) {
                instantiatePlugins.put(pPluginConfigurationId, resultPlugin);
            }

        } else {
            resultPlugin = (T) instantiatePlugins.get(pPluginConfigurationId);
        }

        return resultPlugin;
    }

    @Override
    public List<PluginConfiguration> getAllPluginConfigurations() {
        Iterable<PluginConfiguration> confs = pluginConfRepository.findAll();
        if (confs == null) {
            Collections.emptyList();
        }
        return Lists.newArrayList(confs);
    }

    private List<String> getPluginPackage() {
        if (pluginPackage == null) {
            pluginPackage = new ArrayList<>();
        }
        return pluginPackage;
    }

    @Override
    public void addPluginPackage(final String pPluginPackage) {
        getPluginPackage().add(pPluginPackage);
    }

    @Override
    public PluginMetaData checkPluginClassName(Class<?> pClass, String pPluginClassName) throws EntityInvalidException {
        PluginMetaData metaData = null;
        boolean isFound = false;

        // Search all the plugins of type pClass
        for (PluginMetaData pMd : this.getPluginsByType(pClass)) {
            if (!isFound && pMd.getPluginClassName().equals(pPluginClassName)) {
                isFound = true;
                metaData = pMd;
            }
        }

        if (!isFound) {
            throw new EntityInvalidException("Any plugin's type match the plugin class name : " + pPluginClassName);
        }

        return metaData;
    }

}
