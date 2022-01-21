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
package fr.cnes.regards.modules.storage.service.location;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.event.BroadcastPluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineStorageLocation;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageLocation;

/**
 * This component handle the pool of {@link IStorageLocation} plugins configuration as known as {@link StorageLocationConfiguration}.
 *
 * @author Sébastien Binda
 */
@Component
public class StoragePluginConfigurationHandler implements IHandler<BroadcastPluginConfEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoragePluginConfigurationHandler.class);

    private final SetMultimap<String, PluginConfiguration> storages = LinkedHashMultimap.create();

    private final SetMultimap<String, PluginConfiguration> onlineStorages = LinkedHashMultimap.create();

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ITenantResolver tenantResolver;

    @EventListener(ApplicationStartedEvent.class)
    public void init() {
        subscriber.subscribeTo(BroadcastPluginConfEvent.class, this);
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                refresh();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * When a {@link PluginConfiguration} is created, updated or deleted handle the event to update the list of plugin configurations.
     */
    @Override
    public void handle(TenantWrapper<BroadcastPluginConfEvent> wrapper) {
        String tenant = wrapper.getTenant();
        if ((wrapper.getContent().getPluginTypes().contains(IStorageLocation.class.getName()))) {
            runtimeTenantResolver.forceTenant(tenant);
            try {

                switch (wrapper.getContent().getAction()) {
                    case CREATE:
                        PluginConfiguration conf = pluginService
                                .getPluginConfiguration(wrapper.getContent().getPluginBusinnessId());
                        this.storages.put(tenant, conf);
                        if (conf.getInterfaceNames().contains(IOnlineStorageLocation.class.getName())) {
                            this.onlineStorages.put(tenant, conf);
                        }
                        break;
                    case DELETE:
                        BroadcastPluginConfEvent pluginEvent = wrapper.getContent();
                        this.storages.remove(tenant, pluginEvent.getLabel());
                        this.onlineStorages.remove(tenant, pluginEvent.getLabel());
                        break;
                    case ACTIVATE:
                    case DISABLE:
                    case UPDATE:
                    default:
                        break;
                }
            } catch (EntityNotFoundException e) {
                // Nothing to do, the storage does not match an existing plugin configuration. Only log error.
                LOGGER.error(e.getMessage(), e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Return all the configured {@link PluginConfiguration} labels.
     */
    public Set<PluginConfiguration> getConfiguredStorages() {
        if ((this.storages.get(runtimeTenantResolver.getTenant()) == null)
                || this.storages.get(runtimeTenantResolver.getTenant()).isEmpty()) {
            this.refresh();
            LOGGER.trace("[STORAGE CONFIGURATION] Plugin configuration list refreshed !");
        }
        return this.storages.get(runtimeTenantResolver.getTenant());
    }

    public Optional<PluginConfiguration> getConfiguredStorage(String storage) {
        return getConfiguredStorages().stream().filter(c -> c.getLabel().equals(storage)).findFirst();
    }

    /**
     * Return all the online storage location configured {@link PluginConfiguration} labels.
     */
    private Set<PluginConfiguration> getConfiguredOnlineStorages() {
        if ((this.onlineStorages.get(runtimeTenantResolver.getTenant()) == null)
                || this.onlineStorages.get(runtimeTenantResolver.getTenant()).isEmpty()) {
            this.refresh();
        }
        return this.onlineStorages.get(runtimeTenantResolver.getTenant());
    }

    /**
     * Refresh the list of configured storage locations for the current user tenant.
     */
    public void refresh() {
        List<PluginConfiguration> confs = pluginService.getPluginConfigurationsByType(IStorageLocation.class);
        List<PluginConfiguration> onlineConfs = confs.stream()
                .filter(c -> c.getInterfaceNames().contains(IOnlineStorageLocation.class.getName()))
                .collect(Collectors.toList());
        confs.forEach(c -> this.storages.put(runtimeTenantResolver.getTenant(), c));
        onlineConfs.forEach(c -> this.onlineStorages.put(runtimeTenantResolver.getTenant(), c));
    }

    public boolean isConfigured(String storage) {
        return this.getConfiguredStorages().stream().anyMatch(c -> c.getLabel().equals(storage));
    }

    public boolean isOnline(String storage) {
        return this.getConfiguredOnlineStorages().stream().anyMatch(c -> c.getLabel().equals(storage));
    }

}
