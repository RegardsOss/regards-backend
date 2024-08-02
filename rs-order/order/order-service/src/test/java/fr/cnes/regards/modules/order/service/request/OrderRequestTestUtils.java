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

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.order.amqp.input.OrderRequestDtoEvent;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.dto.OrderErrorType;
import fr.cnes.regards.modules.order.dto.input.DataTypeLight;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.input.OrderRequestFilters;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.dto.output.OrderResponseDto;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Helper for order request tests
 *
 * @author Iliana Ghazali
 **/
public class OrderRequestTestUtils {

    public static final String TEST_USER_ORDER = "testUserOrderRequest";

    public static final String TEST_USER_UNKNOWN_ORDER = "unknownUser";

    public static final DefaultRole TEST_USER_ROLE = DefaultRole.EXPLOIT;

    public static final List<String> SEARCH_QUERIES = List.of("type:Feature");

    public static final String FILENAME_FILTER = "^.*";

    static List<OrderRequestDtoEvent> createValidOrderRequestEvents(int nbReq) {
        List<OrderRequestDtoEvent> orderRequestDtos = new ArrayList<>();
        for (int i = 0; i < nbReq; i++) {
            orderRequestDtos.add(new OrderRequestDtoEvent(SEARCH_QUERIES,
                                                          new OrderRequestFilters(Set.of(DataTypeLight.RAWDATA),
                                                                                  FILENAME_FILTER),
                                                          String.valueOf(i),
                                                          TEST_USER_ORDER,
                                                          null));
        }
        return orderRequestDtos;
    }

    static List<OrderRequestDtoEvent> createInvalidOrderRequestEvents(int nbReq) {
        List<OrderRequestDtoEvent> orderRequestDtos = new ArrayList<>();
        for (int i = 0; i < nbReq; i++) {
            OrderRequestDtoEvent invalidOrderRequest = new OrderRequestDtoEvent(SEARCH_QUERIES,
                                                                                new OrderRequestFilters(Set.of(
                                                                                    DataTypeLight.RAWDATA), null),
                                                                                String.valueOf(i),
                                                                                TEST_USER_ORDER,
                                                                                null);
            invalidOrderRequest.setUser(RandomStringUtils.randomAlphanumeric(129));
            orderRequestDtos.add(invalidOrderRequest);
        }
        return orderRequestDtos;
    }

    static List<OrderRequestDto> createValidOrderRequests(int nbReq) {
        List<OrderRequestDto> orderRequestDtos = new ArrayList<>();

        for (int i = 0; i < nbReq; i++) {
            orderRequestDtos.add(new OrderRequestDto(SEARCH_QUERIES,
                                                     new OrderRequestFilters(Set.of(DataTypeLight.RAWDATA), null),
                                                     String.valueOf(i),
                                                     TEST_USER_ORDER,
                                                     null));
        }
        return orderRequestDtos;
    }

    static void checkOrderRequestResponsesEvents(List<OrderResponseDtoEvent> actualResponseDtoEvents,
                                                 int expectedNbReq,
                                                 OrderRequestStatus expectedStatus,
                                                 String expectedMessage,
                                                 Long firstOrderId) {
        List<OrderResponseDtoEvent> expectedResponses = new ArrayList<>();
        for (int i = 0; i < expectedNbReq; i++) {
            Long orderId = expectedStatus.equals(OrderRequestStatus.FAILED)
                           || expectedStatus.equals(OrderRequestStatus.DENIED) ? null : firstOrderId + i;
            expectedResponses.add(new OrderResponseDtoEvent(expectedStatus,
                                                            orderId,
                                                            String.valueOf(i),
                                                            expectedMessage,
                                                            null,
                                                            null,
                                                            null,
                                                            null,
                                                            null));
        }
        Assertions.assertThat(actualResponseDtoEvents).hasSameElementsAs(expectedResponses);
    }

    static void checkOrderRequestResponsesEvents(List<OrderResponseDtoEvent> actualResponseDtoEvents,
                                                 int expectedNbReq,
                                                 OrderRequestStatus expectedStatus,
                                                 String expectedMessage,
                                                 Long firstOrderId,
                                                 OrderErrorType errorType,
                                                 Integer errors) {
        List<OrderResponseDtoEvent> expectedResponses = new ArrayList<>();
        for (int i = 0; i < expectedNbReq; i++) {
            Long orderId = expectedStatus.equals(OrderRequestStatus.FAILED)
                           || expectedStatus.equals(OrderRequestStatus.DENIED) ? null : firstOrderId + i;
            expectedResponses.add(new OrderResponseDtoEvent(expectedStatus,
                                                            orderId,
                                                            String.valueOf(i),
                                                            expectedMessage,
                                                            null,
                                                            errorType,
                                                            errors,
                                                            null,
                                                            null));
        }
        Assertions.assertThat(actualResponseDtoEvents).hasSameElementsAs(expectedResponses);
    }

    static void checkOrderRequestResponses(List<OrderResponseDto> actualResponseDtos,
                                           int expectedNbReq,
                                           OrderRequestStatus expectedStatus,
                                           String expectedMessage,
                                           Long firstOrderId) {
        List<OrderResponseDto> expectedResponses = new ArrayList<>();
        for (int i = 0; i < expectedNbReq; i++) {
            Long orderId = expectedStatus.equals(OrderRequestStatus.FAILED)
                           || expectedStatus.equals(OrderRequestStatus.DENIED) ? null : firstOrderId + i;
            expectedResponses.add(new OrderResponseDto(expectedStatus,
                                                       orderId,
                                                       String.valueOf(i),
                                                       expectedMessage,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null));
        }

        Assertions.assertThat(actualResponseDtos).hasSameElementsAs(expectedResponses);
    }

}
