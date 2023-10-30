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
package fr.cnes.regards.modules.delivery.service.order.manager;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.dao.IDeliveryRequestRepository;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryStatus;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.client.amqp.AutoOrderResponseHandler;
import fr.cnes.regards.modules.order.dto.OrderErrorType;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.timeout;

/**
 * @author Stephane Cortine
 */
@ActiveProfiles({ "test", "testAmqp", "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=order_response_handler_it",
                                   "regards.amqp.enabled=true" })
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EnableRetry
public class OrderResponseEventHandlerIT extends AbstractMultitenantServiceIT {

    @SpyBean
    private AutoOrderResponseHandler handler; // class under test

    @Autowired
    private IDeliveryRequestRepository deliveryRequestRepository;

    @Autowired
    private DeliveryRequestService deliveryRequestService;

    @Autowired
    private EndingDeliveryService endingDeliveryService;

    @Before
    public void init() {
        deliveryRequestRepository.deleteAll();
        Mockito.reset(publisher);
    }

    /**
     * Test the select request in database (select request used in {@link OrderResponseEventHandler} with the IN
     * clause containing a lot of correlation identifiers.
     * If there are too many correlation identifiers in the IN clause or the size of correlation identifier is too
     * many; the test will fail.
     */
    @Test
    public void test_select_request() {
        // Bulk size limit by default to handle messages in the order response event handler
        int bulkSize = 1000;
        deliveryRequestService.saveAllRequests(simulateDeliveryRequest(bulkSize));

        handler.handleBatch(simulateResponse(bulkSize));
    }

    /**
     * Test the handler of order response event in multithreading (repeat 5 times in order to be certain the result
     * is always the same in function when events can arrive in the handler).
     */
    @Test
    @Repeat(value = 5)
    public void test_multiThreading_with_order_response_events() throws InterruptedException {
        // Given
        String correlationId = "corr-MULTITHREADING-orderDelivery";
        OffsetDateTime nowMinusOneHour = OffsetDateTime.now().minusHours(1);
        DeliveryRequest savedDeliverySaved = deliveryRequestService.saveRequest(createDeliveryRequest(correlationId,
                                                                                                      nowMinusOneHour,
                                                                                                      DeliveryRequestStatus.GRANTED));

        List<OrderResponseDtoEvent> orderResponseDtoEvents = new ArrayList<>();
        orderResponseDtoEvents.add(new OrderResponseDtoEvent(OrderRequestStatus.SUBORDER_DONE,
                                                             1L,
                                                             correlationId,
                                                             null,
                                                             null,
                                                             null,
                                                             0,
                                                             3,
                                                             10L));

        orderResponseDtoEvents.add(new OrderResponseDtoEvent(OrderRequestStatus.SUBORDER_DONE,
                                                             1L,
                                                             correlationId,
                                                             null,
                                                             null,
                                                             null,
                                                             0,
                                                             3,
                                                             11L));

        orderResponseDtoEvents.add(new OrderResponseDtoEvent(OrderRequestStatus.FAILED,
                                                             1L,
                                                             correlationId,
                                                             "Error message",
                                                             null,
                                                             OrderErrorType.INTERNAL_ERROR,
                                                             2,
                                                             3,
                                                             12L));
        final ExecutorService executor = Executors.newFixedThreadPool(orderResponseDtoEvents.size());

        // When
        for (final OrderResponseDtoEvent orderResponseDtoEvent : orderResponseDtoEvents) {
            executor.execute(() -> {
                runtimeTenantResolver.forceTenant("PROJECT");
                handler.handleBatch(Collections.singletonList(orderResponseDtoEvent));
            });
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // Then
        DeliveryRequest expectedDeliveryRequest = test_deliveryRequest_exists_in_database(savedDeliverySaved.getId());

        assertEquals(1L, expectedDeliveryRequest.getOrderId());
        assertEquals(DeliveryRequestStatus.ERROR, expectedDeliveryRequest.getDeliveryStatus().getStatus());
        assertTrue(nowMinusOneHour.isBefore(expectedDeliveryRequest.getDeliveryStatus().getStatusDate()));
        assertEquals(3, expectedDeliveryRequest.getTotalSubOrders());
    }

    /**
     * Test a list of order response events in order to check the result is in ERROR status when the received last
     * event is not in ERROR status.
     */
    @Test
    public void test_list_order_response_event_with_same_correlationId() {
        String correlationId = "corr-TEST-orderDelivery";
        OffsetDateTime nowMinusOneHour = OffsetDateTime.now().minusHours(1);
        DeliveryRequest savedDeliverySaved = deliveryRequestService.saveRequest(createDeliveryRequest(correlationId,
                                                                                                      nowMinusOneHour,
                                                                                                      DeliveryRequestStatus.GRANTED));

        List<OrderResponseDtoEvent> orderResponseDtoEvents = new LinkedList<>();
        orderResponseDtoEvents.add(new OrderResponseDtoEvent(OrderRequestStatus.SUBORDER_DONE,
                                                             1L,
                                                             correlationId,
                                                             null,
                                                             null,
                                                             null,
                                                             0,
                                                             3,
                                                             10L));

        orderResponseDtoEvents.add(new OrderResponseDtoEvent(OrderRequestStatus.FAILED,
                                                             1L,
                                                             correlationId,
                                                             "Error message",
                                                             null,
                                                             OrderErrorType.INTERNAL_ERROR,
                                                             2,
                                                             3,
                                                             11L));

        orderResponseDtoEvents.add(new OrderResponseDtoEvent(OrderRequestStatus.SUBORDER_DONE,
                                                             1L,
                                                             correlationId,
                                                             null,
                                                             null,
                                                             null,
                                                             0,
                                                             3,
                                                             12L));

        // When
        handler.handleBatch(orderResponseDtoEvents);

        DeliveryRequest expectedDeliveryRequest = test_deliveryRequest_exists_in_database(savedDeliverySaved.getId());

        assertEquals(1L, expectedDeliveryRequest.getOrderId());
        assertEquals(DeliveryRequestStatus.ERROR, expectedDeliveryRequest.getDeliveryStatus().getStatus());
        assertEquals(DeliveryErrorType.TOO_MANY_SUBORDERS, expectedDeliveryRequest.getErrorType());
        assertTrue(nowMinusOneHour.isBefore(expectedDeliveryRequest.getDeliveryStatus().getStatusDate()));
        assertEquals(3, expectedDeliveryRequest.getTotalSubOrders());
    }

    @Test
    public void test_orderResponseEvent_granted() {
        // Given
        String correlationId = "corr-GRANTED-orderDelivery";
        OffsetDateTime nowMinusOneHour = OffsetDateTime.now().minusHours(1);
        DeliveryRequest savedDeliverySaved = deliveryRequestService.saveRequest(createDeliveryRequest(correlationId,
                                                                                                      nowMinusOneHour,
                                                                                                      DeliveryRequestStatus.GRANTED));

        // When
        handler.handleBatch(Collections.singletonList(new OrderResponseDtoEvent(OrderRequestStatus.GRANTED,
                                                                                1L,
                                                                                correlationId,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null)));

        // Then
        DeliveryRequest expectedDeliveryRequest = test_deliveryRequest_exists_in_database(savedDeliverySaved.getId());

        assertEquals(1L, expectedDeliveryRequest.getOrderId());
        assertEquals(DeliveryRequestStatus.GRANTED, expectedDeliveryRequest.getDeliveryStatus().getStatus());
        assertTrue(nowMinusOneHour.isBefore(expectedDeliveryRequest.getDeliveryStatus().getStatusDate()));

        ArgumentCaptor<DeliveryResponseDtoEvent> responseCaptor = ArgumentCaptor.forClass(DeliveryResponseDtoEvent.class);
        // handler do nothing when receiving GRANTED from order
        Mockito.verify(publisher, timeout(100).times(0)).publish(responseCaptor.capture());
    }

    @Test
    public void test_orderResponseEvent_suborder_done() {
        // Given
        String correlationId = "corr-SUBORDER-DONE-orderDelivery";
        OffsetDateTime nowMinusOneHour = OffsetDateTime.now().minusHours(1);
        DeliveryRequest savedDeliverySaved = deliveryRequestService.saveRequest(createDeliveryRequest(correlationId,
                                                                                                      nowMinusOneHour,
                                                                                                      DeliveryRequestStatus.GRANTED));
        // When
        handler.handleBatch(Collections.singletonList(new OrderResponseDtoEvent(OrderRequestStatus.DONE,
                                                                                1L,
                                                                                correlationId,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                0,
                                                                                1,
                                                                                10L)));

        // Then
        DeliveryRequest expectedDeliveryRequest = test_deliveryRequest_exists_in_database(savedDeliverySaved.getId());

        assertEquals(1L, expectedDeliveryRequest.getOrderId());
        assertEquals(DeliveryRequestStatus.DONE,
                     expectedDeliveryRequest.getDeliveryStatus().getStatus(),
                     "The status must be DONE");
        assertTrue(nowMinusOneHour.isBefore(expectedDeliveryRequest.getDeliveryStatus().getStatusDate()));
        assertEquals(1, expectedDeliveryRequest.getTotalSubOrders());
    }

    @Test
    public void test_orderResponseEvent_suborder_done_with_error() {
        // Given
        String correlationId = "corr-ERROR-orderDelivery";
        OffsetDateTime nowMinusOneHour = OffsetDateTime.now().minusHours(1);
        DeliveryRequest savedDeliverySaved = deliveryRequestService.saveRequest(createDeliveryRequest(correlationId,
                                                                                                      nowMinusOneHour,
                                                                                                      DeliveryRequestStatus.GRANTED));
        // When
        handler.handleBatch(Collections.singletonList(new OrderResponseDtoEvent(OrderRequestStatus.SUBORDER_DONE,
                                                                                1L,
                                                                                correlationId,
                                                                                "Error message",
                                                                                null,
                                                                                OrderErrorType.INTERNAL_ERROR,
                                                                                1,
                                                                                1,
                                                                                10L)));

        // Then
        DeliveryRequest expectedDeliveryRequest = test_deliveryRequest_exists_in_database(savedDeliverySaved.getId());

        assertEquals(1L, expectedDeliveryRequest.getOrderId());
        assertEquals(DeliveryRequestStatus.ERROR, expectedDeliveryRequest.getDeliveryStatus().getStatus());
        assertTrue(nowMinusOneHour.isBefore(expectedDeliveryRequest.getDeliveryStatus().getStatusDate()));
        assertEquals(1, expectedDeliveryRequest.getTotalSubOrders());
    }

    @Test
    public void test_orderResponseEvent_suborder_done_with_several_suborders() {
        // Given
        String correlationId = "corr-ERROR_SUBORDER-orderDelivery";
        OffsetDateTime nowMinusOneHour = OffsetDateTime.now().minusHours(1);
        DeliveryRequest savedDeliverySaved = deliveryRequestService.saveRequest(createDeliveryRequest(correlationId,
                                                                                                      nowMinusOneHour,
                                                                                                      DeliveryRequestStatus.GRANTED));
        // When
        handler.handleBatch(Collections.singletonList(new OrderResponseDtoEvent(OrderRequestStatus.SUBORDER_DONE,
                                                                                1L,
                                                                                correlationId,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                1,
                                                                                2,
                                                                                10L)));

        // Then
        DeliveryRequest expectedDeliveryRequest = test_deliveryRequest_exists_in_database(savedDeliverySaved.getId());

        assertEquals(1L, expectedDeliveryRequest.getOrderId());
        // In ERROR status because several sub-orders in the order response event
        assertEquals(DeliveryRequestStatus.ERROR, expectedDeliveryRequest.getDeliveryStatus().getStatus());
        assertEquals(DeliveryErrorType.TOO_MANY_SUBORDERS, expectedDeliveryRequest.getErrorType());
        assertTrue(nowMinusOneHour.isBefore(expectedDeliveryRequest.getDeliveryStatus().getStatusDate()));
        assertEquals(2, expectedDeliveryRequest.getTotalSubOrders());

        Void run = new EndingDeliveryTask(endingDeliveryService, 5000).run();

        Mockito.verify(publisher, timeout(100).times(1)).publish(Mockito.any(DeliveryResponseDtoEvent.class));
    }

    @Test
    public void test_orderResponseEvent_suborder_done_with_delivery_request_already_in_error() {
        // Given
        String correlationId = "corr-ERROR-orderDelivery";
        // For specific the comparing of egality, the date is truncate with ChronoUnit.MICROS like in database
        OffsetDateTime nowMinusOneHour = OffsetDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MICROS);
        DeliveryRequest savedDeliverySaved = deliveryRequestService.saveRequest(createDeliveryRequest(correlationId,
                                                                                                      nowMinusOneHour,
                                                                                                      DeliveryRequestStatus.ERROR));
        // When
        handler.handleBatch(Collections.singletonList(new OrderResponseDtoEvent(OrderRequestStatus.SUBORDER_DONE,
                                                                                1L,
                                                                                correlationId,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                1,
                                                                                2,
                                                                                10L)));

        // Then
        DeliveryRequest expectedDeliveryRequest = test_deliveryRequest_exists_in_database(savedDeliverySaved.getId());

        assertNull(expectedDeliveryRequest.getOrderId());
        assertEquals(DeliveryRequestStatus.ERROR, expectedDeliveryRequest.getDeliveryStatus().getStatus());
        assertTrue(nowMinusOneHour.isEqual(expectedDeliveryRequest.getDeliveryStatus().getStatusDate()));
        assertNull(expectedDeliveryRequest.getTotalSubOrders());
    }

    @Test
    public void test_orderResponseEvent_order_done() {
        // Given
        String correlationId = "corr-ORDER_DONE-orderDelivery";
        OffsetDateTime nowMinusOneHour = OffsetDateTime.now().minusHours(1);
        DeliveryRequest savedDeliverySaved = deliveryRequestService.saveRequest(createDeliveryRequest(correlationId,
                                                                                                      nowMinusOneHour,
                                                                                                      DeliveryRequestStatus.GRANTED));
        // When
        handler.handleBatch(Collections.singletonList(new OrderResponseDtoEvent(OrderRequestStatus.DONE,
                                                                                1L,
                                                                                correlationId,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                1,
                                                                                10L)));

        // Then
        DeliveryRequest expectedDeliveryRequest = test_deliveryRequest_exists_in_database(savedDeliverySaved.getId());

        assertEquals(1L, expectedDeliveryRequest.getOrderId());
        assertEquals(DeliveryRequestStatus.DONE, expectedDeliveryRequest.getDeliveryStatus().getStatus());
        assertTrue(nowMinusOneHour.isBefore(expectedDeliveryRequest.getDeliveryStatus().getStatusDate()));
        assertEquals(1, expectedDeliveryRequest.getTotalSubOrders());
    }

    @Test
    public void test_orderResponseEvent_failed() {
        // Given
        String correlationId = "corr-FAILED-orderDelivery";
        OffsetDateTime nowMinusOneHour = OffsetDateTime.now().minusHours(1);
        DeliveryRequest savedDeliverySaved = deliveryRequestService.saveRequest(createDeliveryRequest(correlationId,
                                                                                                      nowMinusOneHour,
                                                                                                      DeliveryRequestStatus.GRANTED));

        // When
        handler.handleBatch(Collections.singletonList(new OrderResponseDtoEvent(OrderRequestStatus.FAILED,
                                                                                1L,
                                                                                correlationId,
                                                                                "Error message",
                                                                                null,
                                                                                OrderErrorType.EMPTY_ORDER,
                                                                                1,
                                                                                1,
                                                                                10L)));

        // Then
        DeliveryRequest expectedDeliveryRequest = test_deliveryRequest_exists_in_database(savedDeliverySaved.getId());

        assertEquals(1L, expectedDeliveryRequest.getOrderId());
        assertEquals(DeliveryRequestStatus.ERROR, expectedDeliveryRequest.getDeliveryStatus().getStatus());
        assertEquals("Error message", expectedDeliveryRequest.getDeliveryStatus().getErrorCause());
        assertEquals(DeliveryErrorType.EMPTY_ORDER, expectedDeliveryRequest.getDeliveryStatus().getErrorType());
        assertTrue(nowMinusOneHour.isBefore(expectedDeliveryRequest.getDeliveryStatus().getStatusDate()));
        assertEquals(1, expectedDeliveryRequest.getTotalSubOrders());
    }

    @Test
    public void test_orderResponseEvent_denied() {
        // Given
        String correlationId = "corr-DENIED-orderDelivery";
        OffsetDateTime nowMinusOneHour = OffsetDateTime.now().minusHours(1);
        DeliveryRequest savedDeliverySaved = deliveryRequestService.saveRequest(createDeliveryRequest(correlationId,
                                                                                                      nowMinusOneHour,
                                                                                                      DeliveryRequestStatus.GRANTED));
        // Check if the delivery request is saved right in db
        test_deliveryRequest_exists_in_database(savedDeliverySaved.getId());
        // When
        handler.handleBatch(Collections.singletonList(new OrderResponseDtoEvent(OrderRequestStatus.DENIED,
                                                                                1L,
                                                                                correlationId,
                                                                                "Error message",
                                                                                null,
                                                                                OrderErrorType.ORDER_LIMIT_REACHED,
                                                                                null,
                                                                                null,
                                                                                null)));

        // Then
        Optional<DeliveryRequest> expectedDeliveryRequest = deliveryRequestService.findDeliveryRequest(
            savedDeliverySaved.getId());
        if (expectedDeliveryRequest.isPresent()) {
            fail("The delivery request must not exist in db");
        }
        ArgumentCaptor<DeliveryResponseDtoEvent> responseCaptor = ArgumentCaptor.forClass(DeliveryResponseDtoEvent.class);
        Mockito.verify(publisher, timeout(100).times(1)).publish(responseCaptor.capture());
        List<DeliveryResponseDtoEvent> publishedDeliveryResponseEvt = responseCaptor.getAllValues();

        assertEquals(1, publishedDeliveryResponseEvt.size());
        assertEquals(correlationId, publishedDeliveryResponseEvt.get(0).getCorrelationId());
        assertEquals(DeliveryRequestStatus.ERROR, publishedDeliveryResponseEvt.get(0).getStatus());
        assertEquals("Error message", publishedDeliveryResponseEvt.get(0).getMessage());
        assertEquals(DeliveryErrorType.ORDER_LIMIT_REACHED, publishedDeliveryResponseEvt.get(0).getErrorType());
        assertEquals("originAppId", publishedDeliveryResponseEvt.get(0).getOriginRequestAppId().orElse(null));
        assertEquals(1, publishedDeliveryResponseEvt.get(0).getOriginRequestPriority().orElse(null));
    }

    // ---------------------
    // -- UTILITY METHODS --
    // ---------------------

    private DeliveryRequest test_deliveryRequest_exists_in_database(Long deliveryRequestId) {
        Optional<DeliveryRequest> expectedDeliveryRequest = deliveryRequestService.findDeliveryRequest(deliveryRequestId);
        if (expectedDeliveryRequest.isEmpty()) {
            fail("The delivery request must exist in db");
        }
        return expectedDeliveryRequest.get();
    }

    private List<OrderResponseDtoEvent> simulateResponse(int nbResponses) {
        List<OrderResponseDtoEvent> responses = new ArrayList<>(nbResponses);

        for (int index = 0; index < nbResponses; index++) {
            responses.add(new OrderResponseDtoEvent(OrderRequestStatus.GRANTED,
                                                    1L,
                                                    UUID.randomUUID()
                                                    + "-corr-"
                                                    + index
                                                    + "-orderDelivery-"
                                                    + UUID.randomUUID(),
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null));
        }
        return responses;
    }

    private DeliveryRequest createDeliveryRequest(String correlationId,
                                                  OffsetDateTime statusDate,
                                                  DeliveryRequestStatus status) {
        return new DeliveryRequest(correlationId,
                                   "usertest",
                                   new DeliveryStatus(statusDate, statusDate, 1000, status, null, null),
                                   null,
                                   null,
                                   "originAppId",
                                   1);
    }

    private List<DeliveryRequest> simulateDeliveryRequest(int nbRequests) {
        List<DeliveryRequest> requests = new ArrayList<>(nbRequests);

        for (int index = 0; index < nbRequests; index++) {
            OffsetDateTime now = OffsetDateTime.now();
            requests.add(createDeliveryRequest(UUID.randomUUID()
                                               + "-corr-"
                                               + index
                                               + "-orderDelivery-"
                                               + UUID.randomUUID(), now, DeliveryRequestStatus.GRANTED));
        }
        return requests;
    }

}
