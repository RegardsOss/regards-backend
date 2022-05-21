/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.service.OrderCreationService;
import fr.cnes.regards.modules.order.service.OrderServiceTestIT;
import fr.cnes.regards.modules.order.service.job.ProcessExecutionJob;
import fr.cnes.regards.modules.order.service.job.StorageFilesJob;
import fr.cnes.regards.modules.processing.forecast.MultiplierResultSizeForecast;
import fr.cnes.regards.modules.processing.order.*;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=order_processing_test_it_scope_item", })
public class OrderProcessingServiceIT extends AbstractOrderProcessingServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceTestIT.class);

    OrderProcessInfoMapper processInfoMapper = new OrderProcessInfoMapper();

    @Before
    public void initProjectUser() {
        Role role = new Role();
        role.setName(DefaultRole.REGISTERED_USER.name());
        ProjectUser projectUser = new ProjectUser();
        projectUser.setRole(role);
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(projectUser), HttpStatus.OK));
    }

    @Test
    public void runAllTestsInSingleInstance() throws Exception {
        // TODO: one day, split this test into more @Test methods.
        // Right now, it is not the case because the tests refuse to run
        // when launched in succession (they work when run independently)
        // for different reasons depending on how the tests are split:
        // - setting the DirtiesContext to after each method,
        // - splitting into several classes with only one method per test class,
        // - setting failsafe.reuseForks, all that failed.
        // For now, the only reliable way to make all three tests run
        // successfully in sequence is to have them in the same spring context
        // and in the same @Test method.

        simpleOrderWithProcessItemFiles();
        clean();

        simpleOrderWithProcessSuborderExecution();
        clean();

        simpleOrderWithProcessSuborderFeatures();
        clean();
    }

    private void simpleOrderWithProcessItemFiles() throws Exception {
        // The important params are those:
        OrderProcessInfo processInfo = new OrderProcessInfo(Scope.FEATURE,
                                                            Cardinality.ONE_PER_INPUT_FILE,
                                                            List.of(DataType.RAWDATA),
                                                            new SizeLimit(SizeLimit.Type.FILES, 1L),
                                                            new MultiplierResultSizeForecast(1d),
                                                            Boolean.FALSE);
        launchOrderAndExpectResults(processInfo, 10, Collections.singletonList(OrderStatus.RUNNING));
    }

    private void simpleOrderWithProcessSuborderFeatures() throws Exception {
        // The important params are those:
        OrderProcessInfo processInfo = new OrderProcessInfo(Scope.SUBORDER,
                                                            Cardinality.ONE_PER_FEATURE,
                                                            List.of(DataType.RAWDATA),
                                                            new SizeLimit(SizeLimit.Type.FILES, 4L),
                                                            new MultiplierResultSizeForecast(1d),
                                                            Boolean.FALSE);
        launchOrderAndExpectResults(processInfo, 3, Collections.singletonList(OrderStatus.RUNNING));
    }

    private void simpleOrderWithProcessSuborderExecution() throws Exception {
        // The important params are those:
        OrderProcessInfo processInfo = new OrderProcessInfo(Scope.SUBORDER,
                                                            Cardinality.ONE_PER_EXECUTION,
                                                            List.of(DataType.RAWDATA),
                                                            new SizeLimit(SizeLimit.Type.FILES, 5L),
                                                            new MultiplierResultSizeForecast(1d),
                                                            Boolean.FALSE);
        launchOrderAndExpectResults(processInfo, 2, Collections.singletonList(OrderStatus.RUNNING));
    }

    @Test
    public void simpleOrderWithProcessSuborderExecutionWithForbidSplitValid() throws Exception {
        // The important params are those:
        OrderProcessInfo processInfo = new OrderProcessInfo(Scope.SUBORDER,
                                                            Cardinality.ONE_PER_EXECUTION,
                                                            List.of(DataType.RAWDATA),
                                                            new SizeLimit(SizeLimit.Type.FEATURES, 10L),
                                                            new MultiplierResultSizeForecast(1d),
                                                            Boolean.TRUE);
        launchOrderAndExpectResults(processInfo, 1, Collections.singletonList(OrderStatus.RUNNING));
    }

    @Test
    public void simpleOrderWithProcessSuborderExecutionWithForbidSplitInvalid() throws Exception {
        // The important params are those:
        OrderProcessInfo processInfo = new OrderProcessInfo(Scope.SUBORDER,
                                                            Cardinality.ONE_PER_EXECUTION,
                                                            List.of(DataType.QUICKLOOK_SD),
                                                            new SizeLimit(SizeLimit.Type.FEATURES, 9L),
                                                            new MultiplierResultSizeForecast(1d),
                                                            Boolean.TRUE);
        launchOrderAndExpectResults(processInfo, 0, Collections.singletonList(OrderStatus.FAILED));
    }

    private void launchOrderAndExpectResults(OrderProcessInfo processInfo,
                                             int expectedExecutions,
                                             java.util.List<OrderStatus> expectedOrderStatus)
        throws EntityInvalidException, InterruptedException, IOException {
        //########################
        //######## GIVE
        // These parameters are necessary for tests but do not define the test behaviour:
        UUID processBusinessId = UUID.randomUUID();
        Map<String, String> processParameters = HashMap.of("param", "value").toJavaMap();
        String defaultTenant = getDefaultTenant();
        tenantResolver.forceTenant(defaultTenant);
        ProcessingMock processingMock = new ProcessingMock(runtimeTenantResolver,
                                                           publisher,
                                                           taskExecutor,
                                                           batchCorrelations);
        AtomicInteger sendProcessingRequestCallCount = new AtomicInteger();
        String orderOwner = randomLabel("simpleOrder");
        Basket basket = createBasket(orderOwner, defaultTenant, processBusinessId, processParameters);

        CountDownLatch orderCreatedLatch = new CountDownLatch(1);
        CountDownLatch receivedExecutionResultsLatch = new CountDownLatch(expectedExecutions);

        setupMocksAndHandlers(processBusinessId,
                              processInfoMapper,
                              processInfo,
                              defaultTenant,
                              processingMock,
                              sendProcessingRequestCallCount,
                              orderOwner,
                              orderCreatedLatch,
                              receivedExecutionResultsLatch);

        Long storageJobsSuccessBefore = jobInfoRepos.countByClassNameAndStatusStatusIn(StorageFilesJob.class.getName(),
                                                                                       JobStatus.SUCCEEDED);
        Long execJobsSuccessBefore = jobInfoRepos.countByClassNameAndStatusStatusIn(ProcessExecutionJob.class.getName(),
                                                                                    JobStatus.SUCCEEDED);

        //########################
        //######## WHEN
        Order order = orderService.createOrder(basket, orderOwner, "http://frontend.com", 240);
        LOGGER.info("Order has been created !!");

        //########################
        //######## THEN
        awaitLatches(orderCreatedLatch, receivedExecutionResultsLatch);

        assertProcessingEventSizes(expectedExecutions, processingMock, storageJobsSuccessBefore, execJobsSuccessBefore);

        if (expectedExecutions > 0) {
            showMetalink(order);
        }

        List<ExecResultHandlerResultEvent> execResultEvents = execResultHandlerResultEventHandler.getEvents();
        assertThat(execResultEvents).hasSize(expectedExecutions);

        // check the status of the event processed
        java.util.List<OrderCreationService.OrderCreationCompletedEvent> orderExecutions = orderCreationCompletedEventHandler.getEvents()
                                                                                                                             .asJava();
        assertThat(orderExecutions).isNotEmpty();
        assertTrue("Unexpected status after order execution",
                   expectedOrderStatus.contains(orderExecutions.get(0).getOrder().getStatus()));
    }

}