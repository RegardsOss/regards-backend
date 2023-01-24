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

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.integration.test.job.AbstractMultitenantServiceWithJobIT;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.order.amqp.input.OrderRequestDtoEvent;
import fr.cnes.regards.modules.order.amqp.output.OrderRequestResponseDtoEvent;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.service.commons.OrderCreationCompletedEventHandler;
import fr.cnes.regards.modules.order.service.commons.OrderRequestResponseEventHandler;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static fr.cnes.regards.modules.order.service.request.OrderRequestTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Test for {@link OrderRequestEventHandler}. <br/>
 * The purpose of this test is to create orders in success or failure with the publishing of
 * {@link OrderRequestDtoEvent}s
 * <p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #create_order_success()}</li>
 *    </ul></li>
 *  <li>Error cases :
 *    <ul>
 *      <li>{@link #create_order_unreachable_catalog()}</li>
 *      <li>{@link #create_order_invalid_requests()}</li>
 *    </ul></li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@ActiveProfiles(value = { "default", "test", "testAmqp", "noscheduler", "nojobs" }, inheritProfiles = false)
@ContextConfiguration(classes = ServiceConfiguration.class)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=order_request_handler_test_it",
    "regards.amqp.enabled=true" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrderRequestEventHandlerIT extends AbstractMultitenantServiceWithJobIT {

    /**
     * Class under test
     */
    @SpyBean
    private OrderRequestEventHandler requestHandler;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IAuthenticationResolver authenticationResolver;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IBasketRepository basketRepository;

    @Autowired
    private IProjectUsersClient projectUsersClient;

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private OrderCreationCompletedEventHandler completedEventHandler;

    @SpyBean
    private IEmailClient emailClient;

    @SpyBean
    private OrderRequestResponseEventHandler responseHandler;

    @SpyBean
    private IComplexSearchClient searchClient;

    @Before
    public void init() throws Exception {
        mockServices();
        cleanRepositories();
        simulateStartEvents();

    }

    @Test
    public void create_order_success() {
        // --- GIVEN ---
        int nbOrders = 2;
        List<OrderRequestDtoEvent> validOrderRequests = createValidOrderRequestEvents(nbOrders);
        completedEventHandler.setConsumer(orderCompletedEvent -> new CountDownLatch(nbOrders).countDown());

        // --- WHEN ---
        publisher.publish(validOrderRequests);
        Mockito.verify(requestHandler, Mockito.timeout(10000)).handleBatch(any());

        // --- THEN ---
        // Wait for job and order to be executed / completed in running status
        awaitForJobAndOrderAsyncCompletion(nbOrders, OrderStatus.RUNNING);

        // Check order response messages granted
        ArgumentCaptor<List<OrderRequestResponseDtoEvent>> responseCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(responseCaptor.capture());
        checkOrderRequestResponses(nbOrders, responseCaptor.getAllValues().get(1), OrderRequestStatus.GRANTED, null);

        // check no mail was sent
        Mockito.verifyNoInteractions(emailClient);

        // check completed event
        assertThat(completedEventHandler.getEvents()).hasSize(nbOrders);
    }

    @Test
    public void create_order_unreachable_catalog() {
        // --- GIVEN ---
        int nbOrders = 2;
        List<OrderRequestDtoEvent> validOrderRequests = createValidOrderRequestEvents(nbOrders);
        completedEventHandler.setConsumer(orderCompletedEvent -> new CountDownLatch(nbOrders).countDown());
        // throw an exception during the catalog search to make the order fail
        Mockito.doThrow(new RsRuntimeException("expected exception")).when(searchClient).searchDataObjects(any());

        Project project = new Project();
        project.setHost("test-host:666");
        EntityModel entityModel = EntityModel.of(project);
        ResponseEntity responseEntity = new ResponseEntity<>(entityModel, HttpStatus.ACCEPTED);
        Mockito.doReturn(responseEntity).when(projectsClient).retrieveProject(any());

        // --- WHEN ---
        publisher.publish(validOrderRequests);
        Mockito.verify(requestHandler, Mockito.timeout(100000)).handleBatch(any());

        // --- THEN ---
        // Wait for job and order to be executed / completed in fail status
        awaitForJobAndOrderAsyncCompletion(nbOrders, OrderStatus.FAILED);

        // check that order response messages are still granted because they were sent before the completion of
        // the order, which is asynchronous.
        ArgumentCaptor<List<OrderRequestResponseDtoEvent>> responseCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(responseCaptor.capture());
        checkOrderRequestResponses(nbOrders, responseCaptor.getAllValues().get(1), OrderRequestStatus.GRANTED, null);

        // check no mail was sent
        Mockito.verifyNoInteractions(emailClient);

        // check completed event were sent
        assertThat(completedEventHandler.getEvents()).hasSize(nbOrders);
    }

    @Test
    public void create_order_invalid_requests() {
        // --- GIVEN ---
        int nbOrders = 3;
        List<OrderRequestDtoEvent> invalidRequests = createInvalidOrderRequestEvents(nbOrders);

        // --- WHEN ---
        publisher.publish(invalidRequests);
        Mockito.verify(requestHandler, Mockito.timeout(10000).times(nbOrders)).validate(any());

        // --- THEN ---
        // check that OrderRequestResponseDtoEvents are sent with denied status
        ArgumentCaptor<List<OrderRequestResponseDtoEvent>> responseCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(responseHandler, Mockito.timeout(15000)).handleBatch(responseCaptor.capture());
        for (OrderRequestResponseDtoEvent responseEvent : responseCaptor.getValue()) {
            assertThat(responseEvent.getStatus()).isEqualTo(OrderRequestStatus.DENIED);
            assertThat(responseEvent.getMessage()).isNotBlank();
        }
    }

    // -------------
    // UTILS METHODS
    // -------------

    private void mockServices() {
        Mockito.when(authenticationResolver.getUser()).thenReturn(TEST_USER_ORDER);
        Mockito.when(authenticationResolver.getRole()).thenReturn(TEST_USER_ROLE.toString());
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(anyString()))
               .thenReturn(ResponseEntity.ok()
                                         .body(EntityModel.of(new ProjectUser("test@test.fr",
                                                                              new Role(DefaultRole.EXPLOIT.toString()),
                                                                              null,
                                                                              null))));
    }

    private void cleanRepositories() {
        orderRepository.deleteAll();
        basketRepository.deleteAll();
        jobInfoRepository.deleteAll();
        completedEventHandler.clear();
        Mockito.reset(publisher);
    }

    private void simulateStartEvents() {
        // simulate application started and ready
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
    }

    private void awaitForJobAndOrderAsyncCompletion(int nbOrders, OrderStatus finalOrderStatuses) {
        // Wait for CreateOrderJob creation
        try {
            Awaitility.await().atMost(Duration.of(1000, ChronoUnit.MILLIS)).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                List<JobInfo> createdJob = this.getJobTestUtils().retrieveFullJobInfos(CreateOrderJob.class);
                return createdJob.size() == 1 && createdJob.get(0).getStatus().getStatus() == JobStatus.QUEUED;
            });
        } catch (Exception e) {
            Assertions.fail("1 CreatedOrderJob should be present with status QUEUED.", e);
        }

        // Wait for CreateOrderJob completion
        this.getJobTestUtils().runAndWaitJob(this.getJobTestUtils().retrieveFullJobInfos(CreateOrderJob.class), 10);

        // Wait for order statuses to be in final expected state
        try {
            Awaitility.await().atMost(Duration.of(30000, ChronoUnit.MILLIS)).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return orderRepository.findAll(Sort.by("id"))
                                      .stream()
                                      .filter(order -> order.getStatus() == finalOrderStatuses)
                                      .toList()
                                      .size() == nbOrders;
            });
        } catch (Exception e) {
            Assertions.fail(String.format("Orders should be in %s states but ended in %s states",
                                          finalOrderStatuses,
                                          orderRepository.findAll().stream().map(Order::getStatus).toList()), e);
        }
    }

}