/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.order.zip.job.event;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.modules.delivery.service.order.zip.job.OrderDeliveryZipJob;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Handle {@link fr.cnes.regards.modules.delivery.service.order.zip.job.OrderDeliveryZipJob} {@link JobEvent}s in
 * finished states.
 *
 * @author Iliana Ghazali
 **/
@Component
public class OrderDeliveryZipJobEventHandler implements ApplicationListener<ApplicationReadyEvent>, IHandler<JobEvent> {

    private final ISubscriber subscriber;

    private final OrderDeliveryZipJobEventService jobEventService;

    public OrderDeliveryZipJobEventHandler(ISubscriber subscriber, OrderDeliveryZipJobEventService jobEventService) {
        this.subscriber = subscriber;
        this.jobEventService = jobEventService;
    }

    @Override
    public void handle(String tenant, JobEvent jobEvent) {
        UUID jobId = jobEvent.getJobId();
        LOGGER.debug("[ORDER DELIVERY ZIP JOB EVENT HANDLER] Handling event with job id '{}'...", jobId);
        long start = System.currentTimeMillis();
        if (OrderDeliveryZipJob.class.getName().equals(jobEvent.getJobClassName()) && jobEvent.getJobEventType()
                                                                                              .isFinalState()) {
            jobEventService.handleFinishedOrderDeliveryZipJobEvent(jobEvent);
        }
        LOGGER.debug("[ORDER DELIVERY ZIP JOB EVENT HANDLER] event with job id '{}' handled in {} ms",
                     jobId,
                     System.currentTimeMillis() - start);
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

}
