/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.event.BroadcastPluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfClearCacheEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginServiceAction;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.PluginParamDescriptor;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.PluginParamType;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.StringPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The implementation of {@link IPluginService}.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 * <p>
 */
@Service
public class PluginService implements IPluginService, InitializingBean {

    public static final String ERROR_WHILE_GETTING_THE_PLUGIN_CONFIGURATION = "Error while getting the plugin configuration {}.";

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginService.class);

    private static final String PLUGIN_BUSINESS_ID_REQUIRED_MSG = "Plugin configuration business identifier is required";

    private final PluginConfigurationService pluginDaoService;

    /**
     * {@link IRuntimeTenantResolver}
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Plugin utils
     */
    private final PluginCache pluginCache;

    private final PluginInstantiationService pluginInstanceService;

    /**
     * Amqp publisher
     */
    private final IPublisher publisher;

    private final IEncryptionService encryptionService;

    /**
     * GSON instance for plugin transformation
     */
    private final Gson gson;

    @Value("${regards.plugins.packages-to-scan:#{null}}")
    private String[] packagesToScan;

    public PluginService(PluginConfigurationService pluginDaoService,
                         IPublisher publisher,
                         IRuntimeTenantResolver runtimeTenantResolver,
                         PluginCache pluginCache,
                         PluginInstantiationService pluginInstanceService,
                         IEncryptionService encryptionService,
                         Gson gson) {
        this.pluginDaoService = pluginDaoService;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.pluginCache = pluginCache;
        this.pluginInstanceService = pluginInstanceService;
        this.encryptionService = encryptionService;
        this.gson = gson;
    }

    @Override
    public void afterPropertiesSet() {
        PluginUtils.setup(packagesToScan == null ? null : Arrays.asList(packagesToScan), gson);
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
    @MultitenantTransactional(noRollbackFor = { EntityInvalidException.class,
                                                EncryptionException.class,
                                                EntityNotFoundException.class })
    public PluginConfiguration savePluginConfiguration(PluginConfiguration plgConf)
        throws EntityInvalidException, EncryptionException, EntityNotFoundException {

        // Check plugin configuration validity
        List<String> validationErrors = PluginUtils.validateOnCreate(plgConf);
        if (!validationErrors.isEmpty()) {
            throw new EntityInvalidException(validationErrors);
        }

        StringBuilder msg = new StringBuilder("Cannot save plugin configuration");
        PluginConfiguration pluginConfInDb = pluginDaoService.findCompleteByBusinessId(plgConf.getBusinessId());
        if ((pluginConfInDb != null)
            && !Objects.equals(pluginConfInDb.getId(), plgConf.getId())
            && pluginConfInDb.getBusinessId().equals(plgConf.getBusinessId())) {
            msg.append(String.format(". A plugin configuration with same businessId (%s) already exists.",
                                     plgConf.getBusinessId()));
            throw new EntityInvalidException(msg.toString());
        }

        PluginMetaData pluginMeta = PluginUtils.getPlugins().get(plgConf.getPluginId());
        plgConf.setMetaDataAndPluginId(pluginMeta);
        plgConf.setVersion(pluginMeta.getVersion());

        // Generate business id
        plgConf.generateBusinessIdIfNotSet();

        ensureOnlyOneConfIsActive(plgConf);
        boolean shouldPublishCreation = plgConf.getId() == null;

        // Now that generic concerns on PluginConfiguration are dealt with, lets encrypt sensitive plugin parameter
        // only way to know if a plugin parameter is sensitive is via the plugin metadata
        for (PluginParamDescriptor paramMeta : pluginMeta.getParameters()) {
            if (paramMeta.isSensible()) {
                manageSensibleParameter(plgConf.getParameter(paramMeta.getName()));
            }
        }

        PluginConfiguration newConf = pluginDaoService.savePluginConfiguration(plgConf);
        if (shouldPublishCreation) {
            publisher.publish(new BroadcastPluginConfEvent(newConf.getId(),
                                                           newConf.getBusinessId(),
                                                           newConf.getLabel(),
                                                           PluginServiceAction.CREATE,
                                                           newConf.getInterfaceNames()));
            publisher.publish(new PluginConfEvent(newConf.getId(),
                                                  newConf.getBusinessId(),
                                                  newConf.getLabel(),
                                                  PluginServiceAction.CREATE,
                                                  newConf.getInterfaceNames()));

        }
        return newConf;
    }

    private void manageSensibleParameter(IPluginParam param) throws EncryptionException {
        if ((param != null) && param.hasValue()) {
            if (param.getType().equals(PluginParamType.STRING)) {
                StringPluginParam sensibleParam = (StringPluginParam) param;
                sensibleParam.setValue(encryptionService.encrypt(sensibleParam.getValue()));
            } else {
                String message = String.format(
                    "Only STRING type parameter can be encrypted at the moment : inconsistent type \"%s\" for parameter \"%s\"",
                    param.getType(),
                    param.getName());
                LOGGER.error(message);
                throw new EncryptionException(message, null);
            }
        }
    }

    /**
     * Method to ensure that the given configuration to save will be the only one active if his
     * {@link PluginInterface} is configured to allow only one active conf.
     *
     * @param plgConf {@link PluginConfiguration} to save
     */
    @MultitenantTransactional(noRollbackFor = { EncryptionException.class,
                                                EntityInvalidException.class,
                                                EntityNotFoundException.class })
    public void ensureOnlyOneConfIsActive(PluginConfiguration plgConf)
        throws EncryptionException, EntityInvalidException, EntityNotFoundException {
        if (plgConf.isActive()) {
            List<String> uniqueActiveConfInterfaces = Lists.newArrayList();
            for (String interfaceName : plgConf.getInterfaceNames()) {
                PluginInterface pi;
                try {
                    pi = Class.forName(interfaceName).getAnnotation(PluginInterface.class);
                    if ((pi != null) && !pi.allowMultipleActiveConfigurations()) {
                        uniqueActiveConfInterfaces.add(interfaceName);
                    }
                } catch (ClassNotFoundException e) {
                    // Nothing to do interface does not exist
                    LOGGER.warn(e.getMessage(), e);
                }
            }
            if (!uniqueActiveConfInterfaces.isEmpty()) {
                // First disable all other active configurations
                List<PluginConfiguration> confs = pluginDaoService.findAllPluginConfigurations();
                for (PluginConfiguration conf : confs) {
                    if (!conf.getId().equals(plgConf.getId())
                        && conf.isActive()
                        && !Collections.disjoint(conf.getInterfaceNames(), uniqueActiveConfInterfaces)) {
                        conf.setIsActive(false);
                        LOGGER.info(
                            "As only one active configuration is allowed, the plugin <{} bId={}> is disabled. The new active plugin is <{} bId={}>",
                            conf.getLabel(),
                            conf.getBusinessId(),
                            plgConf.getLabel(),
                            plgConf.getBusinessId());
                        updatePluginConfiguration(conf);
                    }
                }
            }
        }
    }

    @Override
    @MultitenantTransactional(noRollbackFor = { EntityNotFoundException.class })
    public PluginConfiguration getPluginConfiguration(String businessId) throws EntityNotFoundException {
        PluginConfiguration plgConf = pluginDaoService.findCompleteByBusinessId(businessId);
        if (plgConf == null) {
            LOGGER.error(ERROR_WHILE_GETTING_THE_PLUGIN_CONFIGURATION, businessId);
            throw new EntityNotFoundException(businessId, PluginConfiguration.class);
        }
        PluginMetaData metaData = PluginUtils.getPlugins().get(plgConf.getPluginId());
        if (metaData == null) {
            LOGGER.error("Plugin {} is not available. Plugin is missing or service cannot access it.",
                         plgConf.getPluginId());
            throw new EntityNotFoundException(plgConf.getPluginId(), PluginMetaData.class);
        }
        plgConf.setMetaDataAndPluginId(metaData);
        return plgConf;
    }

    @Override
    public PluginConfiguration loadPluginConfiguration(String businessId) {
        return pluginDaoService.findCompleteByBusinessId(businessId);
    }

    @Override
    public void setMetadata(PluginConfiguration... pluginConfigurations) {
        Arrays.stream(pluginConfigurations)
              .forEach(pluginConfiguration -> pluginConfiguration.setMetaDataAndPluginId(PluginUtils.getPlugins()
                                                                                                    .get(
                                                                                                        pluginConfiguration.getPluginId())));
    }

    @Override
    public boolean exists(String businessId) {
        boolean exists = pluginDaoService.existsByBusinessId(businessId);
        LOGGER.trace(exists ? "Plugin [businessId={}] exists" : "Plugin [businessId={}] does not exist", businessId);
        return exists;
    }

    @Override
    @MultitenantTransactional(noRollbackFor = { EntityNotFoundException.class,
                                                EntityInvalidException.class,
                                                EncryptionException.class })
    public PluginConfiguration updatePluginConfiguration(PluginConfiguration pluginConf)
        throws EntityNotFoundException, EntityInvalidException, EncryptionException {
        final PluginConfiguration oldConf = pluginDaoService.findCompleteByBusinessId(pluginConf.getBusinessId());
        if (oldConf == null) {
            LOGGER.error("Error while updating the plugin configuration {}.", pluginConf.getId());
            throw new EntityNotFoundException(pluginConf.getLabel(), PluginConfiguration.class);
        }
        // Retrieve id
        pluginConf.setId(oldConf.getId());
        boolean oldConfActive = oldConf.isActive();

        // Check plugin configuration validity
        List<String> validationErrors = PluginUtils.validateOnUpdate(pluginConf);
        if (!validationErrors.isEmpty()) {
            throw new EntityInvalidException(validationErrors);
        }
        // Now that generic concerns on PluginConfiguration are dealt with, lets encrypt updated sensitive plugin parameter
        // only way to know if a plugin parameter is sensitive is via the plugin metadata
        PluginMetaData pluginMeta = PluginUtils.getPlugins().get(pluginConf.getPluginId());
        pluginConf.setMetaDataAndPluginId(pluginMeta);
        for (PluginParamDescriptor paramMeta : pluginMeta.getParameters()) {
            IPluginParam newParam = pluginConf.getParameter(paramMeta.getName());
            IPluginParam oldParam = oldConf.getParameter(paramMeta.getName());
            if ((newParam != null) && newParam.hasValue()) {
                // Check if parameter is sensitive and value changed. If it does, encrypt the new value
                if (paramMeta.isSensible() && (oldParam == null || !Objects.equals(newParam.getValue(),
                                                                                   oldParam.getValue()))) {
                    manageSensibleParameter(newParam);
                }
            }
        }

        ensureOnlyOneConfIsActive(pluginConf);

        PluginConfiguration newConf = pluginDaoService.savePluginConfiguration(pluginConf);
        newConf.setMetaDataAndPluginId(pluginMeta);
        if (oldConfActive != newConf.isActive()) {
            // Publish an event indicating the plugin availability changed (now available or unavailable)
            publisher.publish(new PluginConfEvent(pluginConf.getId(),
                                                  newConf.getBusinessId(),
                                                  newConf.getLabel(),
                                                  newConf.isActive() ?
                                                      PluginServiceAction.ACTIVATE :
                                                      PluginServiceAction.DISABLE,
                                                  pluginMeta.getInterfaceNames()));
        }
        // Event sent to all current microservice instances to inform this plugin conf changed.
        publisher.publish(new BroadcastPluginConfEvent(pluginConf.getId(),
                                                       newConf.getBusinessId(),
                                                       newConf.getLabel(),
                                                       PluginServiceAction.UPDATE,
                                                       pluginMeta.getInterfaceNames()));
        return newConf;
    }

    public PluginConfiguration updatePluginConfiguration(PluginConfiguration plugin,
                                                         Class<?> pluginType,
                                                         boolean onlyOneActiveByType) throws ModuleException {
        if (onlyOneActiveByType && plugin.isActive()) {
            // First disable all other active configurations
            List<PluginConfiguration> confs = pluginDaoService.findAllPluginConfigurations();
            for (PluginConfiguration conf : confs) {
                if ((conf.getId().longValue() != plugin.getId().longValue())
                    && conf.getInterfaceNames()
                           .contains(pluginType.getName())
                    && conf.isActive()) {
                    conf.setIsActive(false);
                    updatePluginConfiguration(conf);
                }
            }
        }
        // Finally do update the configuration
        return updatePluginConfiguration(plugin);
    }

    @Override
    @MultitenantTransactional(noRollbackFor = { EntityNotFoundException.class,
                                                EntityOperationForbiddenException.class })
    public void deletePluginConfiguration(String businessId)
        throws EntityNotFoundException, EntityOperationForbiddenException {
        PluginConfiguration toDelete = pluginDaoService.findCompleteByBusinessId(businessId);
        if (toDelete == null) {
            LOGGER.error("Error while deleting the plugin configuration {}.", businessId);
            throw new EntityNotFoundException(businessId, PluginConfiguration.class);
        }
        if (!pluginDaoService.getDependentPlugins(businessId).isEmpty()) {
            throw new EntityOperationForbiddenException("Operation cancelled: dependent plugin configurations exist.");
        }
        PluginMetaData pluginMeta = PluginUtils.getPlugins().get(toDelete.getPluginId());
        // remove it from the plugin cache on all instances
        publisher.publish(new BroadcastPluginConfEvent(toDelete.getId(),
                                                       toDelete.getBusinessId(),
                                                       toDelete.getLabel(),
                                                       PluginServiceAction.DELETE,
                                                       Option.of(pluginMeta)
                                                             .map(PluginMetaData::getInterfaceNames)
                                                             .getOrElse(new HashSet<>())));
        pluginDaoService.deleteById(toDelete.getId());
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(Class<?> interfacePluginType) {
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for (PluginConfiguration pluginConfiguration : pluginDaoService.findAllPluginConfigurations()) {
            PluginMetaData pluginMeta = PluginUtils.getPluginMetadata(pluginConfiguration.getPluginId());
            if (pluginMeta == null) {
                LOGGER.error("The plugin {} is not provided", pluginConfiguration.getPluginId());
            } else if (pluginMeta.getInterfaceNames().contains(interfacePluginType.getName())) {
                pluginConfiguration.setMetaDataAndPluginId(pluginMeta);
                pluginConfigurations.add(pluginConfiguration);
            }
        }
        pluginConfigurations.sort(Comparator.comparing(PluginConfiguration::getLabel));
        return pluginConfigurations;
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurations(String pluginId) {
        return pluginDaoService.findByPluginIdOrderByPriorityOrderDesc(pluginId);
    }

    @Override
    public List<PluginConfiguration> getActivePluginConfigurations(String pluginId) {
        return pluginDaoService.findByPluginIdAndActiveTrueOrderByPriorityOrderDesc(pluginId);
    }

    @Override
    public PluginMetaData getPluginMetaDataById(String pluginImplId) {
        return PluginUtils.getPlugins().get(pluginImplId);
    }

    @Override
    public <T> T getFirstPluginByType(Class<?> interfacePluginType, IPluginParam... dynamicParameters)
        throws ModuleException {

        // Get pluginMap configuration for given type
        final List<PluginConfiguration> confs = getPluginConfigurationsByType(interfacePluginType);

        if (confs.isEmpty()) {
            throw new ModuleException(String.format("No plugin configuration defined for the type <%s>.",
                                                    interfacePluginType.getName()));
        }

        // Search configuration with upper priority
        PluginConfiguration configuration = null;

        for (final PluginConfiguration conf : confs) {
            if ((configuration == null) || (conf.getPriorityOrder() < configuration.getPriorityOrder())) {
                configuration = conf;
            }
        }

        // Get the plugin associated to this configuration
        return configuration != null ? getPlugin(configuration.getBusinessId(), dynamicParameters) : null;
    }

    /**
     * We consider only plugin without dynamic parameters, so we can profit from the cache system.
     *
     * @return whether a plugin conf, without dynamic parameters is instantiable or not
     * @throws ModuleException                          when no plugin configuration with this business id exists
     * @throws NotAvailablePluginConfigurationException as per {@link #getPlugin(String, IPluginParam...)}
     */
    @Override
    public boolean canInstantiate(String businessId) throws ModuleException {
        try {
            getPlugin(businessId);
            return true;
        } catch (PluginUtilsRuntimeException e) {
            LOGGER.warn(String.format("Plugin with configuration %s couldn't be instantiated", businessId), e);
            return false;
        }
    }

    @Override
    @MultitenantTransactional(noRollbackFor = { ModuleException.class })
    @SuppressWarnings("unchecked")
    public <T> T getPlugin(PluginConfiguration plgConf, IPluginParam... dynamicParameters) throws ModuleException {
        String tenant = runtimeTenantResolver.getTenant();
        Assert.notNull(tenant, "Tenant is required");
        Assert.notNull(plgConf, "Plugin conf can not be null");

        if (dynamicParameters.length == 0) {
            try {
                return (T) pluginCache.getPluginForTenant(tenant, plgConf).get(plgConf.getBusinessId());
            } catch (RsRuntimeException e) {
                if (e.getCause() instanceof ModuleException me) {
                    throw me;
                }
                if (e.getCause() instanceof NotAvailablePluginConfigurationException na) {
                    throw na;
                }
                throw new CannotInstanciatePluginException(e.getMessage());
            }
        } else {
            ConcurrentHashMap<String, Object> instantiatePluginMap = pluginCache.getTenantCache(tenant);
            pluginInstanceService.instantiateInnerPlugins(plgConf, instantiatePluginMap);
            return pluginInstanceService.instantiatePlugin(plgConf, instantiatePluginMap, dynamicParameters);
        }
    }

    @Override
    @MultitenantTransactional(noRollbackFor = { ModuleException.class })
    public <T> T getPlugin(String businessId, IPluginParam... dynamicParameters) throws ModuleException {
        PluginConfiguration plgConf = loadPluginConfiguration(businessId);
        if (plgConf == null) {
            LOGGER.error(
                "Plugin Configuration with business id {} does not seems to exists. Did you confuse businessId and id?",
                businessId);
            throw new EntityNotFoundException(businessId, PluginConfiguration.class);
        }
        return getPlugin(plgConf, dynamicParameters);
    }

    @Override
    public <T> Optional<T> getOptionalPlugin(String businessId, IPluginParam... dynamicPluginParameters)
        throws NotAvailablePluginConfigurationException {
        Optional<T> plugin;
        try {
            plugin = Optional.of(getPlugin(businessId, dynamicPluginParameters));
        } catch (ModuleException e) {
            LOGGER.trace(e.getMessage(), e);
            plugin = Optional.empty();
        }
        return plugin;
    }

    @Override
    public List<PluginConfiguration> getAllPluginConfigurations() {
        return pluginDaoService.findAllPluginConfigurationsSorted(Sort.by(Sort.Direction.ASC, "label"));
    }

    @Override
    public PluginMetaData checkPluginClassName(Class<?> clazz, String pluginClassName) throws EntityInvalidException {
        // For all pluginMap of given type, find one with given class name
        Optional<PluginMetaData> optMetaData = getPluginsByType(clazz).stream()
                                                                      .filter(md -> md.getPluginClassName()
                                                                                      .equals(pluginClassName))
                                                                      .findAny();
        if (optMetaData.isEmpty()) {
            throw new EntityInvalidException("No plugin type matches the plugin class name : " + pluginClassName);
        }

        return optMetaData.get();
    }

    @Override
    @MultitenantTransactional(noRollbackFor = { EntityNotFoundException.class })
    public PluginConfiguration getPluginConfigurationByLabel(String configurationLabel) throws EntityNotFoundException {
        PluginConfiguration conf = pluginDaoService.findOneByLabel(configurationLabel);
        if (conf == null) {
            throw new EntityNotFoundException(configurationLabel, PluginConfiguration.class);
        }
        return conf;
    }

    @Override
    public Optional<PluginConfiguration> findPluginConfigurationByLabel(String configurationLabel) {
        return Optional.ofNullable(pluginDaoService.findOneByLabel(configurationLabel));
    }

    @Override
    public void cleanLocalPluginCache(String businessId) {
        Assert.notNull(businessId, PLUGIN_BUSINESS_ID_REQUIRED_MSG);
        // Remove plugin from cache
        pluginCache.cleanPluginRecursively(runtimeTenantResolver.getTenant(), businessId);
    }

    @Override
    public void cleanPluginCache() {
        publisher.publish(new PluginConfClearCacheEvent());
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
        PluginConfiguration exportedConf = new PluginConfiguration(pluginConf.getLabel(),
                                                                   pluginConf.getPriorityOrder(),
                                                                   pluginConf.getPluginId());
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
                } catch (EncryptionException e) {
                    LOGGER.warn("Error decrypting sensitive parameter {}:{}. Cause : {}.",
                                pluginConf.getPluginId(),
                                param.getName(),
                                e.getMessage());
                    LOGGER.debug(e.getMessage(), e);
                }
                exportedConf.getParameters().add(sensibleParam);
            } else {
                exportedConf.getParameters().add(param);
            }
        }

        return exportedConf;
    }

}
