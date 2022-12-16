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
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.order.amqp.output.OrderRequestResponseDtoEvent;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.service.BasketService;
import fr.cnes.regards.modules.order.service.IOrderService;
import fr.cnes.regards.modules.order.service.settings.OrderSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.OffsetDateTime;
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

    /**
     * Constants
     */
    public static final String SEARCH_ENGINE_TYPE = "legacy";

    private static final String DEFAULT_ACCESS_ROLE = DefaultRole.EXPLOIT.toString();

    public static final String ERROR_RESPONSE_FORMAT = "%s: %s"; // "Exception: error cause"

    /**
     * Services
     */
    private final BasketService basketService;

    private final IOrderService orderService;

    private final OrderSettingsService orderSettings;

    private final IPublisher publisher;

    public OrderRequestService(BasketService basketService,
                               IOrderService orderService,
                               OrderSettingsService orderSettings,
                               IPublisher publisher) {
        this.basketService = basketService;
        this.orderService = orderService;
        this.orderSettings = orderSettings;
        this.publisher = publisher;
    }

    /**
     * Generic method to create an {@link Order} from {@link OrderRequestDto}s.
     * <br/>
     * A {@link Basket} is built from requests and used to create an {@link Order}.
     * {@link OrderRequestResponseDtoEvent}s are then published with the creation status.
     */
    public void createOrderFromRequests(List<OrderRequestDto> orderRequests) {
        List<OrderRequestResponseDtoEvent> responses = new ArrayList<>();
        for (OrderRequestDto orderRequest : orderRequests) {
            try {
                Basket basket = createBasketFromRequests(orderRequest);
                orderService.createOrder(basket,
                                         "Generated order " + OffsetDateTimeAdapter.format(OffsetDateTime.now()),
                                         null,
                                         orderSettings.getAppSubOrderDuration(),
                                         orderRequest.getUser(),
                                         orderRequest.getCorrelationId());
                responses.add(buildSuccessResponse(orderRequest));
            } catch (EmptySelectionException | TooManyItemsSelectedInBasketException | EntityInvalidException e) {
                LOGGER.error("Request with correlationId {} has failed. Cause:", orderRequest.getCorrelationId(), e);
                responses.add(buildErrorResponse(orderRequest,
                                                 String.format(ERROR_RESPONSE_FORMAT,
                                                               e.getClass().getSimpleName(),
                                                               e.getMessage())));
            }
        }
        publisher.publish(responses);
    }

    private Basket createBasketFromRequests(OrderRequestDto orderRequest)
        throws TooManyItemsSelectedInBasketException, EmptySelectionException {
        Basket basket = basketService.findOrCreate(orderRequest.getCorrelationId());
        for (String query : orderRequest.getQueries()) {
            //FIXME: user and role should not be given directly for security reasons.
            // Instead a token must be used, this feature will be released later.
            basket = basketService.addSelection(basket.getId(),
                                                createBasketSelectionRequest(query),
                                                orderRequest.getUser(),
                                                DEFAULT_ACCESS_ROLE);
        }
        return basket;
    }

    private BasketSelectionRequest createBasketSelectionRequest(String query) {
        BasketSelectionRequest basketSelectionRequest = new BasketSelectionRequest();
        basketSelectionRequest.setEngineType(SEARCH_ENGINE_TYPE);
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.add("q", query);
        basketSelectionRequest.setSearchParameters(searchParameters);
        return basketSelectionRequest;
    }

    private OrderRequestResponseDtoEvent buildErrorResponse(OrderRequestDto orderRequest, String cause) {
        return new OrderRequestResponseDtoEvent(orderRequest.getCorrelationId(), OrderRequestStatus.FAILED, cause);
    }

    private OrderRequestResponseDtoEvent buildSuccessResponse(OrderRequestDto orderRequest) {
        return new OrderRequestResponseDtoEvent(orderRequest.getCorrelationId(), OrderRequestStatus.GRANTED, null);
    }

}
