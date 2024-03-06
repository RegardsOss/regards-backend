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
package fr.cnes.regards.modules.storage.service.file.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler to handle {@link FilesReferenceEvent} AMQP messages.<br>
 * Those messages are sent to create new file reference.<br>
 * Each message is saved in a concurrent list to handle availability request by bulk.
 *
 * @author Sébastien Binda
 */
@Component
public class FilesStorageRequestEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FilesStorageRequestEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesStorageRequestEventHandler.class);

    /**
     * Bulk size limit to handle messages
     * NOTE : Over 100 performance are decreased
     */
    @Value("${regards.storage.store.items.bulk.size:10}")
    private int BULK_SIZE;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private FileStorageRequestService fileStorageReqService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FilesStorageRequestEvent.class, this);
    }

    @Override
    public void handleBatch(List<FilesStorageRequestEvent> messages) {
        LOGGER.debug("[FILES STORAGE REQUEST EVENT HANDLER] Bulk saving {} FilesStorageRequestEvent...",
                     messages.size());
        long start = System.currentTimeMillis();
        fileStorageReqService.store(messages);
        LOGGER.info("[FILES STORAGE REQUEST EVENT HANDLER] {} FilesStorageRequestEvent handled in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public Errors validate(FilesStorageRequestEvent message) {
        return null;
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

}
