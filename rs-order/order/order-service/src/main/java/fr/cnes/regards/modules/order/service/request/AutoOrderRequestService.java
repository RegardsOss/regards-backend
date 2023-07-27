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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.exception.CatalogSearchException;
import fr.cnes.regards.modules.order.domain.exception.ExceededBasketSizeException;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.dto.output.OrderResponseDto;
import fr.cnes.regards.modules.order.exception.AutoOrderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        for (OrderRequestDto orderRequest : orderRequests) {
            try {
                Order createdOrder = autoOrderCompletionService.generateOrder(orderRequest, role, checkSizeLimit);
                responses.add(OrderResponseDto.buildSuccessResponse(orderRequest,
                                                                    createdOrder.getId(),
                                                                    OrderRequestStatus.GRANTED));
                LOGGER.debug("Successfully created order with id '{}' in {}ms from {} request(s).",
                             createdOrder.getId(),
                             System.currentTimeMillis() - start,
                             orderRequests.size());

            } catch (AutoOrderException | CatalogSearchException e) {
                LOGGER.error("Order request with correlationId {} has failed. Cause:",
                             orderRequest.getCorrelationId(),
                             e);
                responses.add(OrderResponseDto.buildErrorResponse(orderRequest, e, getErrorStatus(e)));
            }
        }
        return responses;
    }

    private OrderRequestStatus getErrorStatus(Exception e) {
        if (e.getCause() != null && e.getCause().getClass() == ExceededBasketSizeException.class) {
            return OrderRequestStatus.DENIED;
        } else {
            return OrderRequestStatus.FAILED;
        }
    }
}
