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
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDataStorage;

/**
 * @author sbinda
 *
 */
@Component
@MultitenantTransactional
public class StoragePluginConfigurationHandler implements IHandler<PluginConfEvent> {

    // FIXME : Handle tenant ?
    private Set<String> existingStorages = Sets.newHashSet();

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ISubscriber subscriber;

    @PostConstruct
    public void init() {
        // Initialize existing storages
        List<PluginConfiguration> confs = pluginService.getPluginConfigurationsByType(IDataStorage.class);
        this.existingStorages = confs.stream().map(c -> c.getLabel()).collect(Collectors.toSet());
        // Listen for plugins modifications
        subscriber.subscribeTo(PluginConfEvent.class, this);
    }

    @Override
    public void handle(TenantWrapper<PluginConfEvent> wrapper) {
        if ((wrapper.getContent().getPluginTypes().contains(IDataStorage.class.getName()))) {
            try {
                switch (wrapper.getContent().getAction()) {
                    case CREATE:
                        this.existingStorages.add(pluginService
                                .getPluginConfiguration(wrapper.getContent().getPluginConfId()).getLabel());
                        break;
                    case DELETE:
                        this.existingStorages.remove(pluginService
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
            }
        }
    }

    public Set<String> getConfiguredStorages() {
        return this.existingStorages;
    }

}
