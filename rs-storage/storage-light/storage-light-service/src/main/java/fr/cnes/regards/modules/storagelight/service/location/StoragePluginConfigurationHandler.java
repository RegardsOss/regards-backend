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
package fr.cnes.regards.modules.storagelight.service.location;

import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.IOnlineStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;

/**
 * This component handle the pool of {@link IStorageLocation} plugins configuration as known as {@link PrioritizedStorage}.
 *
 * @author Sébastien Binda
 */
@Component
public class StoragePluginConfigurationHandler implements IHandler<PluginConfEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoragePluginConfigurationHandler.class);

    private final SetMultimap<String, String> storages = LinkedHashMultimap.create();

    private final SetMultimap<String, String> onlineStorages = LinkedHashMultimap.create();

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @PostConstruct
    public void init() {
        subscriber.subscribeTo(PluginConfEvent.class, this);
    }

    /**
     * When a {@link PluginConfiguration} is created, updated or deleted handle the event to update the list of plugin configurations.
     */
    @Override
    public void handle(TenantWrapper<PluginConfEvent> wrapper) {
        String tenant = wrapper.getTenant();
        if ((wrapper.getContent().getPluginTypes().contains(IStorageLocation.class.getName()))) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                PluginConfiguration conf = pluginService.getPluginConfiguration(wrapper.getContent().getPluginConfId());
                switch (wrapper.getContent().getAction()) {
                    case CREATE:
                        this.storages.put(tenant, conf.getLabel());
                        if (conf.getInterfaceNames().contains(IOnlineStorageLocation.class.getName())) {
                            this.onlineStorages.put(tenant, conf.getLabel());
                        }
                        break;
                    case DELETE:
                        this.storages.remove(tenant, conf.getLabel());
                        this.onlineStorages.remove(tenant, conf.getLabel());
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
    public Set<String> getConfiguredStorages() {
        if ((this.storages.get(runtimeTenantResolver.getTenant()) == null)
                || this.storages.get(runtimeTenantResolver.getTenant()).isEmpty()) {
            this.refresh(runtimeTenantResolver.getTenant());
        }
        return this.storages.get(runtimeTenantResolver.getTenant());
    }

    /**
     * Return all the online storage location configured {@link PluginConfiguration} labels.
     */
    public Set<String> getConfiguredOnlineStorages() {
        if ((this.onlineStorages.get(runtimeTenantResolver.getTenant()) == null)
                || this.onlineStorages.get(runtimeTenantResolver.getTenant()).isEmpty()) {
            this.refresh(runtimeTenantResolver.getTenant());
        }
        return this.onlineStorages.get(runtimeTenantResolver.getTenant());
    }

    /**
     * Refresh the list of configured storage locations for the current user tenant.
     */
    public void refresh() {
        this.refresh(runtimeTenantResolver.getTenant());
    }

    /**
     * Refresh the list of configured storage locations for the given tenant.
     */
    private void refresh(String tenant) {
        List<PluginConfiguration> confs = pluginService.getPluginConfigurationsByType(IStorageLocation.class);
        List<PluginConfiguration> onlineConfs = pluginService
                .getPluginConfigurationsByType(IOnlineStorageLocation.class);
        confs.forEach(c -> this.storages.put(tenant, c.getLabel()));
        onlineConfs.forEach(c -> this.onlineStorages.put(tenant, c.getLabel()));
    }

    public boolean isOnline(String storage) {
        return this.getConfiguredOnlineStorages().contains(storage);
    }

}
