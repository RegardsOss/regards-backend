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
import fr.cnes.regards.modules.order.client.env.mocks.AutoOrderResponseClientMock;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.cnes.regards.modules.order.dto.output.OrderRequestStatus.*;
import static org.mockito.Mockito.timeout;

/**
 * Tests for {@link AutoOrderResponseHandler}.
 *
 * @author Iliana Ghazali
 **/
@ActiveProfiles({ "test", "testAmqp" })
@ContextConfiguration(classes = { OrderClientTestConfiguration.class })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=auto_order_response_handler_it",
                                   "regards.amqp.enabled=true" })
@SpringBootTest
public class AutoOrderResponseHandlerIT extends AbstractMultitenantServiceIT {

    @SpyBean
    private AutoOrderResponseHandler handler; // class under test

    @Autowired
    private AutoOrderResponseClientMock clientMock;

    @Before
    public void init() {
        clientMock.reset();
    }

    @Test
    public void givenResponses_whenPublished_thenReceiveEvents() {
        // GIVEN
        Map<OrderRequestStatus, Integer> expectedNbEventsByStatus = Map.of(GRANTED,
                                                                           1,
                                                                           DENIED,
                                                                           2,
                                                                           SUBORDER_DONE,
                                                                           3,
                                                                           DONE,
                                                                           4,
                                                                           FAILED,
                                                                           8);
        // WHEN
        publisher.publish(simulateResponses(expectedNbEventsByStatus));
        // THEN
        // check events were successfully received
        Mockito.verify(handler, timeout(2000)).handleBatch(Mockito.any());
        // check events were handled correctly according to their status
        Assertions.assertThat(clientMock.countEventsByStatus())
                  .containsExactlyInAnyOrderEntriesOf(expectedNbEventsByStatus);
    }

    private List<OrderResponseDtoEvent> simulateResponses(Map<OrderRequestStatus, Integer> expectedNbEventsByStatus) {
        long orderId = RandomUtils.nextLong();
        List<OrderResponseDtoEvent> responses = new ArrayList<>(expectedNbEventsByStatus.values().size());
        expectedNbEventsByStatus.forEach((status, nb) -> {
            for (int i = 0; i < nb; i++) {
                responses.add(new OrderResponseDtoEvent(status,
                                                        orderId,
                                                        String.valueOf("corr-" + i + "-" + status),
                                                        "random",
                                                        null));
            }
        });
        return responses;
    }
}
