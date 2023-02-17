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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.service.commons.AbstractOrderServiceIT;
import fr.cnes.regards.modules.order.test.OrderTestUtils;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Thomas GUILLOU
 **/
@ActiveProfiles(value = { "default", "test", "testAmqp", "noscheduler" }, inheritProfiles = false)
@ContextConfiguration(classes = ServiceConfiguration.class)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=order_test_amqp_it",
                                   "regards.amqp.enabled=true",
                                   "regards.order.max.storage.files.jobs.per.user=1" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD,
                hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)
public class OrderServiceAmqpIT extends AbstractOrderServiceIT {

    @Autowired
    private OrderMaintenanceService orderMaintenanceService;

    private static final String CORRELATION_ID = "myCorrelationId";

    private static final String ORDER_OWNER = "owner";

    @Test
    public void test_whenOrderFinishedThenAmqpCorrectlySent() throws ModuleException, InterruptedException {
        // GIVEN
        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("simple");
        basketRepository.save(basket);
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.when(authenticationResolver.getUser()).thenReturn(ORDER_OWNER);

        // WHEN Create and run order
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240, ORDER_OWNER, CORRELATION_ID);
        simulateSchedulerMultipleTimes(1);
        waitForSubOrderDone();
        simulateSchedulerMultipleTimes(2);

        // THEN one suborder is done and ready to be downloaded (max job per user is set to 1)
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        List<OrderResponseDtoEvent> events = argumentCaptor.getAllValues()
                                                           .stream()
                                                           .filter(OrderResponseDtoEvent.class::isInstance)
                                                           .map(OrderResponseDtoEvent.class::cast)
                                                           .toList();
        Assertions.assertEquals(1, events.size());
        OrderResponseDtoEvent suborderDoneEvent = events.get(0);
        Assertions.assertEquals(OrderRequestStatus.SUBORDER_DONE, suborderDoneEvent.getStatus());
        Assertions.assertEquals(CORRELATION_ID, suborderDoneEvent.getCorrelationId());

        Mockito.reset(publisher);
        // continue execution : download files to allow next suborder to be run
        List<OrderDataFile> availableFiles = new ArrayList<>(dataFileService.findAllAvailables(order.getId()));
        simulateSchedulerMultipleTimes(1);
        orderDownloadService.downloadOrderCurrentZip(order.getOwner(), availableFiles, new OutputStreamDoNothing());
        waitForSubOrderDone(); // wait for the second suborder done
        simulateSchedulerMultipleTimes(4);
        waitForStatus(order.getId(), OrderStatus.DONE); // there is only 2 suborders, so order is done

        // THEN
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        events = argumentCaptor.getAllValues()
                               .stream()
                               .filter(OrderResponseDtoEvent.class::isInstance)
                               .map(OrderResponseDtoEvent.class::cast)
                               .toList();

        // check that we received one and only one DONE order response
        List<OrderResponseDtoEvent> mainEventList = events.stream()
                                                          .filter(ev -> ev.getStatus() == OrderRequestStatus.DONE)
                                                          .toList();
        Assertions.assertEquals(1, mainEventList.size());
        OrderResponseDtoEvent mainEvent = mainEventList.get(0);
        Assertions.assertEquals(CORRELATION_ID, mainEvent.getCorrelationId());
        Assertions.assertEquals(OrderRequestStatus.DONE, mainEvent.getStatus());
        // assure that 2 sub-order has been created
        Assertions.assertEquals(2, filesTasksRepository.countByOrderId(order.getId()));
        // thus 2 amqp message SUBORDER_DONE
        List<OrderResponseDtoEvent> suborderEvents = events.stream()
                                                           .filter(ev -> ev.getStatus()
                                                                         == OrderRequestStatus.SUBORDER_DONE)
                                                           .toList();
        Assertions.assertEquals(2, suborderEvents.size());
        Assertions.assertTrue(suborderEvents.stream().allMatch(ev -> CORRELATION_ID.equals(ev.getCorrelationId())));
        String downloadUrl = "regardsHost/api/v1/rs-order/user/orders/" + order.getId() + "/download";
        Assertions.assertEquals(downloadUrl, mainEvent.getDownloadLink());
        Assertions.assertTrue(suborderEvents.stream().allMatch(ev -> downloadUrl.equals(ev.getDownloadLink())));
    }

    @Test
    public void test_withEmptyBucket() throws EntityInvalidException, InterruptedException {
        // GIVEN
        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = new Basket(ORDER_OWNER);
        basketRepository.save(basket);
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.when(authenticationResolver.getUser()).thenReturn(ORDER_OWNER);

        // WHEN Create and run order
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240, ORDER_OWNER, CORRELATION_ID);
        simulateSchedulerMultipleTimes(1);
        waitForSubOrderDone();
        simulateSchedulerMultipleTimes(2); // schedule

        // THEN one suborder is done and ready to be downloaded (max job per user is set to 1)
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Mockito.reset(publisher);
        List<OrderResponseDtoEvent> events = argumentCaptor.getAllValues()
                                                           .stream()
                                                           .filter(OrderResponseDtoEvent.class::isInstance)
                                                           .map(OrderResponseDtoEvent.class::cast)
                                                           .toList();
        Assertions.assertEquals(1, events.size());
        OrderResponseDtoEvent orderDoneEvent = events.get(0);
        order = orderRepository.findSimpleById(order.getId());
        Assertions.assertEquals(OrderStatus.DONE_WITH_WARNING, order.getStatus());
        Assertions.assertEquals(OrderRequestStatus.FAILED, orderDoneEvent.getStatus());
        Assertions.assertEquals(CORRELATION_ID, orderDoneEvent.getCorrelationId());
    }

    private void simulateSchedulerMultipleTimes(int times) {
        IntStream.range(0, times).forEach(i -> {
            try {
                // method calls by the order scheduler
                orderMaintenanceService.updateTenantOrdersComputations();
            } catch (Exception e) {
                LOGGER.warn("An error occurred in scheduled method");
            }
        });
    }

    private void waitForSubOrderDone() throws InterruptedException {
        // "suborder done" means suborder's files is ready to download
        Thread.sleep(3000);
        // sleep is not pretty but makes the wait easier.
        // otherwise, it would be necessary to rethink the management of orders
    }

    static class OutputStreamDoNothing extends OutputStream {

        @Override
        public void write(int b) {
            // Nothing to do
        }

    }
}
