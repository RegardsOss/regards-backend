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
package fr.cnes.regards.modules.delivery.service.order.manager;

import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Service to update a {@link DeliveryRequest} from {@link OrderResponseDtoEvent} with a concurrency management.
 *
 * @author Stephane Cortine
 */
@Service
public class DeliveryFromOrderRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryFromOrderRetryService.class);

    private final DeliveryRequestService deliveryRequestService;

    public DeliveryFromOrderRetryService(DeliveryRequestService deliveryRequestService) {
        this.deliveryRequestService = deliveryRequestService;
    }

    @Retryable(value = { OptimisticLockingFailureException.class },
               maxAttemptsExpression = "${regards.delivery.request.update.error.retries:50}",
               backoff = @Backoff(delay = 1000))
    public void saveRequestWithRetryWithOptimisticLock(Long deliveryRequestId,
                                                       Long orderId,
                                                       Integer totalSubOrders,
                                                       DeliveryRequestStatus status,
                                                       DeliveryErrorType errorType,
                                                       String errorCause) throws OptimisticLockingFailureException {
        deliveryRequestService.updateRequest(deliveryRequestId, orderId, totalSubOrders, status, errorType, errorCause);
    }

    /**
     * Retry recover method called by spring framework if all retries fails.
     */
    @Recover
    public void saveRequestWithRetryWithOptimisticLockRecover(OptimisticLockingFailureException exception,
                                                              Long deliveryRequestId,
                                                              Long orderId,
                                                              Integer totalSubOrders,
                                                              DeliveryRequestStatus status,
                                                              DeliveryErrorType errorType,
                                                              String errorCause) {
        LOGGER.error("Too many retry for optimistic lock on delivery requests. Maybe optimistic lock is not the right"
                     + " solution here.");
        throw exception;
    }
}
