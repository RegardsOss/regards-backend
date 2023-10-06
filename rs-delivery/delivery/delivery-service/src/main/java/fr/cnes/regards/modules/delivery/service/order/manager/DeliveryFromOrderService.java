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
package fr.cnes.regards.modules.delivery.service.order.manager;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.dto.output.OrderResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to update a {@link DeliveryRequest} from {@link OrderResponseDtoEvent}.
 *
 * @author Stephane Cortine
 */
@Service
public class DeliveryFromOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryFromOrderService.class);

    private final static int BULK_CHUNK_SIZE = 1000;

    private final DeliveryRequestService deliveryRequestService;

    private final DeliveryFromOrderRetryService deliveryFromOrderRetryService;

    public DeliveryFromOrderService(DeliveryRequestService deliveryRequestService,
                                    DeliveryFromOrderRetryService deliveryFromOrderRetryService) {
        this.deliveryRequestService = deliveryRequestService;
        this.deliveryFromOrderRetryService = deliveryFromOrderRetryService;
    }

    /**
     * Update the given delivery request in database from the received order response event.
     */
    @MultitenantTransactional
    public List<DeliveryResponseDtoEvent> updateDeliveryRequestFromOrderResponseEvt(List<OrderResponseDtoEvent> events) {

        List<DeliveryResponseDtoEvent> deliveryResponseEvts = new ArrayList<>();

        for (int index = 0; index < events.size(); index += BULK_CHUNK_SIZE) {
            List<OrderResponseDtoEvent> partOrderResponseEvts = events.subList(index,
                                                                               Math.min(index + BULK_CHUNK_SIZE,
                                                                                        events.size()));
            // Find all delivery request in db by the list of correlation identifiers from order response events
            // (used toSet because the list can have several same correlation identifier; improve the request in db)
            List<DeliveryRequest> deliveryRequests = deliveryRequestService.findDeliveryRequestByCorrelationIds(
                partOrderResponseEvts.stream().map(OrderResponseDto::getCorrelationId).collect(Collectors.toSet()));

            deliveryRequests.forEach(deliveryRequest -> {
                List<OrderResponseDtoEvent> orderResponseEvts = partOrderResponseEvts.stream()
                                                                                     .filter(orderResponseEvt -> deliveryRequest.getCorrelationId()
                                                                                                                                .equals(
                                                                                                                                    orderResponseEvt.getCorrelationId()))
                                                                                     .toList();
                orderResponseEvts.forEach(orderResponseEvt -> {
                    DeliveryResponseDtoEvent deliveryResponseEvent = updateDeliveryRequestFromOrderResponseEvt(
                        orderResponseEvt,
                        deliveryRequest);
                    if (deliveryResponseEvent != null) {
                        deliveryResponseEvts.add(deliveryResponseEvent);
                    }
                });
            });
        }
        return deliveryResponseEvts;
    }

    /**
     * Update the given delivery request in database from the received order response event.
     *
     * @return the delivery response event if necessary (when status of order response event: GRANTED, DENIED)
     */
    private DeliveryResponseDtoEvent updateDeliveryRequestFromOrderResponseEvt(OrderResponseDtoEvent orderResponseEvent,
                                                                               DeliveryRequest deliveryRequest) {
        DeliveryResponseDtoEvent deliveryResponseEvt = null;
        // Check status of order response event
        switch (orderResponseEvent.getStatus()) {
            case GRANTED -> {
                deliveryResponseEvt = updateWithGrantedOrder(orderResponseEvent, deliveryRequest);
            }
            case SUBORDER_DONE, DONE -> {
                this.updateWithDoneOrderSuborder(orderResponseEvent, deliveryRequest);
            }
            case DENIED -> {
                deliveryResponseEvt = updateWithDeniedOrder(orderResponseEvent, deliveryRequest);
            }
            case FAILED -> {
                this.updateWithFailedOrder(orderResponseEvent, deliveryRequest);
            }
            default -> LOGGER.warn("Unknown state from order response event : {}", orderResponseEvent.getStatus());
        }
        return deliveryResponseEvt;
    }

    private DeliveryResponseDtoEvent updateWithGrantedOrder(OrderResponseDtoEvent orderResponseEvent,
                                                            DeliveryRequest deliveryRequest) {
        deliveryRequest.update(orderResponseEvent.getOrderId(),
                               orderResponseEvent.getTotalSubOrders(),
                               DeliveryRequestStatus.GRANTED,
                               null,
                               null);
        // Update delivery request in db
        deliveryRequestService.saveRequest(deliveryRequest);
        return new DeliveryResponseDtoEvent(deliveryRequest.getCorrelationId(),
                                            DeliveryRequestStatus.GRANTED,
                                            null,
                                            orderResponseEvent.getMessage(),
                                            null,
                                            null,
                                            deliveryRequest.getOriginRequestAppId(),
                                            deliveryRequest.getOriginRequestPriority());
    }

    private void updateWithDoneOrderSuborder(OrderResponseDtoEvent orderResponseEvent,
                                             DeliveryRequest deliveryRequest) {
        if (deliveryRequest.getStatus() != DeliveryRequestStatus.ERROR) {
            DeliveryRequestStatus deliveryRequestStatus = orderResponseEvent.hasErrors() ?
                DeliveryRequestStatus.ERROR :
                DeliveryRequestStatus.DONE;
            // Update deliver request in db
            deliveryFromOrderRetryService.saveRequestWithRetryWithOptimisticLock(deliveryRequest.getId(),
                                                                                 orderResponseEvent.getOrderId(),
                                                                                 orderResponseEvent.getTotalSubOrders(),
                                                                                 deliveryRequestStatus,
                                                                                 DeliveryErrorType.convert(
                                                                                     orderResponseEvent.getErrorCode()),
                                                                                 orderResponseEvent.getMessage());
        }
    }

    private DeliveryResponseDtoEvent updateWithDeniedOrder(OrderResponseDtoEvent orderResponseEvent,
                                                           DeliveryRequest deliveryRequest) {
        // Delete delivery request in db
        deliveryRequestService.deleteRequestById(deliveryRequest.getId());
        return new DeliveryResponseDtoEvent(deliveryRequest.getCorrelationId(),
                                            DeliveryRequestStatus.DENIED,
                                            DeliveryErrorType.convert(orderResponseEvent.getErrorCode()),
                                            orderResponseEvent.getMessage(),
                                            null,
                                            null,
                                            deliveryRequest.getOriginRequestAppId(),
                                            deliveryRequest.getOriginRequestPriority());
    }

    private void updateWithFailedOrder(OrderResponseDtoEvent orderResponseEvent, DeliveryRequest deliveryRequest) {
        // Update delivery request in db
        deliveryFromOrderRetryService.saveRequestWithRetryWithOptimisticLock(deliveryRequest.getId(),
                                                                             orderResponseEvent.getOrderId(),
                                                                             orderResponseEvent.getTotalSubOrders(),
                                                                             DeliveryRequestStatus.ERROR,
                                                                             DeliveryErrorType.INTERNAL_ERROR,
                                                                             orderResponseEvent.getMessage());
    }

}
