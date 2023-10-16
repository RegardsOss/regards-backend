/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.event.BroadcastPluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginServiceAction;
import fr.cnes.regards.framework.modules.plugins.dto.PluginConfigurationDto;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.PluginParamDescriptor;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.NestedPluginParam;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The implementation of {@link IPluginService}.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 * <p>
 * TODO V3 : with hot plugin loading, be careful to properly clean the plugin cache when plugin version change
 */
@MultitenantTransactional
@Service
public class PluginService implements IPluginService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginService.class);

    private static final String PLUGIN_BUSINESS_ID_REQUIRED_MSG = "Plugin configuration business identifier is required";

    public static final String ERROR_WHILE_GETTING_THE_PLUGIN_CONFIGURATION = "Error while getting the plugin configuration {}.";

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
     * instantiated.
     * <b>Note: </b> PluginService is used in multi-thread environment (see IngesterService and CrawlerService) so
     * ConcurrentHashMap is used instead of HashMap
     */
    private final ConcurrentMap<String, ConcurrentMap<String, Object>> instantiatePluginMap = new ConcurrentHashMap<>();

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

    public PluginService(IPluginConfigurationRepository pluginConfigurationRepository,
                         IPublisher publisher,
                         IRuntimeTenantResolver runtimeTenantResolver,
                         IEncryptionService encryptionService,
                         Gson gson) {
        this.repos = pluginConfigurationRepository;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
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
    @Transactional(noRollbackFor = { EntityNotFoundException.class, EntityNotEmptyException.class })
    public PluginConfiguration savePluginConfiguration(PluginConfiguration plgConf)
        throws EntityInvalidException, EncryptionException, EntityNotFoundException {

        // Check plugin configuration validity
        List<String> validationErrors = PluginUtils.validateOnCreate(plgConf);
        if (!validationErrors.isEmpty()) {
            throw new EntityInvalidException(validationErrors);
        }

        StringBuilder msg = new StringBuilder("Cannot save plugin configuration");
        PluginConfiguration pluginConfInDb = repos.findCompleteByBusinessId(plgConf.getBusinessId());
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
            if (Boolean.TRUE.equals(paramMeta.isSensible())) {
                manageSensibleParameter(plgConf.getParameter(paramMeta.getName()));
            }
        }

        PluginConfiguration newConf = repos.save(plgConf);
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
     * Set decrypted value for plugin instantiation.
     */
    private void decryptSensibleParameter(PluginMetaData pluginMetadata, PluginConfigurationDto conf)
        throws EncryptionException {
        for (PluginParamDescriptor paramType : pluginMetadata.getParameters()) {
            // only decrypt STRING plugin parameter for now.
            if (paramType.getType() == PluginParamType.STRING) {
                StringPluginParam pluginParam = (StringPluginParam) conf.getParameter(paramType.getName());
                if (Boolean.TRUE.equals((pluginParam != null) && paramType.isSensible()) && pluginParam.hasValue()) {
                    conf.getParameters().remove(pluginParam);
                    StringPluginParam decryptedParam = pluginParam.clone();
                    decryptedParam.setValue(encryptionService.decrypt(decryptedParam.getValue()));
                    conf.getParameters().add(decryptedParam);
                }
            }
        }
    }

    /**
     * Method to ensure that the given configuration to save will be the only one active if his
     * {@link PluginInterface} is configured to allow only one active conf.
     *
     * @param plgConf {@link PluginConfiguration} to save
     */
    @Transactional(noRollbackFor = { EntityNotFoundException.class, EntityNotEmptyException.class })
    public void ensureOnlyOneConfIsActive(PluginConfiguration plgConf)
        throws EncryptionException, EntityInvalidException, EntityNotFoundException {
        if (Boolean.TRUE.equals(plgConf.isActive())) {
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
                List<PluginConfiguration> confs = repos.findAll();
                for (PluginConfiguration conf : confs) {
                    if (Boolean.TRUE.equals(!conf.getId().equals(plgConf.getId()) && conf.isActive())
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
    @Transactional(noRollbackFor = { EntityNotFoundException.class, EntityNotEmptyException.class })
    public PluginConfiguration getPluginConfiguration(String businessId) throws EntityNotFoundException {
        PluginConfiguration plgConf = repos.findCompleteByBusinessId(businessId);
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
        return repos.findCompleteByBusinessId(businessId);
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
        boolean exists = repos.existsByBusinessId(businessId);
        LOGGER.trace(exists ? "Plugin [businessId={}] exists" : "Plugin [businessId={}] does not exist", businessId);
        return exists;
    }

    @Override
    @Transactional(noRollbackFor = { EntityNotFoundException.class, EntityNotEmptyException.class })
    public PluginConfiguration updatePluginConfiguration(PluginConfiguration pluginConf)
        throws EntityNotFoundException, EntityInvalidException, EncryptionException {
        final PluginConfiguration oldConf = repos.findCompleteByBusinessId(pluginConf.getBusinessId());
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
                if (Boolean.TRUE.equals(paramMeta.isSensible()) && (oldParam == null
                                                                    || !Objects.equals(newParam.getValue(),
                                                                                       oldParam.getValue()))) {
                    manageSensibleParameter(newParam);
                }
            }
        }

        ensureOnlyOneConfIsActive(pluginConf);

        PluginConfiguration newConf = repos.save(pluginConf);
        newConf.setMetaDataAndPluginId(pluginMeta);

        if (oldConfActive != Boolean.TRUE.equals(newConf.isActive())) {
            // For CATALOG
            publisher.publish(new BroadcastPluginConfEvent(pluginConf.getId(),
                                                           newConf.getBusinessId(),
                                                           newConf.getLabel(),
                                                           Boolean.TRUE.equals(newConf.isActive()) ?
                                                               PluginServiceAction.ACTIVATE :
                                                               PluginServiceAction.DISABLE,
                                                           pluginMeta.getInterfaceNames()));
            // For DAM
            publisher.publish(new PluginConfEvent(pluginConf.getId(),
                                                  newConf.getBusinessId(),
                                                  newConf.getLabel(),
                                                  Boolean.TRUE.equals(newConf.isActive()) ?
                                                      PluginServiceAction.ACTIVATE :
                                                      PluginServiceAction.DISABLE,
                                                  pluginMeta.getInterfaceNames()));
        } else {
            publisher.publish(new PluginConfEvent(pluginConf.getId(),
                                                  newConf.getBusinessId(),
                                                  newConf.getLabel(),
                                                  PluginServiceAction.UPDATE,
                                                  pluginMeta.getInterfaceNames()));
        }
        // Remove the plugin configuration from cache
        cleanRecursively(pluginConf);

        return newConf;
    }

    public PluginConfiguration updatePluginConfiguration(PluginConfiguration plugin,
                                                         Class<?> pluginType,
                                                         boolean onlyOneActiveByType) throws ModuleException {
        if (onlyOneActiveByType && Boolean.TRUE.equals(plugin.isActive())) {
            // First disable all other active configurations
            List<PluginConfiguration> confs = repos.findAll();
            for (PluginConfiguration conf : confs) {
                if ((conf.getId().longValue() != plugin.getId().longValue())
                    && conf.getInterfaceNames()
                           .contains(pluginType.getName())
                    && Boolean.TRUE.equals(conf.isActive())) {
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
        Set<PluginConfiguration> parentPluginConfs = getDependentPlugins(pluginConf.getBusinessId());
        parentPluginConfs.forEach(this::cleanRecursively);
        cleanPluginCache(pluginConf.getBusinessId());
    }

    @Override
    @Transactional(noRollbackFor = { EntityNotFoundException.class, EntityNotEmptyException.class })
    public void deletePluginConfiguration(String businessId) throws ModuleException {
        PluginConfiguration toDelete = repos.findCompleteByBusinessId(businessId);
        if (toDelete == null) {
            LOGGER.error("Error while deleting the plugin configuration {}.", businessId);
            throw new EntityNotFoundException(businessId, PluginConfiguration.class);
        }
        if (!getDependentPlugins(businessId).isEmpty()) {
            throw new EntityOperationForbiddenException("Operation cancelled: dependent plugin configurations exist.");
        }
        PluginMetaData pluginMeta = PluginUtils.getPlugins().get(toDelete.getPluginId());
        publisher.publish(new BroadcastPluginConfEvent(toDelete.getId(),
                                                       toDelete.getBusinessId(),
                                                       toDelete.getLabel(),
                                                       PluginServiceAction.DELETE,
                                                       Option.of(pluginMeta)
                                                             .map(PluginMetaData::getInterfaceNames)
                                                             .getOrElse(new HashSet<>())));
        repos.deleteById(toDelete.getId());

        // Remove the PluginConfiguration from the map
        PluginUtils.doDestroyPlugin(toDelete);
        cleanPluginCache(toDelete.getBusinessId());
    }

    /**
     * @return list of dependent plugin configuration
     */
    private Set<PluginConfiguration> getDependentPlugins(String businessId) {
        Set<PluginConfiguration> dependents = new HashSet<>();
        for (PluginConfiguration conf : repos.findAll()) {
            for (IPluginParam param : conf.getParameters()) {
                if (param.getType() == PluginParamType.PLUGIN) {
                    NestedPluginParam nested = (NestedPluginParam) param;
                    if (businessId.equals(nested.getValue())) {
                        dependents.add(conf);
                    }
                }
            }
        }
        return dependents;
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(Class<?> interfacePluginType) {
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for (PluginConfiguration pluginConfiguration : repos.findAll()) {
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
    public boolean canInstantiate(String businessId) throws ModuleException, NotAvailablePluginConfigurationException {
        try {
            getPlugin(businessId);
            return true;
        } catch (PluginUtilsRuntimeException e) {
            LOGGER.warn(String.format("Plugin with configuration %s couldn't be instantiated", businessId), e);
            return false;
        }
    }

    /**
     * Retrieve a plugin for a given tenant by adding the new instanciated plugin in tenant cache map
     * if it does not exist
     *
     * @param plgConf           plugin conf to instanciate
     * @param pluginCacheTenant tenant
     * @return new tenant plugin cache map
     */
    private ConcurrentMap<String, Object> getPluginForTenant(PluginConfiguration plgConf,
                                                             ConcurrentMap<String, Object> pluginCacheTenant) {
        ConcurrentMap<String, Object> newCacheForThisTenant = pluginCacheTenant == null ?
            new ConcurrentHashMap<>() :
            pluginCacheTenant;
        instantiateInnerPlugins(plgConf, newCacheForThisTenant);
        newCacheForThisTenant.computeIfAbsent(plgConf.getBusinessId(), bid -> {
            try {
                return instantiatePlugin(loadPluginConfiguration(plgConf.getBusinessId()), newCacheForThisTenant);
            } catch (ModuleException | NotAvailablePluginConfigurationException e) {
                throw new RsRuntimeException(e);
            }
        });
        return newCacheForThisTenant;
    }

    /**
     * retreive Plugin type parameters from given plugin conf and instantiate the plugin if not already in cache.
     *
     * @param plgConf           {@link PluginConfiguration} to check and instantiate missing inner parameter plugins
     * @param pluginCacheTenant Cache of already instantiated plugin conf for the current tenant
     */
    private void instantiateInnerPlugins(PluginConfiguration plgConf, ConcurrentMap<String, Object> pluginCacheTenant) {
        Iterator<PluginConfiguration> it = getInnerPluginsConf(plgConf).descendingIterator();
        while (it.hasNext()) {
            PluginConfiguration innerConf = it.next();
            pluginCacheTenant.computeIfAbsent(innerConf.getBusinessId(), bid -> {
                try {
                    return instantiatePlugin(innerConf, pluginCacheTenant);
                } catch (ModuleException | NotAvailablePluginConfigurationException e) {
                    throw new RsRuntimeException(e);
                }
            });
        }
    }

    /**
     * Retrieve ordered list of {@link PluginConfiguration} matching all the given plugin inner plugin parameters.
     *
     * @param pluginConf {@link PluginConfiguration} to check
     * @return Ordered list of inner plugin configuration of the fiven plugin configuration
     */
    private LinkedList<PluginConfiguration> getInnerPluginsConf(PluginConfiguration pluginConf) {
        LinkedList<PluginConfiguration> innerConfList = new LinkedList<>();

        for (PluginParamDescriptor paramType : getPluginMetadata(pluginConf.getPluginId()).getParameters()) {
            if (paramType.getType() == PluginParamType.PLUGIN) {
                NestedPluginParam pluginParam = (NestedPluginParam) pluginConf.getParameter(paramType.getName());
                if ((pluginParam != null) && pluginParam.hasValue() && !pluginParam.getValue()
                                                                                   .equals(pluginConf.getBusinessId())) {
                    PluginConfiguration innerPluginConf = loadPluginConfiguration(pluginParam.getValue());
                    // Add inner plugin to result list
                    innerConfList.add(innerPluginConf);
                    // Check if inner plugin contains other inner plugins and add them to result list
                    innerConfList.addAll(getInnerPluginsConf(innerPluginConf));
                }
            }
        }
        LOGGER.debug("Found {} inner plugin(s) for : {}", innerConfList.size(), pluginConf.getBusinessId());
        return innerConfList;
    }

    @Override
    @Transactional(noRollbackFor = { ModuleException.class, NotAvailablePluginConfigurationException.class })
    @SuppressWarnings("unchecked")
    public <T> T getPlugin(PluginConfiguration plgConf, IPluginParam... dynamicParameters)
        throws ModuleException, NotAvailablePluginConfigurationException {
        String tenant = runtimeTenantResolver.getTenant();
        Assert.notNull(tenant, "Tenant is required");
        Assert.notNull(plgConf, "Plugin conf can not be null");

        if (dynamicParameters.length == 0) {
            try {
                return (T) instantiatePluginMap.compute(tenant,
                                                        (t, cacheForThisTenant) -> getPluginForTenant(plgConf,
                                                                                                      cacheForThisTenant))
                                               .get(plgConf.getBusinessId());
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
            instantiateInnerPlugins(plgConf, instantiatePluginMap.get(tenant));
            return instantiatePlugin(plgConf, instantiatePluginMap.get(tenant), dynamicParameters);
        }
    }

    @Override
    @Transactional(noRollbackFor = { ModuleException.class, NotAvailablePluginConfigurationException.class })
    @SuppressWarnings("unchecked")
    public <T> T getPlugin(String businessId, IPluginParam... dynamicParameters)
        throws ModuleException, NotAvailablePluginConfigurationException {
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

    /**
     * Retrieve plugin {@link PluginMetaData} or raise a {@link PluginMetadataNotFoundRuntimeException} if not found.
     *
     * @param pluginId Plugin identifier to load
     * @return PluginMetaData
     */
    private PluginMetaData getPluginMetadata(String pluginId) {
        PluginMetaData pluginMetadata = PluginUtils.getPluginMetadata(pluginId);
        if (pluginMetadata == null) {
            LOGGER.debug("No plugin metadata found for plugin configuration id {}", pluginId);
            logPluginMetadataScanned();
            throw new PluginMetadataNotFoundRuntimeException("Metadata not found for plugin configuration identifier "
                                                             + pluginId);
        }
        return pluginMetadata;
    }

    /**
     * Instantiate a plugin.
     *
     * @param pluginConf        {@link PluginConfiguration} plugin configuration
     * @param tenantPluginCache plugin cache for this tenant
     * @param dynamicParameters plugin parameters (including potential dynamic ones)
     * @return plugin instance
     */
    @Transactional(noRollbackFor = { EntityNotFoundException.class,
                                     EntityNotEmptyException.class,
                                     PluginUtilsRuntimeException.class })
    private <T> T instantiatePlugin(PluginConfiguration pluginConf,
                                    ConcurrentMap<String, Object> tenantPluginCache,
                                    IPluginParam... dynamicParameters)
        throws ModuleException, NotAvailablePluginConfigurationException {
        // Check if all parameters are really dynamic
        for (IPluginParam dynamicParameter : dynamicParameters) {
            if (!dynamicParameter.isDynamic()) {
                String errorMessage = String.format(
                    "The parameter \"%s\" is not identified as dynamic. Plugin instantiation is cancelled.",
                    dynamicParameter.getName());
                LOGGER.error(errorMessage);
                throw new UnexpectedDynamicParameterException(errorMessage);
            }
        }

        // Get the plugin implementation associated
        PluginMetaData pluginMetadata = getPluginMetadata(pluginConf.getPluginId());

        if (!Objects.equals(pluginMetadata.getVersion(), pluginConf.getVersion())) {
            throw new CannotInstanciatePluginException(String.format(
                "Plugin configuration version (%s) is different from plugin one (%s).",
                pluginConf.getVersion(),
                pluginMetadata.getVersion()));
        }

        PluginConfigurationDto pluginConfDto = pluginConf.toDto();
        decryptSensibleParameter(pluginMetadata, pluginConfDto);

        LOGGER.info("New plugin instantiation for {}configuration {} of plugin {}",
                    dynamicParameters.length > 0 ? "dynamic " : "",
                    pluginConf.getBusinessId(),
                    pluginMetadata.getPluginId());

        return PluginUtils.getPlugin(pluginConfDto, pluginMetadata, tenantPluginCache, dynamicParameters);
    }

    private void logPluginMetadataScanned() {
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

            LOGGER.debug("Available pluginMap metadata : {} -> {} / {} / {}",
                         entry.getKey(),
                         entry.getValue().getPluginId(),
                         entry.getValue().getPluginClassName(),
                         buf);
        }
    }

    @Override
    public List<PluginConfiguration> getAllPluginConfigurations() {
        return repos.findAll(Sort.by(Sort.Direction.ASC, "label"));
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
    @Transactional(noRollbackFor = { EntityNotFoundException.class, EntityNotEmptyException.class })
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
    public void cleanPluginCache(String businessId) {
        Assert.notNull(businessId, PLUGIN_BUSINESS_ID_REQUIRED_MSG);

        // Remove plugin from cache
        String tenant = runtimeTenantResolver.getTenant();
        instantiatePluginMap.computeIfPresent(tenant, (t, cacheForThisTenant) -> {
            cacheForThisTenant.computeIfPresent(businessId, (bid, instantiatedPlugin) -> {
                // Launch destroy method
                PluginUtils.doDestroyPlugin(instantiatedPlugin);
                return null;
            });
            return cacheForThisTenant;
        });
    }

    @Override
    public void cleanPluginCache() {
        String tenant = runtimeTenantResolver.getTenant();
        instantiatePluginMap.computeIfPresent(tenant, (t, cacheForThisTenant) -> {
            for (String businessId : cacheForThisTenant.keySet()) {
                cacheForThisTenant.computeIfPresent(businessId, (bis, instantiatedPlugin) -> {
                    // Launch destroy method
                    PluginUtils.doDestroyPlugin(instantiatedPlugin);
                    return null;
                });
            }
            return new ConcurrentHashMap<>();
        });
    }

    @Override
    public PluginConfiguration prepareForExport(PluginConfiguration pluginConf) {

        // Retrieve meta information
        PluginMetaData pluginMeta = PluginUtils.getPlugins().get(pluginConf.getPluginId());

        // Create a clone if sensible plugin
        for (PluginParamDescriptor paramDesc : pluginMeta.getParameters()) {
            if (Boolean.TRUE.equals(paramDesc.isSensible())) {
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

            if (Boolean.TRUE.equals(paramDesc.isSensible())) {
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
