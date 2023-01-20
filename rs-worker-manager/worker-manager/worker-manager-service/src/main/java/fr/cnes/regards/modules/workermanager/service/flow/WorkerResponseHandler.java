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
package fr.cnes.regards.modules.workermanager.service.flow;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.workermanager.amqp.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.amqp.events.in.WorkerResponseEvent;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionsRequestsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * AMQP Batch handler to handle {@link WorkerResponseEvent}
 *
 * @author Sébastien Binda
 */
@Component
public class WorkerResponseHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<WorkerResponseEvent> {

    /**
     * Bulk size limit to handle messages
     */
    @Value("${regards.workermanager.worker.response.bulk.size:1000}")
    private int BULK_SIZE;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private RequestService service;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(WorkerResponseEvent.class,
                               this,
                               service.getWorkerResponseQueueName(),
                               service.getWorkerResponseQueueName(),
                               false);
    }

    @Override
    public Class<WorkerResponseEvent> getMType() {
        return WorkerResponseEvent.class;
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

    @Override
    public Errors validate(WorkerResponseEvent message) {
        return EventHeadersHelper.validateHeader(message);
    }

    @Override
    public void handleBatch(List<WorkerResponseEvent> messages) {
        long start = System.currentTimeMillis();
        LOGGER.info("Handling {} workers responses", messages.size());
        SessionsRequestsInfo info = service.handleWorkersResponses(messages);
        LOGGER.info("{} success requests, {} running requests and {} error requests handled in {}ms",
                    info.getRequests(RequestStatus.SUCCESS).size(),
                    info.getRequests(RequestStatus.RUNNING).size(),
                    info.getRequests(RequestStatus.ERROR).size(),
                    System.currentTimeMillis() - start);
    }
}
