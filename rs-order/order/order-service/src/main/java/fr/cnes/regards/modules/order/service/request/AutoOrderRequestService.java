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
package fr.cnes.regards.modules.order.service.request;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.order.domain.exception.ExceededBasketSizeException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.dto.OrderErrorType;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.dto.output.OrderResponseDto;
import fr.cnes.regards.modules.order.exception.AutoOrderException;
import fr.cnes.regards.modules.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This service creates orders through the execution of a {@link CreateOrderJob}.
 * {@link OrderResponseDtoEvent} are sent to indicate the status of the creation.
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class AutoOrderRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoOrderRequestService.class);

    private final AutoOrderCompletionService autoOrderCompletionService;

    public AutoOrderRequestService(AutoOrderCompletionService autoOrderCompletionService) {
        this.autoOrderCompletionService = autoOrderCompletionService;
    }

    /**
     * see {@link this#createOrderFromRequests(List, String, boolean)}
     */
    public OrderResponseDto createOrderFromRequest(OrderRequestDto orderRequest, String role) {
        return createOrderFromRequests(List.of(orderRequest), role, false).get(0);
    }

    /**
     * see {@link this#createOrderFromRequests(List, String, boolean)}
     */
    public List<OrderResponseDto> createOrderFromRequestsWithSizeLimit(List<OrderRequestDto> orderRequests,
                                                                       String role) {
        return createOrderFromRequests(orderRequests, role, true);
    }

    /**
     * Generic method to create an {@link Order} from {@link OrderRequestDto}s.
     *
     * @param orderRequests  metadata to create an order
     * @param role           user role to check data access permissions
     * @param checkSizeLimit if basket size should be verified before launching the order
     * @return order creation status
     */
    private List<OrderResponseDto> createOrderFromRequests(List<OrderRequestDto> orderRequests,
                                                           String role,
                                                           boolean checkSizeLimit) {
        List<OrderResponseDto> responses = new ArrayList<>();
        LOGGER.debug("Creating automatically order response from {} request(s).", orderRequests.size());
        long start = System.currentTimeMillis();
        for (OrderRequestDto orderRequestDto : orderRequests) {
            initCorrelationId(orderRequestDto);
            try {
                Order createdOrder = autoOrderCompletionService.generateOrder(orderRequestDto, role, checkSizeLimit);
                responses.add(OrderResponseDto.buildSuccessResponse(orderRequestDto,
                                                                    createdOrder.getId(),
                                                                    OrderRequestStatus.GRANTED));
                LOGGER.debug("Successfully created order [id={}] in {}ms from {} request(s).",
                             createdOrder.getId(),
                             System.currentTimeMillis() - start,
                             orderRequests.size());

            } catch (AutoOrderException e) {
                LOGGER.error("Order request with correlationId {} has failed.", orderRequestDto.getCorrelationId(), e);
                responses.add(manageErrorOrderResponse(orderRequestDto, e));
            }
        }
        return responses;
    }

    private OrderResponseDto manageErrorOrderResponse(OrderRequestDto orderRequest, Exception exception) {
        OrderRequestStatus orderRequestStatus = OrderRequestStatus.FAILED;
        OrderErrorType errorType = OrderErrorType.INTERNAL_ERROR;

        if (exception.getCause() != null) {
            if (exception.getCause().getClass() == ExceededBasketSizeException.class
                || exception.getCause().getClass() == TooManyItemsSelectedInBasketException.class) {
                orderRequestStatus = OrderRequestStatus.DENIED;
                errorType = OrderErrorType.ORDER_LIMIT_REACHED;
            }
            if (exception.getCause().getClass() == EmptySelectionException.class) {
                orderRequestStatus = OrderRequestStatus.FAILED;
                errorType = OrderErrorType.EMPTY_ORDER;
            }
        }

        return OrderResponseDto.buildErrorResponse(orderRequest, exception.getMessage(), orderRequestStatus, errorType);
    }

    private void initCorrelationId(OrderRequestDto orderRequestDto) {
        if (orderRequestDto.getCorrelationId() == null) {
            orderRequestDto.setCorrelationId(String.format(OrderService.DEFAULT_CORRELATION_ID_FORMAT,
                                                           UUID.randomUUID()));
        }
    }

}
