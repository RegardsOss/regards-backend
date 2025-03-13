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
package fr.cnes.regards.modules.crawler.service.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.crawler.domain.EntityEventRequest;
import fr.cnes.regards.modules.crawler.service.service.EntityIndexerService;
import fr.cnes.regards.modules.dam.domain.entities.event.DatasetEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.Arrays;
import java.util.List;

/**
 * Handler for {@link DatasetEvent}
 * This will save a {@link EntityEventRequest} in database that will be processed later by the {@link fr.cnes.regards.modules.crawler.service.scheduler.SaveEntityIntoEsScheduler}
 *
 * @author Thibaud Michaudel
 **/
@Component
public class DatasetEventHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<DatasetEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetEventHandler.class);

    private final ISubscriber subscriber;

    private final EntityIndexerService entityIndexerService;

    @Value("${regards.dam.request.bulk.size:100}")
    private int bulkSize;

    public DatasetEventHandler(ISubscriber subscriber, EntityIndexerService entityIndexerService) {
        this.subscriber = subscriber;
        this.entityIndexerService = entityIndexerService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DatasetEvent.class, this);
    }

    @Override
    public void handleBatch(List<DatasetEvent> messages) {
        LOGGER.debug("[DatasetEvent HANDLER {} messages received ] ", messages.size());
        long start = System.currentTimeMillis();
        // Save only one request for each urn
        List<EntityEventRequest> entityRequests = messages.stream()
                                                          .flatMap(event -> Arrays.stream(event.getIpIds()).map(urn ->
                                                                   new EntityEventRequest(urn.toString(), event.getUserToNotify(),
                                                                                               event.getRoleToNotify())))
                                                          .distinct()
                                                          .toList();

        entityIndexerService.saveEntityUpdateRequests(entityRequests);

        LOGGER.info("[DatasetEvent HANDLER] {} messages handled in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Errors validate(DatasetEvent message) {
        return null;
    }

    @Override
    public boolean isRetryEnabled() {
        return true;
    }
}
