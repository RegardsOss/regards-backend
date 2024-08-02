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
package fr.cnes.regards.modules.delivery.service.order.zip.job.event;

import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryAndJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handle the ending of a {@link fr.cnes.regards.modules.delivery.service.order.zip.job.OrderDeliveryZipJob} with
 * its linked {@link DeliveryRequest}.
 *
 * @author Iliana Ghazali
 **/
@Service
public class OrderDeliveryZipJobEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDeliveryZipJobEventService.class);

    private final DeliveryAndJobService deliveryAndJobService;

    private final UpdateJobDeliveryRequestRetryService updateJobDeliveryRequestRetryService;

    public OrderDeliveryZipJobEventService(DeliveryAndJobService deliveryAndJobService,
                                           UpdateJobDeliveryRequestRetryService updateJobDeliveryRequestRetryService) {
        this.deliveryAndJobService = deliveryAndJobService;
        this.updateJobDeliveryRequestRetryService = updateJobDeliveryRequestRetryService;
    }

    /**
     * Handle the ending of an order delivery zip job and its related delivery request according to its final status.
     *
     * @param jobEvent zip delivery job to handle
     */
    public void handleFinishedOrderDeliveryZipJobEvent(JobEvent jobEvent) {
        deliveryAndJobService.findDeliveryRequestByJobId(jobEvent.getJobId()).ifPresent(deliveryRequest -> {
            // update linked delivery request if it is not already done
            updateRequestToErrorIfNecessary(jobEvent, deliveryRequest);
            // whatever the final job status, separate delivery request from job by deleting association in
            // delivery/job table
            deliveryAndJobService.deleteByDeliveryRequestId(deliveryRequest.getId());
            LOGGER.debug("Successfully handled event with job id '{}' and related delivery request with "
                         + "correlation id '{}'.", jobEvent.getJobId(), deliveryRequest.getCorrelationId());
        });

    }

    /**
     * Check if the request status was set to ERROR status.
     * If it is not the case, meaning the job failed or was aborted unexpectedly, the request will be updated to an
     * ERROR status.
     *
     * @param jobEvent        metadata about the job event received
     * @param deliveryRequest delivery request linked to that job
     */
    public void updateRequestToErrorIfNecessary(JobEvent jobEvent, DeliveryRequest deliveryRequest) {
        JobEventType jobStatus = jobEvent.getJobEventType();
        DeliveryRequestStatus deliveryRequestStatus = deliveryRequest.getStatus();
        if ((jobStatus.equals(JobEventType.FAILED) || jobStatus.equals(JobEventType.ABORTED))
            && !deliveryRequestStatus.equals(DeliveryRequestStatus.ERROR)) {
            LOGGER.warn("Unexpected delivery request status (correlation id '{}'), which is not consistent with job "
                        + "status '{}'. Expected request status to be '{}' but was '{}'. The status will be set to error.",
                        deliveryRequest.getCorrelationId(),
                        jobStatus,
                        DeliveryRequestStatus.ERROR,
                        deliveryRequestStatus);
            updateJobDeliveryRequestRetryService.updateRequestToErrorWithRetry(jobEvent.getJobId(),
                                                                               deliveryRequest.getId());
        }
    }

}
