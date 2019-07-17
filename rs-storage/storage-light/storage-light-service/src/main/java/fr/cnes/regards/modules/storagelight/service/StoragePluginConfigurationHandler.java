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
package fr.cnes.regards.modules.storagelight.service;

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
import fr.cnes.regards.modules.storagelight.domain.plugin.IDataStorage;

/**
 * @author sbinda
 *
 */
@Component
public class StoragePluginConfigurationHandler implements IHandler<PluginConfEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoragePluginConfigurationHandler.class);

    private final SetMultimap<String, String> existingStorages = LinkedHashMultimap.create();

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @PostConstruct
    public void init() {
        // Listen for plugins modifications
        subscriber.subscribeTo(PluginConfEvent.class, this);
    }

    @Override
    public void handle(TenantWrapper<PluginConfEvent> wrapper) {
        String tenant = wrapper.getTenant();
        if ((wrapper.getContent().getPluginTypes().contains(IDataStorage.class.getName()))) {
            runtimeTenantResolver.forceTenant(tenant);
            LOGGER.info("New data storage plugin conf {} for tenant {}", wrapper.getContent().getPluginConfId(),
                        wrapper.getTenant());
            try {
                switch (wrapper.getContent().getAction()) {
                    case CREATE:
                        this.existingStorages.put(tenant, pluginService
                                .getPluginConfiguration(wrapper.getContent().getPluginConfId()).getLabel());
                        break;
                    case DELETE:
                        this.existingStorages.remove(tenant, pluginService
                                .getPluginConfiguration(wrapper.getContent().getPluginConfId()).getLabel());
                        break;
                    case ACTIVATE:
                    case DISABLE:
                    case UPDATE:
                    default:
                        break;
                }
            } catch (EntityNotFoundException e) {
                // Nothing to do, message is not valid.
                LOGGER.error(e.getMessage(), e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    public Set<String> getConfiguredStorages() {
        if (this.existingStorages.get(runtimeTenantResolver.getTenant()) == null) {
            this.refresh(runtimeTenantResolver.getTenant());
        }
        return this.existingStorages.get(runtimeTenantResolver.getTenant());
    }

    public void refresh() {
        this.refresh(runtimeTenantResolver.getTenant());
    }

    private void refresh(String tenant) {
        List<PluginConfiguration> confs = pluginService.getPluginConfigurationsByType(IDataStorage.class);
        confs.forEach(c -> this.existingStorages.put(tenant, c.getLabel()));
    }

}
