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
package fr.cnes.regards.modules.delivery.service.submission.create;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.delivery.amqp.input.DeliveryRequestDtoEvent;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.dao.IDeliveryRequestRepository;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.settings.DeliverySettingService;
import fr.cnes.regards.modules.delivery.service.submission.create.amqp.DeliveryRequestEventHandler;
import fr.cnes.regards.modules.order.amqp.input.OrderRequestDtoEvent;
import fr.cnes.regards.modules.order.dto.input.DataTypeLight;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.input.OrderRequestFilters;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;

/**
 * Test for {@link DeliveryRequestEventHandler}.
 * <p>The purpose of this test is to check if {@link DeliveryRequestDtoEvent}s are properly handled when received.</p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #givenValidEvents_whenSent_thenExpectGranted()}</li>
 *    </ul></li>
 *  <li>Error cases :
 *    <ul>
 *      <li>{@link #givenEventsWithInvalidUser_whenSent_thenExpectDenied()}</li>
 *      <li>{@link #givenEventsWithInvalidModel_whenSent_thenExpectDenied()}</li>
 *    </ul></li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@ActiveProfiles({ "test", "testAmqp",  "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=delivery_request_handler_it",
                                   "regards.amqp.enabled=true" })
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DeliveryRequestEventHandlerIT extends AbstractMultitenantServiceIT {

    private static final String TEST_USER = "user-delivery@test.fr";

    private static final int DEFAULT_PRIORITY = 1;

    @SpyBean
    private DeliveryRequestEventHandler handler; // class under test

    @Autowired
    private IProjectUsersClient projectUsersClient;

    @Autowired
    private IDeliveryRequestRepository deliveryRequestRepository;

    @Autowired
    private DeliverySettingService deliverySettingService;

    @Before
    public void init() {
        clean();
    }

    private void clean() {
        deliveryRequestRepository.deleteAll();
        Mockito.reset(publisher);
    }

    @Test
    public void givenEventsWithInvalidUser_whenSent_thenExpectDenied() {
        // GIVEN
        int nbRequests = 3;
        // WHEN
        handler.handleBatch(simulateRequests(nbRequests));
        // THEN
        // check events were handled with denied status because user is not known
        ArgumentCaptor<DeliveryResponseDtoEvent> responseCaptor = ArgumentCaptor.forClass(DeliveryResponseDtoEvent.class);
        Mockito.verify(publisher, timeout(100).times(nbRequests)).publish(responseCaptor.capture());
        List<DeliveryResponseDtoEvent> actualResponses = responseCaptor.getAllValues();
        Assertions.assertThat(actualResponses.size()).isEqualTo(nbRequests);
        for (int i = 0; i < nbRequests; i++) {
            DeliveryResponseDtoEvent response = actualResponses.get(i);
            Assertions.assertThat(response.getCorrelationId()).isEqualTo("corr-" + i + "-delivery");
            Assertions.assertThat(response.getStatus()).isEqualTo(DeliveryRequestStatus.DENIED);
            Assertions.assertThat(response.getErrorType()).isEqualTo(DeliveryErrorType.FORBIDDEN);
            Assertions.assertThat(response.getMessage()).contains("Unknown user");
        }
    }

    @Test
    public void givenEventsWithInvalidModel_whenSent_thenExpectDenied() {
        // GIVEN
        int nbRequests = 2;
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(anyString()))
               .thenReturn(ResponseEntity.ok(EntityModel.of(new ProjectUser())));
        // WHEN
        List<DeliveryRequestDtoEvent> events = simulateRequests(nbRequests);
        events.forEach(event -> event.getOrder().setCorrelationId(RandomStringUtils.random(256)));
        handler.handleBatch(events);
        // THEN
        // check events were handled with denied status because correlationId is too long
        ArgumentCaptor<DeliveryResponseDtoEvent> responseCaptor = ArgumentCaptor.forClass(DeliveryResponseDtoEvent.class);
        Mockito.verify(publisher, timeout(100).times(nbRequests)).publish(responseCaptor.capture());
        List<DeliveryResponseDtoEvent> actualResponses = responseCaptor.getAllValues();
        Assertions.assertThat(actualResponses.size()).isEqualTo(nbRequests);
        for (int i = 0; i < nbRequests; i++) {
            DeliveryResponseDtoEvent response = actualResponses.get(i);
            Assertions.assertThat(response.getCorrelationId()).isEqualTo("corr-" + i + "-delivery");
            Assertions.assertThat(response.getStatus()).isEqualTo(DeliveryRequestStatus.DENIED);
            Assertions.assertThat(response.getErrorType()).isEqualTo(DeliveryErrorType.INVALID_CONTENT);
            Assertions.assertThat(response.getMessage()).contains("provided correlationId");
        }
    }

    @Test
    public void givenValidEvents_whenSent_thenExpectGranted() {
        // GIVEN
        int nbRequests = 4;
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(anyString()))
               .thenReturn(ResponseEntity.ok(EntityModel.of(new ProjectUser())));
        // WHEN
        handler.handleBatch(simulateRequests(nbRequests));
        // THEN
        // check events were handled with granted status
        // published events are OrderRequestDtoEvents from DeliveryRequestDtoEvents
        ArgumentCaptor<ISubscribable> responseCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, timeout(2000).times(nbRequests)).publish(responseCaptor.capture());

        // check order responses
        List<OrderRequestDtoEvent> orderEvents = responseCaptor.getAllValues()
                                                               .stream()
                                                               .filter(event -> event instanceof OrderRequestDtoEvent)
                                                               .map(orderEvent -> (OrderRequestDtoEvent) orderEvent)
                                                               .toList();
        Assertions.assertThat(orderEvents.size()).isEqualTo(nbRequests);
        for (int i = 0; i < nbRequests; i++) {
            OrderRequestDtoEvent orderEvent = orderEvents.get(i);
            Assertions.assertThat(orderEvent.getCorrelationId()).isEqualTo("corr-" + i + "-delivery");
            Assertions.assertThat(orderEvent.getUser()).isEqualTo(TEST_USER);
            Assertions.assertThat(orderEvent.getFilters().getDataTypes())
                      .containsExactlyInAnyOrder(DataTypeLight.RAWDATA);
            Assertions.assertThat(orderEvent.getQueries()).isEqualTo(List.of("type:Feature"));
        }

        // check delivery requests are successfully registered
        List<DeliveryRequest> deliveryRequests = deliveryRequestRepository.findAll();
        Assertions.assertThat(deliveryRequests.size()).isEqualTo(nbRequests);
        for (int i = 0; i < nbRequests; i++) {
            DeliveryRequest request = deliveryRequests.get(i);
            Assertions.assertThat(request.getCorrelationId()).isEqualTo("corr-" + i + "-delivery");
            Assertions.assertThat(request.getStatus()).isEqualTo(DeliveryRequestStatus.GRANTED);
            Assertions.assertThat(request.getCreationDate()).isNotNull();
            Assertions.assertThat(request.getStatusDate()).isNotNull();
            Assertions.assertThat(request.getExpiryDate())
                      .isEqualTo(request.getCreationDate()
                                        .plusHours((Integer) deliverySettingService.getValue(DeliverySettings.REQUEST_TTL_HOURS)));
            Assertions.assertThat(request.getOriginRequestAppId()).isEqualTo(TEST_USER);
            Assertions.assertThat(request.getOriginRequestPriority()).isEqualTo(DEFAULT_PRIORITY);
        }
    }

    @Test
    public void givenEventsWithDuplicatedCorrelationIds_whenSent_thenExpectIgnored() {
        givenValidEvents_whenSent_thenExpectGranted();
        givenValidEvents_whenSent_thenExpectGranted();
    }

    private List<DeliveryRequestDtoEvent> simulateRequests(int nbRequests) {
        List<DeliveryRequestDtoEvent> responses = new ArrayList<>(nbRequests);
        for (int i = 0; i < nbRequests; i++) {
            responses.add(new DeliveryRequestDtoEvent("corr-" + i + "-delivery",
                                                      "target",
                                                      new OrderRequestDto(List.of("type:Feature"),
                                                                          new OrderRequestFilters(Set.of(DataTypeLight.RAWDATA),
                                                                                                  null),
                                                                          String.valueOf(i),
                                                                          TEST_USER,
                                                                          null)));
        }
        return responses;
    }

}