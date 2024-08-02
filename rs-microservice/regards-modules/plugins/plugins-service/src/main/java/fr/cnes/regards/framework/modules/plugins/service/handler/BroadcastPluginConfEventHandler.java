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
package fr.cnes.regards.framework.modules.plugins.service.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.plugins.domain.event.BroadcastPluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.service.PluginCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Handle {@link BroadcastPluginConfEvent} to remove a single plugin instance from the plugin cache for specific tenant
 *
 * @author Léo Mieulet
 */
@Component
public class BroadcastPluginConfEventHandler implements IHandler<BroadcastPluginConfEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastPluginConfEventHandler.class);

    private final PluginCache pluginCache;

    private final ISubscriber subscriber;

    public BroadcastPluginConfEventHandler(PluginCache pluginCache, ISubscriber subscriber) {
        this.pluginCache = pluginCache;
        this.subscriber = subscriber;
    }

    @EventListener
    public void handleApplicationReadyEvent(ApplicationReadyEvent applicationReadyEvent) {
        subscriber.subscribeTo(BroadcastPluginConfEvent.class, this);
    }

    @Override
    public void handle(String tenant, BroadcastPluginConfEvent event) {
        long start = System.currentTimeMillis();
        LOGGER.debug("Received BroadcastPluginConfEvent for plugin configuration businessId {}. Event {}",
                     event.getPluginBusinnessId(),
                     event);
        if (event.getAction().isRequireCleanCache()) {
            pluginCache.cleanPluginRecursively(tenant, event.getPluginBusinnessId());
        }
        LOGGER.info("Processed BroadcastPluginConfEvent for plugin configuration businessId {}. Took {}ms",
                    event.getPluginBusinnessId(),
                    System.currentTimeMillis() - start);
    }
}
