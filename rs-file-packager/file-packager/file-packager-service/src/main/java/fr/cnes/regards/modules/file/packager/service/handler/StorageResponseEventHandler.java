/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.file.packager.service.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler for {@link StorageResponseEvent} that are received when a package has been stored by the file access
 * microservice. The package need to be updated in database and an event is sent to the file catalog for each file in
 * this package.
 *
 * @author Thibaud Michaudel
 */
@Component
public class StorageResponseEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<StorageResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageResponseEventHandler.class);

    private final ISubscriber subscriber;

    private final FilePackagerService filePackagerService;

    @Value("${regards.file.packager.storage.response.bulk.size:50}")
    private int bulkSize;

    public StorageResponseEventHandler(ISubscriber subscriber, FilePackagerService filePackagerService) {
        this.subscriber = subscriber;
        this.filePackagerService = filePackagerService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(StorageResponseEvent.class, this);
    }

    @Override
    public void handleBatch(List<StorageResponseEvent> messages) {
        LOGGER.debug("[StorageResponseEvent HANDLER] {} package storage events received", messages.size());
        long start = System.currentTimeMillis();
        filePackagerService.updatePackageAfterCompletion(messages);

        LOGGER.info("[StorageResponseEvent HANDLER] {} package storage events handled in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Errors validate(StorageResponseEvent message) {
        return null;
    }

    @Override
    public boolean isRetryEnabled() {
        return true;
    }

}