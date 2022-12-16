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

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.order.amqp.input.OrderRequestDtoEvent;
import fr.cnes.regards.modules.order.amqp.output.OrderRequestResponseDtoEvent;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.input.OrderRequestFilters;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import org.assertj.core.api.Assertions;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper for order request tests
 *
 * @author Iliana Ghazali
 **/
public class OrderRequestTestUtils {

    public static final String TEST_USER_ORDER = "testUserOrderRequest";

    public static final DefaultRole TEST_USER_ROLE = DefaultRole.EXPLOIT;

    public static final List<String> SEARCH_QUERIES = List.of("some:opensearchrequest");

    public static final String CORR_ID_FORMAT = "Corr n°%d";

    static List<OrderRequestDtoEvent> createValidOrderRequestEvents(int nbReq) {
        List<OrderRequestDtoEvent> orderRequestDtos = new ArrayList<>();
        for (int i = 0; i < nbReq; i++) {
            orderRequestDtos.add(new OrderRequestDtoEvent(SEARCH_QUERIES,
                                                          String.format(CORR_ID_FORMAT, i),
                                                          TEST_USER_ORDER,
                                                          new OrderRequestFilters(List.of(DataType.AIP), null)));
        }
        return orderRequestDtos;
    }

    static List<OrderRequestDtoEvent> createInvalidOrderRequestEvents(int nbReq) {
        List<OrderRequestDtoEvent> orderRequestDtos = new ArrayList<>();
        for (int i = 0; i < nbReq; i++) {
            orderRequestDtos.add(new OrderRequestDtoEvent(SEARCH_QUERIES,
                                                          null,
                                                          TEST_USER_ORDER,
                                                          new OrderRequestFilters(List.of(), null)));
        }
        return orderRequestDtos;
    }

    static List<OrderRequestDto> createValidOrderRequests(int nbReq) {
        List<OrderRequestDto> orderRequestDtos = new ArrayList<>();

        for (int i = 0; i < nbReq; i++) {
            orderRequestDtos.add(new OrderRequestDto(SEARCH_QUERIES,
                                                     String.format(CORR_ID_FORMAT, i),
                                                     TEST_USER_ORDER,
                                                     new OrderRequestFilters(List.of(DataType.AIP), null)));
        }
        return orderRequestDtos;
    }

    static void checkOrderRequestResponses(int nbReq,
                                           List<OrderRequestResponseDtoEvent> actualResponseDtoEvents,
                                           OrderRequestStatus status,
                                           String message) {
        List<OrderRequestResponseDtoEvent> expectedResponses = new ArrayList<>();
        for (int i = 0; i < nbReq; i++) {
            expectedResponses.add(new OrderRequestResponseDtoEvent(String.format(CORR_ID_FORMAT, i), status, message));
        }
        Assertions.assertThat(actualResponseDtoEvents).hasSameElementsAs(expectedResponses);
    }

    static void checkOrders(List<Order> createdOrders, int nbReq, OrderStatus status) {
        for (int i = 0; i < nbReq; i++) {
            Order order = createdOrders.get(i);
            assertThat(order.getOwner()).isEqualTo(TEST_USER_ORDER);
            assertThat(order.getCorrelationId()).isEqualTo(String.valueOf(i));
            assertThat(order.getStatus()).isEqualTo(status);

        }
    }
}
