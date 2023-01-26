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
package fr.cnes.regards.modules.order.service.request;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.order.amqp.output.OrderRequestResponseDtoEvent;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.output.OrderRequestResponseDto;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.exception.OrderRequestServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * This service creates orders through the execution of a {@link CreateOrderJob}.
 * {@link OrderRequestResponseDtoEvent} are sent to indicate the status of the creation.
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class OrderRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderRequestService.class);

    public static final String ERROR_RESPONSE_FORMAT = "%s: \"%s\""; // Exception: "error cause"

    private final AutoOrderCompletionService autoOrderCompletionService;

    private final IPublisher publisher;

    public OrderRequestService(AutoOrderCompletionService autoOrderCompletionService, IPublisher publisher) {
        this.autoOrderCompletionService = autoOrderCompletionService;
        this.publisher = publisher;
    }

    /**
     * see {@link this#createOrderFromRequests(List, String)}
     */
    public OrderRequestResponseDto createOrderFromRequest(OrderRequestDto orderRequest, String role) {
        return createOrderFromRequests(List.of(orderRequest), role).get(0);
    }

    /**
     * Generic method to create an {@link Order} from {@link OrderRequestDto}s.
     */
    public List<OrderRequestResponseDto> createOrderFromRequests(List<OrderRequestDto> orderRequests, String role) {
        List<OrderRequestResponseDto> responses = new ArrayList<>();
        for (OrderRequestDto orderRequest : orderRequests) {
            try {
                Order createdOrder = autoOrderCompletionService.generateOrder(orderRequest, role);
                responses.add(buildSuccessResponse(orderRequest, createdOrder.getId()));
            } catch (OrderRequestServiceException e) {
                LOGGER.error("Request with correlationId {} has failed. Cause:",
                             orderRequest.getCorrelationId(),
                             e);
                responses.add(buildErrorResponse(orderRequest,
                                                 String.format(ERROR_RESPONSE_FORMAT,
                                                               e.getClass().getSimpleName(),
                                                               e.getMessage())));
            }
        }
        return responses;
    }

    private OrderRequestResponseDto buildErrorResponse(OrderRequestDto orderRequest, String cause) {
        return new OrderRequestResponseDto(OrderRequestStatus.FAILED,
                                           null,
                                           orderRequest.getCorrelationId(),
                                           cause,
                                           null);
    }

    private OrderRequestResponseDto buildSuccessResponse(OrderRequestDto orderRequest, Long createdOrderId) {
        return new OrderRequestResponseDto(OrderRequestStatus.GRANTED,
                                           createdOrderId,
                                           orderRequest.getCorrelationId(),
                                           null,
                                           null);
    }

    public void publishResponses(List<OrderRequestResponseDto> responses) {
        publisher.publish(responses.stream().map(OrderRequestResponseDtoEvent::new).toList());
    }

}
