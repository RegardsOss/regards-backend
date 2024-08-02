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

import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.service.order.zip.job.OrderDeliveryZipJobProgressManager;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Update a {@link DeliveryRequest} linked to a failed {@link fr.cnes.regards.framework.modules.jobs.domain.JobInfo}
 * with a concurrency management.
 *
 * @author Iliana Ghazali
 **/
@Service
public class UpdateJobDeliveryRequestRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateJobDeliveryRequestRetryService.class);

    private final OrderDeliveryZipJobProgressManager jobProgressManager;

    private final DeliveryRequestService deliveryRequestService;

    public UpdateJobDeliveryRequestRetryService(OrderDeliveryZipJobProgressManager jobProgressManager,
                                                DeliveryRequestService deliveryRequestService) {
        this.jobProgressManager = jobProgressManager;
        this.deliveryRequestService = deliveryRequestService;
    }

    /**
     * Put a {@link DeliveryRequest} to an error state with retry attempts in case another process has already
     * locked the entity. Locking failure is triggered by a {@link OptimisticLockingFailureException}.
     * By default, the number of retries is high because the request must absolutely be updated to an error state.
     *
     * @param jobId             job id linked to the delivery request to update
     * @param deliveryRequestId {@link DeliveryRequest} to update to error status
     */
    @Retryable(value = { OptimisticLockingFailureException.class },
               maxAttemptsExpression = "${regards.delivery.request.update.error.retries:50}",
               backoff = @Backoff(delay = 1000))
    public void updateRequestToErrorWithRetry(UUID jobId, long deliveryRequestId) {
        DeliveryRequest deliveryRequest = deliveryRequestService.findDeliveryRequest(deliveryRequestId)
                                                                .orElseThrow(() -> new EntityNotFoundException(String.format(
                                                                    "Delivery request with id '%d' was not found and therefore cannot be updated to ERROR "
                                                                    + "status",
                                                                    deliveryRequestId)));
        jobProgressManager.handleDeliveryError(deliveryRequest,
                                               new DeliveryOrderException(String.format(
                                                   "Delivery job with id '%s' did not handle properly delivery request with "
                                                   + "correlation id '%s'. An unexpected error occurred as the job ended in "
                                                   + "FAILED or ABORTED status and the request is not in error state. "
                                                   + "The request will therefore be updated to an ERROR status.",
                                                   jobId,
                                                   deliveryRequest.getCorrelationId())));
    }

    /**
     * Recovery method if {@link DeliveryRequest} could not be updated to an error status. Basically the error is
     * just rethrown, this case should never happen.
     */
    @Recover
    public void recoverUpdateRequestFailure(OptimisticLockingFailureException exception,
                                            UUID jobId,
                                            long deliveryRequestId) {
        LOGGER.error("Failed to update delivery request with id '{}' and job id '{}' to ERROR status.",
                     deliveryRequestId,
                     jobId,
                     exception);
        throw exception;
    }

}
