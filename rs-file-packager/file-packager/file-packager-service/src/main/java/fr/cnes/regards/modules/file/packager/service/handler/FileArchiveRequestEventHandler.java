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
package fr.cnes.regards.modules.file.packager.service.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileArchiveRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler for {@link FileArchiveRequestEvent} sent from file-catalog to file-packager in order to request the packaging of a small file.
 * For each message, the handler will save one {@link FileInBuildingPackage} in database that will be processed by
 * the scheduler WIP
 *
 * @author Thibaud Michaudel
 **/
@Component
public class FileArchiveRequestEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FileArchiveRequestEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileArchiveRequestEventHandler.class);

    private final ISubscriber subscriber;

    private final FilePackagerService filePackagerService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    @Value("${regards.file.packager.files.archive.request.bulk.size:100}")
    private int bulkSize;

    public FileArchiveRequestEventHandler(ISubscriber subscriber,
                                          FilePackagerService filePackagerService,
                                          IRuntimeTenantResolver runtimeTenantResolver) {
        this.subscriber = subscriber;
        this.filePackagerService = filePackagerService;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FileArchiveRequestEvent.class, this);
    }

    @Override
    public void handleBatch(List<FileArchiveRequestEvent> messages) {
        LOGGER.debug("[FileArchiveRequestEvent HANDLER] {} file archive requests received", messages.size());
        long start = System.currentTimeMillis();
        
        filePackagerService.createNewFilesInBuildingPackage(messages);

        LOGGER.info("[FileArchiveRequestEvent EVENT HANDLER] {} file archive requests handled in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Errors validate(FileArchiveRequestEvent message) {
        return null;
    }

}
