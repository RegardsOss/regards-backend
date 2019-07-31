/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.modules.plugins.domain.PluginParamDescriptor;
import fr.cnes.regards.framework.modules.plugins.domain.event.BroadcastPluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginServiceAction;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.NestedPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.PluginParamType;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.StringPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;

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
     * {@link PluginConfiguration} JPA Repository
     */
    private final IPluginConfigurationRepository repos;

    /**
     * {@link IRuntimeTenantResolver}
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

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
                if (interfacePluginType == null
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
        if (pluginConfInDb != null && !Objects.equals(pluginConfInDb.getId(), plgConf.getId())
                && pluginConfInDb.getLabel().equals(plgConf.getLabel())) {
            msg.append(String.format(". A plugin configuration with same label (%s) already exists.",
                                     plgConf.getLabel()));
            throw new EntityInvalidException(msg.toString());
        }

        ensureOnlyOneConfIsActive(plgConf);
        boolean shouldPublishCreation = plgConf.getId() == null;

        // Now that generic concerns on PluginConfiguration are dealt with, lets encrypt sensitive plugin parameter
        // only way to know if a plugin parameter is sensitive is via the plugin metadata
        PluginMetaData pluginMeta = PluginUtils.getPlugins().get(plgConf.getPluginId());
        for (PluginParamDescriptor paramMeta : pluginMeta.getParameters()) {
            if (paramMeta.isSensible()) {
                manageSensibleParameter(plgConf.getParameter(paramMeta.getName()));
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

    private void manageSensibleParameter(IPluginParam param) throws EncryptionException {
        if (param != null && param.hasValue()) {
            if (param.getType().equals(PluginParamType.STRING)) {
                StringPluginParam sensibleParam = (StringPluginParam) param;
                sensibleParam.setValue(encryptionService.encrypt(sensibleParam.getValue()));
            } else {
                String message = String
                        .format("Only STRING type parameter can be encrypted at the moment : inconsistent type \"%s\" for parameter \"%s\"",
                                param.getType(), param.getName());
                LOGGER.error(message);
                throw new EncryptionException(message, null);
            }
        }
    }

    /**
     * Set decrypted value for plugin instanciation.
     */
    private void decryptSensibleParameter(PluginMetaData pluginMetadata, PluginConfiguration conf)
            throws EncryptionException {
        for (PluginParamDescriptor paramType : pluginMetadata.getParameters()) {
            if (paramType.isSensible()) {
                StringPluginParam pluginParam = (StringPluginParam) conf.getParameter(paramType.getName());
                pluginParam.setDecryptedValue(encryptionService.decrypt(pluginParam.getValue()));
            }
        }
    }

    /**
     * Method to ensure that the given configuration to save will be the only one active if his
     * {@link PluginInterface} is configured to allow only one active conf.
     * @param plgConf {@link PluginConfiguration} to save
     */
    public void ensureOnlyOneConfIsActive(PluginConfiguration plgConf)
            throws EncryptionException, EntityInvalidException, EntityNotFoundException {
        if (plgConf.isActive()) {
            List<String> uniqueActiveConfInterfaces = Lists.newArrayList();
            for (String interfaceName : plgConf.getInterfaceNames()) {
                PluginInterface pi;
                try {
                    pi = Class.forName(interfaceName).getAnnotation(PluginInterface.class);
                    if (pi != null && !pi.allowMultipleActiveConfigurations()) {
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
        PluginConfiguration plgConfOpt = repos.findCompleteById(id);
        if (plgConfOpt == null) {
            LOGGER.error(String.format("Error while getting the plugin configuration <%s>.", id));
            throw new EntityNotFoundException(id, PluginConfiguration.class);
        }
        return plgConfOpt;
    }

    @Override
    public PluginConfiguration loadPluginConfiguration(Long id) {
        return repos.findCompleteById(id);
    }

    @Override
    public boolean exists(Long id) {
        return repos.existsById(id);
    }

    @Override
    public boolean existsByLabel(String pluginConfLabel) {
        return repos.findOneByLabel(pluginConfLabel) != null;
    }

    @Override
    public PluginConfiguration updatePluginConfiguration(PluginConfiguration pluginConf)
            throws EntityNotFoundException, EntityInvalidException, EncryptionException {
        final PluginConfiguration oldConf = repos.findCompleteById(pluginConf.getId());
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

        // Now that generic concerns on PluginConfiguration are dealt with, lets encrypt updated sensitive plugin parameter
        // only way to know if a plugin parameter is sensitive is via the plugin metadata
        PluginMetaData pluginMeta = PluginUtils.getPlugins().get(pluginConf.getPluginId());
        for (PluginParamDescriptor paramMeta : pluginMeta.getParameters()) {
            IPluginParam newParam = pluginConf.getParameter(paramMeta.getName());
            IPluginParam oldParam = oldConf.getParameter(paramMeta.getName());
            if (newParam != null && newParam.hasValue()) {
                // Check if parameter is sensitive and value changed. If it does, encrypt the new value
                if (paramMeta.isSensible() && !Objects.equals(newParam.getValue(), oldParam.getValue())) {
                    manageSensibleParameter(newParam);
                }
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
                if (conf.getId().longValue() != plugin.getId().longValue()
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
        parentPluginConfs.forEach(this::cleanRecursively);
        cleanPluginCache(pluginConf.getId());
    }

    @Override
    public void deletePluginConfiguration(Long confId) throws ModuleException {
        Optional<PluginConfiguration> toDeleteOpt = repos.findById(confId);
        if (!toDeleteOpt.isPresent()) {
            LOGGER.error(String.format("Error while deleting the plugin configuration <%d>.", confId));
            throw new EntityNotFoundException(confId.toString(), PluginConfiguration.class);
        }
        PluginConfiguration toDelete = toDeleteOpt.get();
        if (!repos.findByParametersPluginConfiguration(toDelete).isEmpty()) {
            throw new EntityOperationForbiddenException("Operation cancelled: dependent plugin configurations exist.");
        }
        publisher.publish(new BroadcastPluginConfEvent(confId, PluginServiceAction.DELETE,
                toDelete.getInterfaceNames()));
        repos.deleteById(confId);

        // Remove the PluginConfiguration from the map
        PluginUtils.doDestroyPlugin(toDeleteOpt);
        cleanPluginCache(confId);
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(Class<?> interfacePluginType) {
        return repos.findAll().stream().filter(conf -> conf.getInterfaceNames().contains(interfacePluginType.getName()))
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
    public <T> T getFirstPluginByType(Class<?> interfacePluginType, IPluginParam... dynamicParameters)
            throws ModuleException, NotAvailablePluginConfigurationException {

        // Get pluginMap configuration for given type
        final List<PluginConfiguration> confs = getPluginConfigurationsByType(interfacePluginType);

        if (confs.isEmpty()) {
            throw new ModuleException(
                    String.format("No plugin configuration defined for the type <%s>.", interfacePluginType.getName()));
        }

        // Search configuration with upper priority
        PluginConfiguration configuration = null;

        for (final PluginConfiguration conf : confs) {
            if (configuration == null || conf.getPriorityOrder() < configuration.getPriorityOrder()) {
                configuration = conf;
            }
        }

        // Get the plugin associated to this configuration
        return configuration != null ? getPlugin(configuration.getId(), dynamicParameters) : null;
    }

    /**
     * We consider only plugin without dynamic parameters so we can profit from the cache system.
     * @return whether a plugin conf, without dynamic parameters is instanciable or not
     * @throws ModuleException when no plugin configuration with this id exists
     * @throws NotAvailablePluginConfigurationException
     */
    @Override
    public boolean canInstantiate(Long pluginConfId) throws ModuleException, NotAvailablePluginConfigurationException {
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
    public <T> T getPlugin(Long pluginConfId, IPluginParam... dynamicParameters)
            throws ModuleException, NotAvailablePluginConfigurationException {

        if (!isPluginCached(pluginConfId) || dynamicParameters.length > 0) {
            return instanciatePluginAndCache(pluginConfId, dynamicParameters);
        }
        return (T) getCachedPlugin(pluginConfId);
    }

    /**
     * Load plugin from its business identifier
     * @param businessId business identifier
     * @throws NotAvailablePluginConfigurationException
     * @throws ModuleException
     */
    private void loadNestedPlugin(String businessId) throws ModuleException, NotAvailablePluginConfigurationException {
        PluginConfiguration conf = repos.findCompleteByBusinessId(businessId);
        getPlugin(conf.getId());
    }

    /**
     * Instanciate a plugin and cache it <b>if it doesn't have dynamic parameters</b>
     * @param pluginConfId plugin configuration identifier
     * @param dynamicParameters plugin parameters (including potential dynamic ones)
     * @return plugin instance
     */
    private <T> T instanciatePluginAndCache(Long pluginConfId, IPluginParam... dynamicParameters)
            throws ModuleException, NotAvailablePluginConfigurationException {

        // Check if all parameters are really dynamic
        for (IPluginParam dynamicParameter : dynamicParameters) {
            if (!dynamicParameter.isDynamic()) {
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
        for (PluginParamDescriptor paramType : pluginMetadata.getParameters()) {
            if (paramType.getParamType() == PluginParamType.PLUGIN) {
                NestedPluginParam pluginParam = (NestedPluginParam) pluginConf.getParameter(paramType.getName());
                if (pluginParam != null && pluginParam.hasValue()) {
                    // LOAD embedded plugin from its business identifier
                    this.loadNestedPlugin(pluginParam.getValue());
                }
            }
        }

        decryptSensibleParameter(pluginMetadata, pluginConf);

        T resultPlugin = PluginUtils.getPlugin(pluginConf, pluginMetadata, getPluginCache(), dynamicParameters);

        // Put in the map, only if there is no dynamic parameters
        if (dynamicParameters.length == 0) {
            addPluginToCache(pluginConfId, pluginConf.getBusinessId(), resultPlugin);
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

    private void addPluginToCache(Long confId, String businessId, Object plugin) {
        Assert.notNull(confId, PLUGIN_CONF_ID_REQUIRED_MSG);
        Assert.notNull(plugin, "Plugin instance is required");
        getPluginCache().put(confId, plugin);
    }

    @Override
    public boolean isPluginCached(Long confId) {
        Assert.notNull(confId, PLUGIN_CONF_ID_REQUIRED_MSG);
        return getPluginCache().containsKey(confId);
    }

    @Override
    public void cleanPluginCache(Long confId) {
        Assert.notNull(confId, PLUGIN_CONF_ID_REQUIRED_MSG);

        // Remove plugin from cache
        Object plugin = getPluginCache().remove(confId);
        if (plugin != null) {
            // Launch destroy method
            PluginUtils.doDestroyPlugin(plugin);
        }
    }

    @Override
    public void cleanPluginCache() {
        // Remove plugin from cache
        for (Iterator<Entry<Long, Object>> i = getPluginCache().entrySet().iterator(); i.hasNext();) {
            Object plugin = i.next().getValue();
            i.remove();
            if (plugin != null) {
                // Launch destroy method
                PluginUtils.doDestroyPlugin(plugin);
            }
        }
    }

    /**
     * @return a not null map of plugins
     */
    @Override
    public Map<Long, Object> getPluginCache() {
        // Resolve tenant
        String tenant = runtimeTenantResolver.getTenant();
        Assert.notNull(tenant, "Tenant is required");
        Map<Long, Object> tenantCache = instantiatePluginMap.get(tenant);
        if (tenantCache == null) {
            // Init tenant cache
            tenantCache = new ConcurrentHashMap<>();
            instantiatePluginMap.put(runtimeTenantResolver.getTenant(), tenantCache);
        }
        return tenantCache;
    }

    @Override
    public Object getCachedPlugin(Long confId) {
        Assert.notNull(confId, PLUGIN_CONF_ID_REQUIRED_MSG);
        return getPluginCache().get(confId);
    }

    @Override
    public PluginConfiguration prepareForExport(PluginConfiguration pluginConf) {

        // Retrieve meta information
        PluginMetaData pluginMeta = PluginUtils.getPlugins().get(pluginConf.getPluginId());

        // Create a clone if sensible plugin
        for (PluginParamDescriptor paramDesc : pluginMeta.getParameters()) {
            if (paramDesc.isSensible()) {
                // Create a clone with decrypted value
                return cloneSensiblePlugin(pluginMeta, pluginConf);
            }
        }

        // Default behaviour : return passed configuration
        return pluginConf;
    }

    /**
     * Make a shallow clone of this sensible plugin configuration with decrypted values
     */
    private PluginConfiguration cloneSensiblePlugin(PluginMetaData pluginMeta, PluginConfiguration pluginConf) {

        // Create a clone with decrypted value
        PluginConfiguration exportedConf = new PluginConfiguration(pluginMeta, pluginConf.getLabel(),
                pluginConf.getPriorityOrder());
        exportedConf.setBusinessId(pluginConf.getBusinessId());
        exportedConf.setIsActive(false);
        exportedConf.setIconUrl(pluginConf.getIconUrl());

        // Handle parameters
        for (PluginParamDescriptor paramDesc : pluginMeta.getParameters()) {

            // Get related configuration parameter
            IPluginParam param = pluginConf.getParameter(paramDesc.getName());

            if (paramDesc.isSensible()) {
                StringPluginParam source = (StringPluginParam) param;
                StringPluginParam sensibleParam = IPluginParam.build(source.getName(), source.getValue());
                try {
                    // Try to decrypt
                    sensibleParam = IPluginParam.build(source.getName(), encryptionService.decrypt(source.getValue()));
                } catch (EncryptionException e) { // NOSONAR (only message is usable, not need to log e
                    // Nothing to do
                    LOGGER.warn("Error decrypting sensitive parameter {}:{}. Cause : {}.", pluginConf.getPluginId(),
                                param.getName(), e.getMessage());
                }
                exportedConf.getParameters().add(sensibleParam);
            } else {
                exportedConf.getParameters().add(param);
            }
        }

        return exportedConf;
    }
}
