/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnes.regards.framework.modules.plugins.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.Assert;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameterType;
import fr.cnes.regards.framework.modules.plugins.domain.event.BroadcastPluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginServiceAction;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginInterfaceUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;

/**
 * The implementation of {@link IPluginService}.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 *
 *
 *         TODO V3 : with hot plugin loading, be careful to properly clean the plugin cache when plugin version change
 */
@MultitenantTransactional
@Service
public class PluginService implements IPluginService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginService.class);

    @Value("${regards.plugins.packages-to-scan:#{null}}")
    private String[] packagesToScan;

    /**
     * {@link IRuntimeTenantResolver}
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * The plugin's package to scan
     */
    private List<String> pluginPackage;

    /**
     * {@link PluginConfiguration} JPA Repository
     */
    private final IPluginConfigurationRepository pluginConfRepository;

    /**
     * Plugins implementation metadata list sorted by plugin id. Plugin id, is the id of the "@PluginMetaData"
     * annotation of the
     * implementation class.
     * <b>Note: </b> PluginService is used in multi-thread environment (see IngesterService and CrawlerService) so
     * ConcurrentHashMap is used instead of HashMap
     */
    private ConcurrentMap<String, PluginMetaData> plugins;

    /**
     * A {@link Map} with all the {@link Plugin} currently instantiate by tenant.</br>
     * This Map is used because for a {@link PluginConfiguration}, one and only one {@link Plugin} should be
     * instantiate.
     * <b>Note: </b> PluginService is used in multi-thread environment (see IngesterService and CrawlerService) so
     * ConcurrentHashMap is used instead of HashMap
     */
    private final Map<String, ConcurrentMap<Long, Object>> instantiatePlugins = new HashMap<>();

    private final IPublisher publisher;

    public PluginService(final IPluginConfigurationRepository pPluginConfigurationRepository,
            final IPublisher publisher, IRuntimeTenantResolver runtimeTenantResolver) {
        this.pluginConfRepository = pPluginConfigurationRepository;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    private Map<String, PluginMetaData> getLoadedPlugins() {
        if (plugins == null) {
            plugins = PluginUtils.getPlugins(getPluginPackage());
        }
        logPluginServiceState("getLoadedPlugins");
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
            throws EntityInvalidException {
        // Check plugin configuration validity
        final StringBuilder msg = new StringBuilder("Cannot save plugin configuration");

        if (pPluginConfiguration == null) {
            msg.append(". The plugin configuration cannot be null.");
            throw new EntityInvalidException(msg.toString());
        }
        if (pPluginConfiguration.getPluginId() == null) {
            msg.append(". The unique identifier of the plugin (attribute pluginId) is required.");
            throw new EntityInvalidException(msg.toString());
        }
        if (pPluginConfiguration.getPriorityOrder() == null) {
            msg.append(String.format(" <%s> without priority order.", pPluginConfiguration.getPluginId()));
            throw new EntityInvalidException(msg.toString());
        }
        if (pPluginConfiguration.getVersion() == null) {
            msg.append(String.format(" <%s> without version.", pPluginConfiguration.getPluginId()));
            throw new EntityInvalidException(msg.toString());
        }

        if (Strings.isNullOrEmpty(pPluginConfiguration.getLabel())) {
            msg.append(String.format(" <%s> without label.", pPluginConfiguration.getPluginId()));
            throw new EntityInvalidException(msg.toString());
        }

        PluginConfiguration pluginConfInDb = pluginConfRepository.findOneByLabel(pPluginConfiguration.getLabel());
        if ((pluginConfInDb != null) && !Objects.equals(pluginConfInDb.getId(), pPluginConfiguration.getId())
                && pluginConfInDb.getLabel().equals(pPluginConfiguration.getLabel())) {
            msg.append(String.format(". A plugin configuration with same label (%s) already exists.",
                                     pPluginConfiguration.getLabel()));
            throw new EntityInvalidException(msg.toString());
        }

        getLoadedPlugins().forEach((pKey, pValue) -> {
            if (pValue.getPluginClassName().equals(pPluginConfiguration.getPluginClassName())) {
                pPluginConfiguration.setInterfaceNames(pValue.getInterfaceNames());
            }
        });

        final boolean shouldPublishCreation = pPluginConfiguration.getId() == null;

        final PluginConfiguration newConf = pluginConfRepository.save(pPluginConfiguration);
        if (shouldPublishCreation) {
            publisher.publish(new BroadcastPluginConfEvent(newConf.getId(), PluginServiceAction.CREATE,
                    newConf.getInterfaceNames()));
            publisher.publish(new PluginConfEvent(newConf.getId(), PluginServiceAction.CREATE,
                    newConf.getInterfaceNames()));

        }

        return newConf;
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
    public PluginConfiguration loadPluginConfiguration(final Long id) {
        return pluginConfRepository.findById(id);
    }

    @Override
    public boolean exists(final Long pId) {
        return pluginConfRepository.exists(pId);
    }

    @Override
    public boolean existsByLabel(String pluginConfLabel) {
        return pluginConfRepository.findOneByLabel(pluginConfLabel) != null;
    }

    @Override
    public PluginConfiguration updatePluginConfiguration(final PluginConfiguration pPluginConf) throws ModuleException {
        final PluginConfiguration oldConf = pluginConfRepository.findById(pPluginConf.getId());
        if (oldConf == null) {
            LOGGER.error(String.format("Error while updating the plugin configuration <%d>.", pPluginConf.getId()));
            throw new EntityNotFoundException(pPluginConf.getId().toString(), PluginConfiguration.class);
        }
        final boolean oldConfActive = oldConf.isActive();
        final PluginConfiguration newPluginConfiguration = savePluginConfiguration(pPluginConf);

        if (oldConfActive != newPluginConfiguration.isActive()) {
            if (newPluginConfiguration.isActive()) {
                publisher.publish(new BroadcastPluginConfEvent(pPluginConf.getId(), PluginServiceAction.ACTIVATE,
                        newPluginConfiguration.getInterfaceNames()));
                publisher.publish(new PluginConfEvent(pPluginConf.getId(), PluginServiceAction.ACTIVATE,
                        newPluginConfiguration.getInterfaceNames()));
            } else {
                publisher.publish(new BroadcastPluginConfEvent(pPluginConf.getId(), PluginServiceAction.DISABLE,
                        newPluginConfiguration.getInterfaceNames()));
            }
        }
        /**
         * Remove the PluginConfiguratin from the map
         */
        cleanPluginCache(pPluginConf.getId());

        return newPluginConfiguration;
    }

    @Override
    public void deletePluginConfiguration(final Long pConfId) throws ModuleException {
        final PluginConfiguration toDelete = pluginConfRepository.findOne(pConfId);
        if (toDelete == null) {
            LOGGER.error(String.format("Error while deleting the plugin configuration <%d>.", pConfId));
            throw new EntityNotFoundException(pConfId.toString(), PluginConfiguration.class);
        }
        publisher.publish(new BroadcastPluginConfEvent(pConfId, PluginServiceAction.DELETE,
                toDelete.getInterfaceNames()));
        pluginConfRepository.delete(pConfId);

        /**
         * Remove the PluginConfiguratin from the map
         */
        PluginUtils.doDestroyPlugin(toDelete);
        cleanPluginCache(pConfId);
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(final Class<?> pInterfacePluginType) {

        final Iterable<PluginConfiguration> plgConfs = pluginConfRepository.findAll();
        if (plgConfs != null) {
            return Lists.newArrayList(plgConfs).stream()
                    .filter(pc -> pc.getInterfaceNames().contains(pInterfacePluginType.getName()))
                    .collect(Collectors.toList());
        } else {
            return Lists.newArrayList();
        }
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurations(final String pPluginId) {
        return pluginConfRepository.findByPluginIdOrderByPriorityOrderDesc(pPluginId);
    }

    @Override
    public PluginMetaData getPluginMetaDataById(final String pPluginImplId) {
        return getLoadedPlugins().get(pPluginImplId);
    }

    @Override
    public <T> T getFirstPluginByType(final Class<?> pInterfacePluginType, final PluginParameter... pPluginParameters)
            throws ModuleException {

        // Get plugins configuration for given type
        final List<PluginConfiguration> confs = getPluginConfigurationsByType(pInterfacePluginType);

        if (confs.isEmpty()) {
            throw new ModuleException(String.format("No plugin configuration defined for the type <%s>.",
                                                    pInterfacePluginType.getName()));
        }

        // Search configuration with upper priority
        PluginConfiguration configuration = null;

        for (final PluginConfiguration conf : confs) {
            if ((configuration == null) || (conf.getPriorityOrder() < configuration.getPriorityOrder())) {
                configuration = conf;
            }
        }

        // Get the plugin associated to this configuration
        return (configuration != null) ? getPlugin(configuration.getId(), pPluginParameters) : null;
    }

    @Override
    public <T> T getPlugin(final PluginConfiguration pPluginConfiguration,
            final PluginParameter... dynamicPluginParameters) throws ModuleException {
        return getPlugin(pPluginConfiguration.getId(), dynamicPluginParameters);
    }

    /**
     * We consider only plugin without dynamic parameters so we can profit from the cache system.
     *
     * @return whether a plugin conf, without dynamic parameters is instanciable or not
     * @throws ModuleException when no plugin configuration with this id exists
     */
    @Override
    public boolean canInstantiate(final Long pluginConfigurationId) throws ModuleException {
        try {
            getPlugin(pluginConfigurationId);
            return true;
        } catch (PluginUtilsRuntimeException e) {
            LOGGER.warn(String.format("Plugin with configuration %s couldn't be instanciated", pluginConfigurationId),
                        e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getPlugin(final Long pPluginConfigurationId, final PluginParameter... dynamicPluginParameters)
            throws ModuleException {

        if (!isPluginCached(pPluginConfigurationId) || (dynamicPluginParameters.length > 0)) {
            return instanciatePluginAndCache(pPluginConfigurationId, dynamicPluginParameters);
        }
        return (T) getCachedPlugin(pPluginConfigurationId);
    }

    /**
     * Instanciate a plugin and cache it <b>if it doesn't have dynamic parameters</b>
     * @param pPluginConfigurationId plugin configuration identifier
     * @param dynamicPluginParameters plugin parameters (including potential dynamic ones)
     * @return plugin instance
     * @throws ModuleException if error occurs!
     */
    private <T> T instanciatePluginAndCache(final Long pPluginConfigurationId,
            final PluginParameter... dynamicPluginParameters) throws ModuleException {

        // Check if all parameters are really dynamic
        for (PluginParameter dynamicParameter : dynamicPluginParameters) {
            if (!dynamicParameter.isDynamic()) {
                String errorMessage = String.format(
                                                    "The parameter \"%s\" is not identified as dynamic. Plugin instanciation is cancelled.",
                                                    dynamicParameter.getName());
                LOGGER.error(errorMessage);
                throw new UnexpectedDynamicParameter(errorMessage);
            }
        }

        // Get last saved plugin configuration
        final PluginConfiguration pluginConf = loadPluginConfiguration(pPluginConfigurationId);

        // Get the plugin implementation associated
        final PluginMetaData pluginMetadata = getLoadedPlugins().get(pluginConf.getPluginId());

        if (pluginMetadata == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No plugin metadata found for plugin configuration id {}", pluginConf.getPluginId());
                logPluginServiceState("instanciatePluginAndCache");
            }
            throw new PluginMetadataNotFoundRuntimeException(
                    "Metadata not found for plugin configuration identifier " + pluginConf.getPluginId());
        }

        // When plugins are loaded from database, maybe dependant plugins aren't yet loaded
        // So :
        // For all pluginMetada parameters, find PLUGIN ones, get key
        for (PluginParameterType paramType : pluginMetadata.getParameters()) {
            if (paramType.getParamType() == PluginParameterType.ParamType.PLUGIN) {
                String paramName = paramType.getName();
                // Now search from PluginConfiguration parameters associated Plugin
                for (PluginParameter param : pluginConf.getParameters()) {
                    if (param.getName().equals(paramName) && (param.getPluginConfiguration() != null)) {
                        // LOAD embedded plugin
                        this.getPlugin(param.getPluginConfiguration());
                        break;
                    }
                }
            }
        }

        T resultPlugin = PluginUtils.getPlugin(pluginConf, pluginMetadata, getPluginPackage(), getPluginCache(),
                                               dynamicPluginParameters);

        // Put in the map, only if there is no dynamic parameters
        if (dynamicPluginParameters.length == 0) {
            addPluginToCache(pPluginConfigurationId, resultPlugin);
        }

        return resultPlugin;
    }

    /**
     * Allows to add logs on this class state.
     * @param methodInvoquing method that invoques logPluginServiceState
     */
    private void logPluginServiceState(String methodInvoquing) {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("logPluginServiceState invoqued by : {}", methodInvoquing);
            LOGGER.debug("This identifier: {}", this.toString());
            StringJoiner sj = new StringJoiner(" ,");
            pluginPackage.forEach(p -> sj.add(p));
            LOGGER.debug("List of plugin packages: {}", sj.toString());
            for (Entry<String, PluginMetaData> entry : plugins.entrySet()) {

                // Interfaces
                Iterator<String> interfaceIt = entry.getValue().getInterfaceNames().iterator();
                StringBuilder interfaceNamesBuilder = new StringBuilder();
                interfaceNamesBuilder.append("[");
                if (interfaceIt.hasNext()) {
                    interfaceNamesBuilder.append(interfaceIt.next());
                    while (interfaceIt.hasNext()) {
                        interfaceNamesBuilder.append(",");
                        interfaceNamesBuilder.append(interfaceIt.next());
                    }
                }
                interfaceNamesBuilder.append("]");

                LOGGER.debug("Available plugins metadata : {} -> {} / {} / {}", entry.getKey(), entry.getValue().getPluginId(), entry.getValue().getPluginClassName(),
                             interfaceNamesBuilder.toString());
            }
        }
    }

    @Override
    public List<PluginConfiguration> getAllPluginConfigurations() {
        final Iterable<PluginConfiguration> confs = pluginConfRepository.findAll();
        return (confs != null) ? Lists.newArrayList(confs) : Collections.emptyList();
    }

    private List<String> getPluginPackage() {
        // Manage scan packages
        if (pluginPackage == null) {
            pluginPackage = new ArrayList<>();
            if ((packagesToScan != null) && (packagesToScan.length > 0)) {
                for (String packageToScan : packagesToScan) {
                    pluginPackage.add(packageToScan);
                }
            }
        }
        return pluginPackage;
    }

    @Override
    @MultitenantTransactional(propagation = Propagation.SUPPORTS)
    public void addPluginPackage(final String pPluginPackage) {
        // First time initiliaze the plugins for the configured package
        getLoadedPlugins();

        if (!getPluginPackage().contains(pPluginPackage)) {
            getPluginPackage().add(pPluginPackage);
            final Map<String, PluginMetaData> newPlugins = PluginUtils.getPlugins(pPluginPackage, getPluginPackage());
            if (plugins == null) {
                // in case the plugin service has been initialized with PluginService(IPluginRepository) constructor
                plugins = new ConcurrentHashMap<>();
            }
            plugins.putAll(newPlugins);
        }
        logPluginServiceState("addPluginPackage");
    }

    @Override
    public PluginMetaData checkPluginClassName(final Class<?> pClass, final String pPluginClassName)
            throws EntityInvalidException {
        PluginMetaData metaData = null;
        boolean isFound = false;

        // Search all the plugins of type pClass
        for (final PluginMetaData pMd : getPluginsByType(pClass)) {
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

    @Override
    public PluginConfiguration getPluginConfigurationByLabel(final String pConfigurationLabel)
            throws EntityNotFoundException {
        final PluginConfiguration conf = pluginConfRepository.findOneByLabel(pConfigurationLabel);
        if (conf == null) {
            throw new EntityNotFoundException(pConfigurationLabel, PluginConfiguration.class);
        }
        return conf;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.modules.plugins.service.IPluginService#addPluginToCache(java.lang.String,
     * java.lang.Long, java.lang.Object)
     */
    @Override
    public void addPluginToCache(Long pConfId, Object pPlugin) {
        Assert.notNull(pConfId, "Plugin configuration identifier is required");
        Assert.notNull(pPlugin, "Plugin instance is required");

        ConcurrentMap<Long, Object> tenantCache = getPluginCache();
        if (tenantCache == null) {
            // Init tenant cache
            tenantCache = new ConcurrentHashMap<>();
            instantiatePlugins.put(runtimeTenantResolver.getTenant(), tenantCache);
        }
        tenantCache.put(pConfId, pPlugin);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.modules.plugins.service.IPluginService#isPluginCached(java.lang.String,
     * java.lang.Long)
     */
    @Override
    public boolean isPluginCached(Long pConfId) {
        Assert.notNull(pConfId, "Plugin configuration identifier is required");

        ConcurrentMap<Long, Object> tenantCache = getPluginCache();
        if (tenantCache != null) {
            return tenantCache.containsKey(pConfId);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.modules.plugins.service.IPluginService#cleanPluginCache(java.lang.String,
     * java.lang.Long)
     */
    @Override
    public void cleanPluginCache(Long pConfId) {
        Assert.notNull(pConfId, "Plugin configuration identifier is required");

        ConcurrentMap<Long, Object> tenantCache = getPluginCache();
        if (tenantCache != null) {
            // Remove plugin from cache
            Object plugin = tenantCache.remove(pConfId);
            if (plugin != null) {
                // Launch destroy method
                PluginUtils.doDestroyPlugin(plugin);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.modules.plugins.service.IPluginService#getPluginCache()
     */
    @Override
    public ConcurrentMap<Long, Object> getPluginCache() {
        // Resolve tenant
        String tenant = runtimeTenantResolver.getTenant();
        Assert.notNull(tenant, "Tenant is required");

        return instantiatePlugins.get(tenant);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.modules.plugins.service.IPluginService#getCachedPlugin(java.lang.Long)
     */
    @Override
    public Object getCachedPlugin(Long pConfId) {
        Assert.notNull(pConfId, "Plugin configuration identifier is required");

        ConcurrentMap<Long, Object> tenantCache = getPluginCache();
        if (tenantCache != null) {
            return tenantCache.get(pConfId);
        }
        return null;
    }

    /**
     * Return a {@link PluginConfiguration} for a plugin identifier.
     * If it does not exists, the {@link PluginConfiguration} is created.
     *
     * @param pluginId a pluginidentifier
     * @param interfacePluginType the {@link PluginInterface}
     * @return a {@link PluginConfiguration}
     * @throws ModuleException
     *             an error is trhown
     */
    @Override
    public PluginConfiguration getPluginConfiguration(String pluginId, Class<?> interfacePluginType)
            throws ModuleException {

        // Test if a configuration exists for this pluginId
        List<PluginConfiguration> pluginConfigurations = this.getPluginConfigurationsByType(interfacePluginType);

        if (!pluginConfigurations.isEmpty()) {
            PluginConfiguration plgConf = loadPluginConfiguration(pluginId, pluginConfigurations);
            if (plgConf != null) {
                return plgConf;
            }
        }

        // Get the PluginMetadata
        List<PluginMetaData> plgMetaDatas = this.getPluginsByType(interfacePluginType);

        Optional<PluginMetaData> aPlgMetaData = plgMetaDatas.stream().filter(mtd -> mtd.getPluginId().equals(pluginId))
                .findAny();

        if (aPlgMetaData.isPresent()) {

            PluginConfiguration pluginConfiguration = new PluginConfiguration(aPlgMetaData.get(),
                    "Automatic plugin configuration for plugin id : " + pluginId);
            pluginConfiguration.setPluginId(pluginId);

            List<PluginParameter> plgParams = new ArrayList<>();
            for (PluginParameterType param : aPlgMetaData.get().getParameters()) {
                PluginParameter plgParam = new PluginParameter(param.getName(), param.getDefaultValue());
                plgParam.setName(param.getName());
                plgParam.setValue(param.getDefaultValue());
                plgParam.setIsDynamic(param.isOptional());
                plgParams.add(plgParam);
            }
            pluginConfiguration.setParameters(plgParams);

            return this.savePluginConfiguration(pluginConfiguration);
        } else {
            throw new ModuleException(
                    "Unexpected error : the plugin id <" + pluginId + " > is found more that one time in a Plugin");
        }
    }

    /**
     * Return a {@link PluginConfiguration} for a pluginId
     *
     * @param pluginId the pluginid to search
     *
     * @return the found {@link PluginConfiguration}
     */
    private PluginConfiguration loadPluginConfiguration(String pluginId, List<PluginConfiguration> pluginConfs) {
        PluginConfiguration foundPlgConf = null;
        boolean exist = false;

        for (PluginConfiguration aPluginConf : pluginConfs) {
            if (!exist) {
                exist = aPluginConf.getPluginId().equals(pluginId);
                if (exist) {
                    foundPlgConf = aPluginConf;
                }
            }
        }

        return foundPlgConf;
    }

}
