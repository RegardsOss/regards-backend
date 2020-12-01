package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.service.OrderServiceTestIT;
import fr.cnes.regards.modules.processing.order.*;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.default_schema=order_processing_test_it_scope_suborder",
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrderProcessingServiceScopeSuborderIT extends AbstractOrderProcessingServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceTestIT.class);

    @Test
    public void simpleOrderWithProcess() throws Exception {
        //########################
        //######## GIVEN

        // The important params are those:
        OrderProcessInfo processInfo = new OrderProcessInfo(
                Scope.SUBORDER,
                Cardinality.ONE_PER_FEATURE,
                List.of(DataType.RAWDATA),
                new SizeLimit(SizeLimit.Type.FILES, 5L)
        );
        int expectedExecutions = 2;

        // These parameters are necessary for tests but do not define the test behaviour:
        UUID processBusinessId = UUID.randomUUID();
        Map<String, String> processParameters = HashMap.of("param", "value").toJavaMap();
        OrderProcessInfoMapper processInfoMapper  = new OrderProcessInfoMapper();
        String defaultTenant = getDefaultTenant();
        tenantResolver.forceTenant(defaultTenant);
        ProcessingMock processingMock = new ProcessingMock(runtimeTenantResolver, publisher, taskExecutor, batchCorrelations);
        AtomicInteger sendProcessingRequestCallCount = new AtomicInteger();
        String orderOwner = randomLabel("simpleOrder");
        Basket basket = createBasket(orderOwner, defaultTenant, processBusinessId, processParameters);

        CountDownLatch orderCreatedLatch = new CountDownLatch(1);
        CountDownLatch receivedExecutionResultsLatch = new CountDownLatch(expectedExecutions);

        setupMocksAndHandlers(
                processBusinessId,
                processInfoMapper,
                processInfo,
                defaultTenant,
                processingMock,
                sendProcessingRequestCallCount,
                orderOwner,
                orderCreatedLatch,
                receivedExecutionResultsLatch
        );

        //########################
        //######## WHEN
        Order order = orderService.createOrder(basket, orderOwner, "http://frontend.com");
        LOGGER.info("Order has been created !!");

        //########################
        //######## THEN
        awaitLatches(orderCreatedLatch, receivedExecutionResultsLatch);

        assertProcessingEventSizes(expectedExecutions, processingMock);

        showMetalink(order);

        List<ExecResultHandlerResultEvent> execResultEvents = execResultHandlerResultEventHandler.getEvents();
        assertThat(execResultEvents).hasSize(expectedExecutions);
    }

}