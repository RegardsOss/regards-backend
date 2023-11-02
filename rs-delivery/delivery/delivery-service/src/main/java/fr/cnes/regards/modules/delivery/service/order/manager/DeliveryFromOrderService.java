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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.client.amqp.IAutoOrderResponseClient;
import fr.cnes.regards.modules.order.dto.output.OrderResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Service to update a {@link DeliveryRequest} from {@link OrderResponseDtoEvent}.
 *
 * @author Stephane Cortine
 */
@Service
public class DeliveryFromOrderService implements IAutoOrderResponseClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryFromOrderService.class);

    private static final int BULK_CHUNK_SIZE = 1000;

    private final DeliveryRequestService deliveryRequestService;

    private final DeliveryFromOrderRetryService deliveryFromOrderRetryService;

    private final IPublisher publisher;

    public DeliveryFromOrderService(DeliveryRequestService deliveryRequestService,
                                    DeliveryFromOrderRetryService deliveryFromOrderRetryService,
                                    IPublisher publisher) {
        this.deliveryRequestService = deliveryRequestService;
        this.deliveryFromOrderRetryService = deliveryFromOrderRetryService;
        this.publisher = publisher;
    }

    @Override
    @MultitenantTransactional
    public void onOrderDenied(List<OrderResponseDtoEvent> groupedEvents) {
        List<DeliveryResponseDtoEvent> deliveryResponseEvents = updateDeliveryRequestFromOrderResponseEvt(groupedEvents,
                                                                                                          this::updateWithDeniedOrder);
        publisher.publish(deliveryResponseEvents);
    }

    @Override
    @MultitenantTransactional
    public void onOrderGranted(List<OrderResponseDtoEvent> groupedEvents) {
        updateDeliveryRequestFromOrderResponseEvt(groupedEvents, this::updateWithGrantedOrder);
    }

    @Override
    @MultitenantTransactional
    public void onSubOrderDone(List<OrderResponseDtoEvent> groupedEvents) {
        updateDeliveryRequestFromOrderResponseEvt(groupedEvents, this::updateWithSuborderDone);
    }

    @Override
    @MultitenantTransactional
    public void onOrderDone(List<OrderResponseDtoEvent> groupedEvents) {
        updateDeliveryRequestFromOrderResponseEvt(groupedEvents, this::updateWithOrderDone);
    }

    @Override
    @MultitenantTransactional
    public void onOrderFailed(List<OrderResponseDtoEvent> groupedEvents) {
        updateDeliveryRequestFromOrderResponseEvt(groupedEvents, this::updateWithFailedOrder);
    }

    /**
     * Update the given delivery request in database from the received order response event.
     *
     * @return the delivery response events if necessary (when status of order response event: GRANTED, DENIED)
     */
    public List<DeliveryResponseDtoEvent> updateDeliveryRequestFromOrderResponseEvt(List<OrderResponseDtoEvent> events,
                                                                                    BiFunction<OrderResponseDtoEvent, DeliveryRequest, DeliveryResponseDtoEvent> updateDeliveryRequestMethod) {

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
                    DeliveryResponseDtoEvent deliveryResponseEvent = updateDeliveryRequestMethod.apply(orderResponseEvt,
                                                                                                       deliveryRequest);
                    if (deliveryResponseEvent != null) {
                        deliveryResponseEvts.add(deliveryResponseEvent);
                    }
                });
            });
        }
        return deliveryResponseEvts;
    }

    private DeliveryResponseDtoEvent updateWithOrderDone(OrderResponseDtoEvent orderResponseEvent,
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
                                                                                     orderResponseEvent.getErrorType()),
                                                                                 orderResponseEvent.getMessage());
        }
        return null;
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

    private DeliveryResponseDtoEvent updateWithSuborderDone(OrderResponseDtoEvent orderResponseEvent,
                                                            DeliveryRequest deliveryRequest) {
        // update delivery request when suborder is done
        // do nothing if request is already in error
        if (deliveryRequest.getStatus() != DeliveryRequestStatus.ERROR) {
            DeliveryRequestStatus deliveryRequestStatus;
            DeliveryErrorType errorType;
            String message;
            // Reject delivery if more than one suborder is detected
            if (orderResponseEvent.getTotalSubOrders() != null && orderResponseEvent.getTotalSubOrders() > 1) {
                deliveryRequestStatus = DeliveryRequestStatus.ERROR;
                errorType = DeliveryErrorType.TOO_MANY_SUBORDERS;
                message = String.format(
                    "Cannot deliver request %s : this implementation does not support more than 1 sub-order",
                    deliveryRequest.getCorrelationId());
            } else {
                deliveryRequestStatus = orderResponseEvent.hasErrors() ? DeliveryRequestStatus.ERROR : null;
                errorType = DeliveryErrorType.convert(orderResponseEvent.getErrorType());
                message = orderResponseEvent.getMessage();
            }
            // Update deliver request in db
            // do nothing if no error
            if (deliveryRequestStatus == DeliveryRequestStatus.ERROR) {
                deliveryFromOrderRetryService.saveRequestWithRetryWithOptimisticLock(deliveryRequest.getId(),
                                                                                     orderResponseEvent.getOrderId(),
                                                                                     orderResponseEvent.getTotalSubOrders(),
                                                                                     deliveryRequestStatus,
                                                                                     errorType,
                                                                                     message);
            }
        }
        return null;
    }

    private DeliveryResponseDtoEvent updateWithDeniedOrder(OrderResponseDtoEvent orderResponseEvent,
                                                           DeliveryRequest deliveryRequest) {
        // Delete delivery request in db
        deliveryRequestService.deleteRequestById(deliveryRequest.getId());
        return new DeliveryResponseDtoEvent(deliveryRequest.getCorrelationId(),
                                            DeliveryRequestStatus.ERROR,
                                            DeliveryErrorType.convert(orderResponseEvent.getErrorType()),
                                            orderResponseEvent.getMessage(),
                                            null,
                                            null,
                                            deliveryRequest.getOriginRequestAppId(),
                                            deliveryRequest.getOriginRequestPriority());
    }

    private DeliveryResponseDtoEvent updateWithFailedOrder(OrderResponseDtoEvent orderResponseEvent,
                                                           DeliveryRequest deliveryRequest) {
        if (deliveryRequest.getStatus() == DeliveryRequestStatus.ERROR) {
            LOGGER.warn("An order event in error was received while the delivery request with correlation id '{}' is "
                        + "already in error status. This event will be ignored. Order event : (type: '{}', cause: '{}').",
                        deliveryRequest.getCorrelationId(),
                        orderResponseEvent.getErrorType(),
                        orderResponseEvent.getMessage());
        } else {
            // Update delivery request in db
            deliveryFromOrderRetryService.saveRequestWithRetryWithOptimisticLock(deliveryRequest.getId(),
                                                                                 orderResponseEvent.getOrderId(),
                                                                                 orderResponseEvent.getTotalSubOrders(),
                                                                                 DeliveryRequestStatus.ERROR,
                                                                                 DeliveryErrorType.convert(
                                                                                     orderResponseEvent.getErrorType()),
                                                                                 orderResponseEvent.getMessage());
        }
        return null;
    }

}
