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
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.dto.output.OrderResponseDto;
import fr.cnes.regards.modules.order.exception.OrderRequestServiceException;
import fr.cnes.regards.modules.order.service.BasketService;
import fr.cnes.regards.modules.order.service.IOrderService;
import fr.cnes.regards.modules.order.service.settings.OrderSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static fr.cnes.regards.modules.order.service.request.OrderRequestTestUtils.checkOrderRequestResponses;
import static fr.cnes.regards.modules.order.service.request.OrderRequestTestUtils.createValidOrderRequests;
import static org.mockito.ArgumentMatchers.*;

/**
 * Test for {@link OrderRequestService} </br>
 * The purpose of this test is to verify that {@link OrderResponseDtoEvent} are correctly sent following the
 * creation of an order in success or failure.
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class OrderRequestServiceTest {

    @Mock
    private BasketService basketService;

    @Mock
    private IOrderService orderService;

    @Mock
    private OrderSettingsService orderSettings;

    @Spy
    private IPublisher publisher;

    // Class under test
    private OrderRequestService orderRequestService;

    @Before
    public void init() throws EntityInvalidException, TooManyItemsSelectedInBasketException, EmptySelectionException {
        AutoOrderCompletionService autoOrderCompletionService = new AutoOrderCompletionService(basketService,
                                                                                               orderService,
                                                                                               orderSettings);
        this.orderRequestService = new OrderRequestService(autoOrderCompletionService, publisher);
        Mockito.when(basketService.findOrCreate(any())).thenAnswer(answer -> new Basket(answer.getArgument(0)));
        Mockito.when(basketService.addSelection(any(), any(), anyString(), anyString()))
               .thenAnswer(answer -> new Basket(answer.getArgument(0)));
        Mockito.when(orderService.createOrder(any(), any(), any(), anyInt(), any(), any())).thenAnswer(answer -> {
            Order order = new Order();
            order.setId(Long.valueOf(answer.getArgument(5)));
            return order;
        });
    }

    @Test
    @Purpose("Test if success responses are sent following a valid order creation.")
    public void send_orderRequests_success() {
        // GIVEN
        int nbReq = 3;
        List<OrderRequestDto> orderRequests = createValidOrderRequests(nbReq);

        // WHEN
        List<OrderResponseDto> responses = orderRequestService.createOrderFromRequests(orderRequests, null);

        // THEN
        checkOrderRequestResponses(responses, nbReq, OrderRequestStatus.GRANTED, null, 0L);
    }

    @Test
    @Purpose("Test if failure responses are sent following an invalid order creation.")
    public void send_orderRequests_fail() throws TooManyItemsSelectedInBasketException, EmptySelectionException {
        // GIVEN
        int nbReq = 2;
        List<OrderRequestDto> orderRequests = createValidOrderRequests(nbReq);
        EmptySelectionException expectedException = new EmptySelectionException();
        Mockito.when(basketService.addSelection(any(), any(), any(), any())).thenThrow(expectedException);

        // WHEN
        List<OrderResponseDto> responses = orderRequestService.createOrderFromRequests(orderRequests, null);

        // THEN
        checkOrderRequestResponses(responses,
                                   nbReq,
                                   OrderRequestStatus.FAILED,
                                   String.format(OrderRequestService.ERROR_RESPONSE_FORMAT,
                                                 OrderRequestServiceException.class.getSimpleName(),
                                                 String.format(AutoOrderCompletionService.ERROR_RESPONSE_FORMAT,
                                                               expectedException.getClass().getSimpleName(),
                                                               expectedException.getMessage())),
                                   0L);
    }

}
