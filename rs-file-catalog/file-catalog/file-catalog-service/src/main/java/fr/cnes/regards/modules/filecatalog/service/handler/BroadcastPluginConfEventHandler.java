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
package fr.cnes.regards.modules.filecatalog.service.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.plugins.domain.event.BroadcastPluginConfEvent;
import fr.cnes.regards.modules.filecatalog.service.location.StorageLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler for {@link BroadcastPluginConfEvent} that are sent when a storage plugin configuration is updated.
 * This covers both update through storage ui / api and through microservice settings import.
 * <p>
 * When a storage configuration is updated, the storage configuration cache in {@link StorageLocationService} need to
 * be cleared so the new configuration (and not the cached one) will be retrieved the next time its requested.
 * <p>
 * It is not possible to determine whether a message concerns a storage plugin or another plugin,
 * so the cache is invalidated in all cases
 *
 * @author Thibaud Michaudel
 **/
public class BroadcastPluginConfEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<BroadcastPluginConfEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastPluginConfEventHandler.class);

    private final ISubscriber subscriber;

    private final StorageLocationService storageLocationService;

    @Value("${regards.file.catalog.plugin.event.bulk.size:25}")
    private int bulkSize;

    public BroadcastPluginConfEventHandler(ISubscriber subscriber, StorageLocationService storageLocationService) {
        this.subscriber = subscriber;
        this.storageLocationService = storageLocationService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(BroadcastPluginConfEvent.class, this);
    }

    @Override
    public void handleBatch(List<BroadcastPluginConfEvent> messages) {
        LOGGER.debug("[BroadcastPluginConfEvent HANDLER] Receiving {} BroadcastPluginConfEvent", messages.size());
        long start = System.currentTimeMillis();

        messages.forEach(message -> storageLocationService.invalidateCache(message.getPluginBusinnessId()));

        LOGGER.info("[BroadcastPluginConfEvent EVENT] {} BroadcastPluginConfEvent handled in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Errors validate(BroadcastPluginConfEvent message) {
        return null;
    }

}
