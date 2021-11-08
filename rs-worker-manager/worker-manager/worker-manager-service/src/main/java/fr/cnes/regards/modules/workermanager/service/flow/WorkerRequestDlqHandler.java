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
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<WorkerRequestEvent> {

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
        subscriber.subscribeTo(WorkerRequestEvent.class, this, service.getWorkerRequestDlqName(),
                               service.getWorkerRequestDlqName(), false);
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

    @Override
    public Errors validate(WorkerRequestEvent message) {
        return EventHeadersHelper.validateHeader(message);
    }

    @Override
    public void handleBatch(List<WorkerRequestEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.info("Handling {} workers request dlq", events.size());
        service.handleRequestErrors(events);
        LOGGER.info("{} dlq requests handled in {}ms", events.size(), System.currentTimeMillis() - start);
    }

}
