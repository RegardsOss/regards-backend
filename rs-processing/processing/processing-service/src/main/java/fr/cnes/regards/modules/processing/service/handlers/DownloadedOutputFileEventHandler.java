/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.service.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.domain.events.DownloadedOutputFilesEvent;
import fr.cnes.regards.modules.processing.domain.service.IOutputFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * This class defines the event handler used for downloaded output files.
 *
 * @author gandrieu
 */
@Component
public class DownloadedOutputFileEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<DownloadedOutputFilesEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadedOutputFileEventHandler.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ISubscriber subscriber;

    private final IOutputFileService outFileService;

    public DownloadedOutputFileEventHandler(IOutputFileService outFileService,
            IRuntimeTenantResolver runtimeTenantResolver, ISubscriber subscriber) {
        this.outFileService = outFileService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DownloadedOutputFilesEvent.class, this);
    }

    @Override
    public void handle(String tenant, DownloadedOutputFilesEvent message) {
        runtimeTenantResolver.forceTenant(tenant); // Needed in order to publish events

        LOGGER.info("Downloaded outputfile event received: {}", message);

        outFileService.markDownloaded(message.getOutputFileUrls())
                .subscribe(exec -> LOGGER.info("Output files marked as downloaded: {}", message),
                           err -> LOGGER.error(err.getMessage(), err));
    }
}
