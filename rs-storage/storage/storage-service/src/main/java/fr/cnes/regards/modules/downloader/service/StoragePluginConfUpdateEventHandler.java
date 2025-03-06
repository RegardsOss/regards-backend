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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.downloader.service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Event handler only on downloader service thanks to profile.
 * This handler clean plugin cache, when storage send a StoragePluginConfEvent to inform that a plugin conf has
 * changed.
 * As storage and downloader share the same plugins, downloader needs to update its plugin cache when storage update a
 * plugin.
 *
 * @author Sébastien Binda
 **/
@Profile("downloader")
@Component
public class StoragePluginConfUpdateEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IHandler<StoragePluginConfEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoragePluginConfUpdateEventHandler.class);

    private final ISubscriber subscriber;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IPluginService pluginService;

    public StoragePluginConfUpdateEventHandler(ISubscriber subscriber,
                                               IRuntimeTenantResolver runtimeTenantResolver,
                                               IPluginService pluginService) {
        this.subscriber = subscriber;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.pluginService = pluginService;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        subscriber.subscribeTo(StoragePluginConfEvent.class, this);
    }

    @Override
    public void handle(String tenant, StoragePluginConfEvent event) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.info("Clean cache plugin for plugin conf id {}", event.getBusinessId());
            pluginService.cleanLocalPluginCache(event.getBusinessId());
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }
}
