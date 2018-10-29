/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
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
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;

/**
 * The implementation of {@link IPluginService}.
 * @author Christophe Mertz
 * @author Sébastien Binda
 *
 * TODO V3 : with hot plugin loading, be careful to properly clean the plugin cache when plugin version change
 */
@MultitenantTransactional
@Service
public class PluginService implements IPluginService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginService.class);

    /**
     * Error message when a plugin configuration identifier is missing
     */
    private static final String PLUGIN_CONF_ID_REQUIRED_MSG = "Plugin configuration identifier is required";

    /**
     * {@link IRuntimeTenantResolver}
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * {@link PluginConfiguration} JPA Repository
     */
    private final IPluginConfigurationRepository repos;

    /**
     * A {@link Map} with all the {@link Plugin} currently instantiate by tenant.</br>
     * This Map is used because for a {@link PluginConfiguration}, one and only one {@link Plugin} should be
     * instantiate.
     * <b>Note: </b> PluginService is used in multi-thread environment (see IngesterService and CrawlerService) so
     * ConcurrentHashMap is used instead of HashMap
     */
    private final Map<String, Map<Long, Object>> instantiatePluginMap = new ConcurrentHashMap<>();

    /**
     * Amqp publisher
     */
    private final IPublisher publisher;

    private final IEncryptionService encryptionService;

    @Value("${regards.plugins.packages-to-scan:#{null}}")
    private String[] packagesToScan;

    public PluginService(IPluginConfigurationRepository pluginConfigurationRepository, IPublisher publisher,
            IRuntimeTenantResolver runtimeTenantResolver, IEncryptionService encryptionService) {
        this.repos = pluginConfigurationRepository;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.encryptionService = encryptionService;
    }

    @PostConstruct
    public void setup() {
        PluginUtils.setup(packagesToScan == null ? null : Arrays.asList(packagesToScan));
    }

    @Override
    public Set<String> getPluginTypes() {
        return PluginUtils.getPluginInterfaces();
    }

    @Override
    public Set<String> getAvailablePluginTypes() {
        Set<String> instantiablePluginTypes = new HashSet<>();
        PluginUtils.getPlugins().forEach((id, meta) -> instantiablePluginTypes.addAll(meta.getInterfaceNames()));
        return instantiablePluginTypes;
    }

    @Override
    public List<PluginMetaData> getPlugins() {
        return getPluginsByType(null);
    }

    @Override
    public List<PluginMetaData> getPluginsByType(Class<?> interfacePluginType) {
        List<PluginMetaData> availablePlugins = new ArrayList<>();

        PluginUtils.getPlugins().forEach((pluginId, metaData) -> {
            try {
                if ((interfacePluginType == null)
                        || interfacePluginType.isAssignableFrom(Class.forName(metaData.getPluginClassName()))) {
                    availablePlugins.add(metaData);
                }
            } catch (final ClassNotFoundException e) {
                LOGGER.error("cannot instantiate the class : %s" + metaData.getPluginClassName(), e);
            }
        });
        Collections.sort(availablePlugins);
        return availablePlugins;
    }

    @Override
    public PluginConfiguration savePluginConfiguration(PluginConfiguration plgConf)
            throws EntityInvalidException, EncryptionException, EntityNotFoundException {
        // Check plugin configuration validity
        EntityInvalidException validityException = PluginUtils.validate(plgConf);
        if (validityException != null) {
            throw validityException;
        }

        StringBuilder msg = new StringBuilder("Cannot save plugin configuration");
        PluginConfiguration pluginConfInDb = repos.findOneByLabel(plgConf.getLabel());
        if ((pluginConfInDb != null) && !Objects.equals(pluginConfInDb.getId(), plgConf.getId())
                && pluginConfInDb.getLabel().equals(plgConf.getLabel())) {
            msg.append(String.format(". A plugin configuration with same label (%s) already exists.",
                                     plgConf.getLabel()));
            throw new EntityInvalidException(msg.toString());
        }

        PluginUtils.getPlugins().forEach((pluginId, metaData) -> {
            if (metaData.getPluginClassName().equals(plgConf.getPluginClassName())) {
                plgConf.setInterfaceNames(metaData.getInterfaceNames());
            }
        });

        ensureOnlyOneConfIsActive(plgConf);
        boolean shouldPublishCreation = (plgConf.getId() == null);

        // Now that generic concerns on PluginConfiguration are dealt with, lets encrypt sensitive plugin parameter
        // only way to know if a plugin parameter is sensitive is via the plugin metadata
        PluginMetaData pluginMeta = PluginUtils.getPlugins().get(plgConf.getPluginId());
        for (PluginParameterType paramMeta : pluginMeta.getParameters()) {
            if (paramMeta.isSensible()) {
                PluginParameter param = plgConf.getParameter(paramMeta.getName());
                PluginParametersFactory.updateParameter(param,
                                                        encryptionService.encrypt(param.getStripParameterValue()));
            }
        }

        PluginConfiguration newConf = repos.save(plgConf);
        if (shouldPublishCreation) {
            publisher.publish(new BroadcastPluginConfEvent(newConf.getId(), PluginServiceAction.CREATE,
                    newConf.getInterfaceNames()));
            publisher.publish(new PluginConfEvent(newConf.getId(), PluginServiceAction.CREATE,
                    newConf.getInterfaceNames()));

        }
        return newConf;
    }

    /**
     * Method to ensure that the given configuration to save will be the only one active if his
     * {@link PluginInterface} is configured to allow only one active conf.
     * @param plgConf {@link PluginConfiguration} to save
     * @throws EncryptionException
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    public void ensureOnlyOneConfIsActive(PluginConfiguration plgConf)
            throws EncryptionException, EntityInvalidException, EntityNotFoundException {
        if (plgConf.isActive()) {
            List<String> uniqueActiveConfInterfaces = Lists.newArrayList();
            for (String interfaceName : plgConf.getInterfaceNames()) {
                PluginInterface pi;
                try {
                    pi = Class.forName(interfaceName).getAnnotation(PluginInterface.class);
                    if ((pi != null) && !pi.allowMultipleConfigurationActive()) {
                        uniqueActiveConfInterfaces.add(interfaceName);
                    }
                } catch (ClassNotFoundException e) {
                    // Nothing to do interface does not exists
                    LOGGER.warn(e.getMessage(), e);
                }
            }
            if (!uniqueActiveConfInterfaces.isEmpty()) {
                // First disable all other active configurations
                List<PluginConfiguration> confs = repos.findAll();
                for (PluginConfiguration conf : confs) {
                    if (!conf.getId().equals(plgConf.getId()) && conf.isActive()
                            && !Collections.disjoint(conf.getInterfaceNames(), uniqueActiveConfInterfaces)) {
                        conf.setIsActive(false);
                        LOGGER.info("As only one active configuration is allowed, the plugin {} is disabled. The new active plugin is {}",
                                    conf.getLabel(), plgConf.getLabel());
                        updatePluginConfiguration(conf);
                    }
                }
            }
        }
    }

    @Override
    public PluginConfiguration getPluginConfiguration(Long id) throws EntityNotFoundException {
        PluginConfiguration plgConf = repos.findOne(id);
        if (plgConf == null) {
            LOGGER.error(String.format("Error while getting the plugin configuration <%s>.", id));
            throw new EntityNotFoundException(id, PluginConfiguration.class);
        }
        return plgConf;
    }

    @Override
    public PluginConfiguration loadPluginConfiguration(Long id) {
        return repos.findById(id);
    }

    @Override
    public boolean exists(Long id) {
        return repos.exists(id);
    }

    @Override
    public boolean existsByLabel(String pluginConfLabel) {
        return repos.findOneByLabel(pluginConfLabel) != null;
    }

    @Override
    public PluginConfiguration updatePluginConfiguration(PluginConfiguration pluginConf)
            throws EntityNotFoundException, EntityInvalidException, EncryptionException {
        final PluginConfiguration oldConf = repos.findById(pluginConf.getId());
        if (oldConf == null) {
            LOGGER.error(String.format("Error while updating the plugin configuration <%d>.", pluginConf.getId()));
            throw new EntityNotFoundException(pluginConf.getId().toString(), PluginConfiguration.class);
        }
        boolean oldConfActive = oldConf.isActive();
        // Check plugin configuration validity
        EntityInvalidException validityException = PluginUtils.validate(pluginConf);
        if (validityException != null) {
            throw validityException;
        }

        PluginUtils.getPlugins().forEach((pluginId, metaData) -> {
            if (metaData.getPluginClassName().equals(pluginConf.getPluginClassName())) {
                pluginConf.setInterfaceNames(metaData.getInterfaceNames());
            }
        });

        // Now that generic concerns on PluginConfiguration are dealt with, lets encrypt updated sensitive plugin parameter
        // only way to know if a plugin parameter is sensitive is via the plugin metadata
        PluginMetaData pluginMeta = PluginUtils.getPlugins().get(pluginConf.getPluginId());
        for (PluginParameterType paramMeta : pluginMeta.getParameters()) {
            PluginParameter newParam = pluginConf.getParameter(paramMeta.getName());
            PluginParameter oldParam = oldConf.getParameter(paramMeta.getName());
            if ((newParam != null) && (newParam.getValue() != null) && !newParam.getStripParameterValue().isEmpty()) {
                // Check if parameter is sensitive and value changed. If it does, encrypt the new value
                if (paramMeta.isSensible()
                        && !Objects.equals(newParam.getStripParameterValue(), oldParam.getStripParameterValue())) {
                    PluginParametersFactory
                            .updateParameter(newParam, encryptionService.encrypt(newParam.getStripParameterValue()));
                }
            } else if (newParam != null) {
                // Plugin param value is null or empty, so remove the parameter
                pluginConf.getParameters().remove(newParam);
            }
        }

        ensureOnlyOneConfIsActive(pluginConf);

        PluginConfiguration newConf = repos.save(pluginConf);

        if (oldConfActive != newConf.isActive()) {
            // For CATALOG
            publisher.publish(new BroadcastPluginConfEvent(pluginConf.getId(),
                    newConf.isActive() ? PluginServiceAction.ACTIVATE : PluginServiceAction.DISABLE,
                    newConf.getInterfaceNames()));
            // For DAM
            publisher.publish(new PluginConfEvent(pluginConf.getId(),
                    newConf.isActive() ? PluginServiceAction.ACTIVATE : PluginServiceAction.DISABLE,
                    newConf.getInterfaceNames()));
        } else {
            publisher.publish(new PluginConfEvent(pluginConf.getId(), PluginServiceAction.UPDATE,
                    newConf.getInterfaceNames()));
        }
        // Remove the plugin configuration from cache
        cleanRecursively(pluginConf);

        return newConf;
    }

    public PluginConfiguration updatePluginConfiguration(PluginConfiguration plugin, Class<?> pluginType,
            boolean onlyOneActiveByType) throws ModuleException {
        if (onlyOneActiveByType && plugin.isActive()) {
            // First desable all other active configurations
            List<PluginConfiguration> confs = repos.findAll();
            for (PluginConfiguration conf : confs) {
                if ((conf.getId().longValue() != plugin.getId().longValue())
                        && conf.getInterfaceNames().contains(pluginType.getName()) && conf.isActive()) {
                    conf.setIsActive(false);
                    updatePluginConfiguration(conf);
                }
            }
        }
        // Finally do update the configuration
        return updatePluginConfiguration(plugin);
    }

    /**
     * Clean from cache given plugin configuration and all those which use it (recursively)
     */
    private void cleanRecursively(PluginConfiguration pluginConf) {
        // And don't forget to clean all PluginConfiguration that have this plugin as a parameter
        List<PluginConfiguration> parentPluginConfs = repos.findByParametersPluginConfiguration(pluginConf);
        parentPluginConfs.forEach(pc -> cleanRecursively(pc));
        cleanPluginCache(pluginConf.getId());
    }

    @Override
    public void deletePluginConfiguration(Long confId) throws ModuleException {
        final PluginConfiguration toDelete = repos.findOne(confId);
        if (toDelete == null) {
            LOGGER.error(String.format("Error while deleting the plugin configuration <%d>.", confId));
            throw new EntityNotFoundException(confId.toString(), PluginConfiguration.class);
        }
        if (!repos.findByParametersPluginConfiguration(toDelete).isEmpty()) {
            throw new EntityOperationForbiddenException("Operation cancelled: dependent plugin configurations exist.");
        }
        publisher.publish(new BroadcastPluginConfEvent(confId, PluginServiceAction.DELETE,
                toDelete.getInterfaceNames()));
        repos.delete(confId);

        // Remove the PluginConfiguration from the map
        PluginUtils.doDestroyPlugin(toDelete);
        cleanPluginCache(confId);
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(Class<?> interfacePluginType) {
        return repos.findAll().stream().filter(pc -> pc.getInterfaceNames().contains(interfacePluginType.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurations(String pluginId) {
        return repos.findByPluginIdOrderByPriorityOrderDesc(pluginId);
    }

    @Override
    public List<PluginConfiguration> getActivePluginConfigurations(String pluginId) {
        return repos.findByPluginIdAndActiveTrueOrderByPriorityOrderDesc(pluginId);
    }

    @Override
    public PluginMetaData getPluginMetaDataById(String pluginImplId) {
        return PluginUtils.getPlugins().get(pluginImplId);
    }

    @Override
    public <T> T getFirstPluginByType(Class<?> interfacePluginType, PluginParameter... dynamicParameters)
            throws ModuleException {

        // Get pluginMap configuration for given type
        final List<PluginConfiguration> confs = getPluginConfigurationsByType(interfacePluginType);

        if (confs.isEmpty()) {
            throw new ModuleException(
                    String.format("No plugin configuration defined for the type <%s>.", interfacePluginType.getName()));
        }

        // Search configuration with upper priority
        PluginConfiguration configuration = null;

        for (final PluginConfiguration conf : confs) {
            if ((configuration == null) || (conf.getPriorityOrder() < configuration.getPriorityOrder())) {
                configuration = conf;
            }
        }

        // Get the plugin associated to this configuration
        return (configuration != null) ? getPlugin(configuration.getId(), dynamicParameters) : null;
    }

    /**
     * We consider only plugin without dynamic parameters so we can profit from the cache system.
     * @return whether a plugin conf, without dynamic parameters is instanciable or not
     * @throws ModuleException when no plugin configuration with this id exists
     */
    @Override
    public boolean canInstantiate(Long pluginConfId) throws ModuleException {
        try {
            getPlugin(pluginConfId);
            return true;
        } catch (PluginUtilsRuntimeException e) {
            LOGGER.warn(String.format("Plugin with configuration %s couldn't be instanciated", pluginConfId), e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getPlugin(Long pluginConfId, PluginParameter... dynamicParameters) throws ModuleException {

        if (!isPluginCached(pluginConfId) || (dynamicParameters.length > 0)) {
            return instanciatePluginAndCache(pluginConfId, dynamicParameters);
        }
        return (T) getCachedPlugin(pluginConfId);
    }

    /**
     * Instanciate a plugin and cache it <b>if it doesn't have dynamic parameters</b>
     * @param pluginConfId plugin configuration identifier
     * @param dynamicParameters plugin parameters (including potential dynamic ones)
     * @return plugin instance
     * @throws ModuleException if error occurs!
     */
    private <T> T instanciatePluginAndCache(Long pluginConfId, PluginParameter... dynamicParameters)
            throws ModuleException {

        // Check if all parameters are really dynamic
        for (PluginParameter dynamicParameter : dynamicParameters) {
            if (!dynamicParameter.isDynamic() && !dynamicParameter.isOnlyDynamic()) {
                String errorMessage = String
                        .format("The parameter \"%s\" is not identified as dynamic. Plugin instanciation is cancelled.",
                                dynamicParameter.getName());
                LOGGER.error(errorMessage);
                throw new UnexpectedDynamicParameterException(errorMessage);
            }
        }

        // Get last saved plugin configuration
        PluginConfiguration pluginConf = loadPluginConfiguration(pluginConfId);

        // Get the plugin implementation associated
        PluginMetaData pluginMetadata = PluginUtils.getPlugins().get(pluginConf.getPluginId());

        if (pluginMetadata == null) {
            LOGGER.debug("No plugin metadata found for plugin configuration id {}", pluginConf.getPluginId());
            logPluginServiceState("instanciatePluginAndCache");
            throw new PluginMetadataNotFoundRuntimeException(
                    "Metadata not found for plugin configuration identifier " + pluginConf.getPluginId());
        }

        if (!Objects.equals(pluginMetadata.getVersion(), pluginConf.getVersion())) {
            throw new CannotInstanciatePluginException(
                    String.format("Plugin configuration version (%s) is different from plugin one (%s).",
                                  pluginConf.getVersion(), pluginMetadata.getVersion()));
        }

        // When pluginMap are loaded from database, maybe dependant pluginMap aren't yet loaded
        // So :
        // For all pluginMetada parameters, find PLUGIN ones, get key
        for (PluginParameterType paramType : pluginMetadata.getParameters()) {
            if (paramType.getParamType() == PluginParameterType.ParamType.PLUGIN) {
                String paramName = paramType.getName();
                // Now search from PluginConfiguration parameters associated Plugin
                for (PluginParameter param : pluginConf.getParameters()) {
                    if (param.getName().equals(paramName) && (param.getPluginConfiguration() != null)) {
                        // LOAD embedded plugin
                        this.getPlugin(param.getPluginConfiguration().getId());
                        break;
                    }
                }
            }
            if (paramType.isSensible()) {
                PluginParameter pluginParam = pluginConf.getParameter(paramType.getName());
                pluginParam.setDecryptedValue(encryptionService.decrypt(pluginParam.getStripParameterValue()));
            }
        }

        T resultPlugin = PluginUtils.getPlugin(pluginConf, pluginMetadata, getPluginCache(), dynamicParameters);

        // Put in the map, only if there is no dynamic parameters
        if (dynamicParameters.length == 0) {
            addPluginToCache(pluginConfId, resultPlugin);
        }

        return resultPlugin;
    }

    /**
     * Allows to add logs on this class state.
     * @param invokingMethod method that invoques logPluginServiceState
     */
    private void logPluginServiceState(String invokingMethod) {
        LOGGER.debug("logPluginServiceState invoked by : {}", invokingMethod);
        LOGGER.debug("This identifier: {}", this.toString());
        StringBuilder buf = new StringBuilder();
        for (Entry<String, PluginMetaData> entry : PluginUtils.getPlugins().entrySet()) {

            // Interfaces
            Iterator<String> interfaceIt = entry.getValue().getInterfaceNames().iterator();
            buf.delete(0, buf.length());
            buf.append("[");
            if (interfaceIt.hasNext()) {
                buf.append(interfaceIt.next());
                while (interfaceIt.hasNext()) {
                    buf.append(",");
                    buf.append(interfaceIt.next());
                }
            }
            buf.append("]");

            LOGGER.debug("Available pluginMap metadata : {} -> {} / {} / {}", entry.getKey(),
                         entry.getValue().getPluginId(), entry.getValue().getPluginClassName(), buf.toString());
        }
    }

    @Override
    public List<PluginConfiguration> getAllPluginConfigurations() {
        return repos.findAll();
    }

    @Override
    public PluginMetaData checkPluginClassName(Class<?> clazz, String pluginClassName) throws EntityInvalidException {
        // For all pluginMap of given type, find one with given class name
        Optional<PluginMetaData> optMetaData = getPluginsByType(clazz).stream()
                .filter(md -> md.getPluginClassName().equals(pluginClassName)).findAny();
        if (!optMetaData.isPresent()) {
            throw new EntityInvalidException("No plugin type matches the plugin class name : " + pluginClassName);
        }

        return optMetaData.get();
    }

    @Override
    public PluginConfiguration getPluginConfigurationByLabel(String configurationLabel) throws EntityNotFoundException {
        PluginConfiguration conf = repos.findOneByLabel(configurationLabel);
        if (conf == null) {
            throw new EntityNotFoundException(configurationLabel, PluginConfiguration.class);
        }
        return conf;
    }

    @Override
    public Optional<PluginConfiguration> findPluginConfigurationByLabel(String configurationLabel) {
        return Optional.ofNullable(repos.findOneByLabel(configurationLabel));
    }

    @Override
    public void addPluginToCache(Long confId, Object plugin) {
        Assert.notNull(confId, PLUGIN_CONF_ID_REQUIRED_MSG);
        Assert.notNull(plugin, "Plugin instance is required");

        Map<Long, Object> tenantCache = getPluginCache();
        if (tenantCache == null) {
            // Init tenant cache
            tenantCache = new ConcurrentHashMap<>();
            instantiatePluginMap.put(runtimeTenantResolver.getTenant(), tenantCache);
        }
        tenantCache.put(confId, plugin);
    }

    @Override
    public boolean isPluginCached(Long confId) {
        Assert.notNull(confId, PLUGIN_CONF_ID_REQUIRED_MSG);

        Map<Long, Object> tenantCache = getPluginCache();
        if (tenantCache != null) {
            return tenantCache.containsKey(confId);
        }
        return false;
    }

    @Override
    public void cleanPluginCache(Long confId) {
        Assert.notNull(confId, PLUGIN_CONF_ID_REQUIRED_MSG);

        Map<Long, Object> tenantCache = getPluginCache();
        if (tenantCache != null) {
            // Remove plugin from cache
            Object plugin = tenantCache.remove(confId);
            if (plugin != null) {
                // Launch destroy method
                PluginUtils.doDestroyPlugin(plugin);
            }
        }
    }

    @Override
    public void cleanPluginCache() {
        Map<Long, Object> tenantCache = getPluginCache();
        if (tenantCache != null) {
            // Remove plugin from cache
            for (Iterator<Entry<Long, Object>> i = tenantCache.entrySet().iterator(); i.hasNext();) {
                Object plugin = i.next().getValue();
                i.remove();
                if (plugin != null) {
                    // Launch destroy method
                    PluginUtils.doDestroyPlugin(plugin);
                }
            }
        }
    }

    @Override
    public Map<Long, Object> getPluginCache() {
        // Resolve tenant
        String tenant = runtimeTenantResolver.getTenant();
        Assert.notNull(tenant, "Tenant is required");

        return instantiatePluginMap.get(tenant);
    }

    @Override
    public Object getCachedPlugin(Long confId) {
        Assert.notNull(confId, PLUGIN_CONF_ID_REQUIRED_MSG);

        Map<Long, Object> tenantCache = getPluginCache();
        if (tenantCache != null) {
            return tenantCache.get(confId);
        }
        return null;
    }

    /**
     * Return a {@link PluginConfiguration} for a plugin identifier.
     * If it does not exists, the {@link PluginConfiguration} is created.
     * @param pluginId a plugin identifier
     * @param interfacePluginType the {@link PluginInterface}
     * @return a {@link PluginConfiguration}
     * @throws ModuleException an error is thrown
     */
    @Override
    public PluginConfiguration getPluginConfiguration(String pluginId, Class<?> interfacePluginType)
            throws ModuleException {

        // Test if a configuration exists for this pluginId
        List<PluginConfiguration> pluginConfigurations = getPluginConfigurationsByType(interfacePluginType);
        if (!pluginConfigurations.isEmpty()) {
            PluginConfiguration plgConf = loadPluginConfiguration(pluginId, pluginConfigurations);
            if (plgConf != null) {
                return plgConf;
            }
        }

        // Get the PluginMetadata
        List<PluginMetaData> plgMetaDatas = getPluginsByType(interfacePluginType);

        Optional<PluginMetaData> optPlgMetaData = plgMetaDatas.stream().filter(md -> md.getPluginId().equals(pluginId))
                .findAny();

        if (optPlgMetaData.isPresent()) {
            PluginMetaData metaData = optPlgMetaData.get();
            PluginConfiguration plgConf = new PluginConfiguration(metaData,
                    "Automatic plugin configuration for plugin id : " + pluginId);
            plgConf.setPluginId(pluginId);

            PluginParametersFactory ppFactory = PluginParametersFactory.build();
            metaData.getParameters()
                    .forEach(param -> ppFactory.addDynamicParameter(param.getName(), param.getDefaultValue()));
            plgConf.setParameters(ppFactory.getParameters());

            return this.savePluginConfiguration(plgConf);
        } else {
            throw new ModuleException(
                    "Unexpected error : the plugin id <" + pluginId + " > is found more that one time in a Plugin");
        }
    }

    /**
     * Return a {@link PluginConfiguration} for a pluginId
     * @param pluginId the pluginid to search
     * @return the found {@link PluginConfiguration}
     */
    private PluginConfiguration loadPluginConfiguration(String pluginId, List<PluginConfiguration> pluginConfs) {
        return pluginConfs.stream().filter(conf -> conf.getPluginId().equals(pluginId)).findAny().orElse(null);

    }
}
