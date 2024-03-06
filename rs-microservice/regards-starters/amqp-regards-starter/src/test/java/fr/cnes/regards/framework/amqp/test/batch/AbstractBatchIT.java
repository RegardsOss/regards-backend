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
package fr.cnes.regards.framework.amqp.test.batch;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.*;
import fr.cnes.regards.framework.amqp.event.IMessagePropertiesAware;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.test.batch.domain.ResponseTestedMessage;
import fr.cnes.regards.framework.amqp.test.batch.domain.TestedMessage;
import fr.cnes.regards.framework.amqp.test.batch.mock.TestBatchHandler;
import fr.cnes.regards.framework.amqp.test.batch.mock.TestBatchHandlerWithResponses;
import fr.cnes.regards.framework.amqp.test.batch.mock.TestBatchHandlerWithRetry;
import fr.cnes.regards.framework.amqp.test.batch.mock.TestResponseBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

/**
 * Abstract to prepare tests for batch of AMQP messages.
 *
 * @author Iliana Ghazali
 **/
@SpringJUnitConfig
@EnableAutoConfiguration
@TestPropertySource(properties = { "regards.amqp.management.mode=SINGLE",
                                   "regards.amqp.internal.transaction=true",
                                   "spring.jmx.enabled=false" },
                    locations = { "classpath:amqp.properties",
                                  "classpath:application.properties",
                                  "classpath:retry.properties" })
@ActiveProfiles("test")
public abstract class AbstractBatchIT {

    // CONSTANTS

    protected static final String TEST_QUEUE_NAME = "test.queue.name";

    protected static final String TEST_EXCHANGE_NAME = "test.exchange.name";

    protected static final String TEST_EXCHANGE_WITH_RETRY_NAME = "test.retry.exchange.name";

    protected static final String TEST_QUEUE_WITH_RETRY_NAME = "test.retry.queue.name";

    protected static final String PROJECT1_TENANT = "PROJECT1";

    protected static final String FAKE_TENANT = "FAKE";

    // SERVICES

    @SpyBean
    protected IPublisher publisher;

    @SpyBean
    protected IInstancePublisher instancePublisher;

    @Autowired
    protected ISubscriber subscriber;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected IAmqpAdmin amqpAdmin;

    @Autowired
    protected IRabbitVirtualHostAdmin vhostAdmin;

    @Autowired
    protected RetryProperties retryProperties;

    @Value("${regards.tenant:PROJECT}")
    protected String defaultTenant;

    protected TestBatchHandler testBatchHandler;

    protected TestBatchHandlerWithResponses batchHandlerWithResponses;

    protected TestBatchHandlerWithRetry testBatchHandlerWithRetry;

    protected TestResponseBatchHandler testResponseBatchHandler;

    @BeforeEach
    public void beforeEachTest() {
        // New instance for each test
        testBatchHandler = new TestBatchHandler(runtimeTenantResolver);
        batchHandlerWithResponses = new TestBatchHandlerWithResponses(runtimeTenantResolver);
        testBatchHandlerWithRetry = new TestBatchHandlerWithRetry(runtimeTenantResolver);
        testResponseBatchHandler = new TestResponseBatchHandler();

        // Subscribe to message
        subscriber.subscribeTo(TestedMessage.class, testBatchHandler);
        subscriber.subscribeTo(TestedMessage.class, batchHandlerWithResponses, TEST_QUEUE_NAME, TEST_EXCHANGE_NAME);
        subscriber.subscribeTo(TestedMessage.class,
                               testBatchHandlerWithRetry,
                               TEST_QUEUE_WITH_RETRY_NAME,
                               TEST_EXCHANGE_WITH_RETRY_NAME);
        subscriber.subscribeTo(ResponseTestedMessage.class, testResponseBatchHandler);
    }

    @AfterEach
    public void afterEachTest() {
        // Unsubscribe
        subscriber.unsubscribeFrom(TestedMessage.class, true);
        subscriber.unsubscribeFrom(ResponseTestedMessage.class, true);

        // Purge tested queue
        cleanMultitenantAMQPQueue(amqpAdmin.getSubscriptionQueueName(TestBatchHandler.class,
                                                                     Target.ONE_PER_MICROSERVICE_TYPE));
        cleanMultitenantAMQPQueue(amqpAdmin.getSubscriptionQueueName(TestResponseBatchHandler.class,
                                                                     Target.ONE_PER_MICROSERVICE_TYPE));
        cleanMultitenantAMQPQueue(TEST_QUEUE_NAME);
        cleanMultitenantAMQPQueue(TEST_QUEUE_WITH_RETRY_NAME);
    }

    /**
     * Internal method to clean AMQP queues, if actives
     */
    private void cleanMultitenantAMQPQueue(String queueName) {
        Awaitility.await().atMost(Durations.FIVE_SECONDS).until(() -> {
            try {
                vhostAdmin.bind(AmqpChannel.AMQP_MULTITENANT_MANAGER);
                amqpAdmin.purgeQueue(queueName, false);
                return amqpAdmin.isQueueEmpty(queueName);
            } finally {
                vhostAdmin.unbind();
            }
        });
    }

    protected List<String> getMessagesRequestIds(List<? extends IMessagePropertiesAware> messages) {
        return messages.stream().map(this::getMessageRequestId).toList();
    }

    protected String getMessageRequestId(IMessagePropertiesAware message) {
        return message.getMessageProperties().getHeader(AmqpConstants.REGARDS_REQUEST_ID_HEADER);
    }
}
