/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.workermanager.dto.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerRequestDlqEvent;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerResponseEvent;
import fr.cnes.regards.modules.workermanager.dto.events.out.WorkerRequestEvent;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * AMQP Message handler to handle request sent to DLQ by workers
 *
 * @author Sébastien Binda
 */
@Component
public class WorkerRequestDlqHandler
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<WorkerRequestDlqEvent> {

    /**
     * Bulk size limit to handle messages
     */
    @Value("${regards.workermanager.worker.response.bulk.size:1000}")
    private int BULK_SIZE;

    private ISubscriber subscriber;

    private RequestService service;

    public WorkerRequestDlqHandler(ISubscriber subscriber, RequestService service) {
        this.service = service;
        this.subscriber = subscriber;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(WorkerRequestDlqEvent.class, this, service.getWorkerRequestDlqName(),
                               service.getWorkerRequestDlxName(), false);
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

    @Override
    public Class<WorkerRequestDlqEvent> getMType() {
        return WorkerRequestDlqEvent.class;
    }

    @Override
    public Errors validate(WorkerRequestDlqEvent message) {
        return EventHeadersHelper.validateHeader(message);
    }

    @Override
    public void handleBatch(List<WorkerRequestDlqEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.info("Handling {} workers request dlq", events.size());
        service.handleRequestErrors(events);
        LOGGER.info("{} dlq requests handled in {}ms", events.size(), System.currentTimeMillis() - start);
    }

}
