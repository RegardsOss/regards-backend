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
package fr.cnes.regards.modules.order.client.amqp;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.client.env.config.OrderClientTestConfiguration;
import fr.cnes.regards.modules.order.dto.input.DataTypeLight;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.input.OrderRequestFilters;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.times;

/**
 * Test for {@link AutoOrderRequestClient}.
 *
 * @author Iliana Ghazali
 **/
@ActiveProfiles({ "test", "testAmqp" })
@ContextConfiguration(classes = { OrderClientTestConfiguration.class })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=auto_order_request_client_it",
                                   "regards.amqp.enabled=true" })
@SpringBootTest
public class AutoOrderRequestClientIT extends AbstractMultitenantServiceIT {

    @Autowired
    private IAutoOrderRequestClient autoOrderRequestClient; // class under test

    @Test
    public void givenOrderRequest_whenHandled_thenExpectPublishedEvents() {
        // GIVEN
        int nbRequests = 2;
        List<OrderRequestDto> orderRequests = simulateOrderRequests(nbRequests);

        // WHEN
        autoOrderRequestClient.publishOrderRequestEvents(orderRequests, 200L);

        // THEN
        // check that OrderRequestDtoEvents were sent from OrderRequestDtos
        ArgumentCaptor<OrderResponseDtoEvent> responseCaptor = ArgumentCaptor.forClass(OrderResponseDtoEvent.class);
        Mockito.verify(publisher, times(nbRequests)).publish(responseCaptor.capture());
    }

    private List<OrderRequestDto> simulateOrderRequests(int nbRequests) {
        List<OrderRequestDto> orderRequests = new ArrayList<>();
        String corrId = RandomStringUtils.random(4);
        String user = "test-auto-request@test.fr";
        for (int i = 0; i < nbRequests; i++) {
            orderRequests.add(new OrderRequestDto(List.of("tag:test"),
                                                  new OrderRequestFilters(Set.of(DataTypeLight.RAWDATA), null),
                                                  corrId,
                                                  user,
                                                  null));
        }
        return orderRequests;
    }
}
