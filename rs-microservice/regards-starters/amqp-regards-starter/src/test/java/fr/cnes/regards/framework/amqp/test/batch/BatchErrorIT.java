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

import fr.cnes.regards.framework.amqp.IPublisherContract;
import fr.cnes.regards.framework.amqp.batch.RepublishErrorBatchMessageRecover;
import fr.cnes.regards.framework.amqp.batch.RetryBatchMessageHandler;
import fr.cnes.regards.framework.amqp.batch.dto.BatchMessageErrorType;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.amqp.exception.MaxRetriesReachedException;
import fr.cnes.regards.framework.amqp.test.batch.domain.ResponseTestedMessage;
import fr.cnes.regards.framework.amqp.test.batch.domain.TestRuntimeException;
import fr.cnes.regards.framework.amqp.test.batch.domain.TestedMessage;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Tests to verify the receiving of AMQP messages by batch in case of multiple errors.
 * <p>When messages are published, they should be handled differently according to the type of error detected given
 * by {@link BatchMessageErrorType}
 * .</p>
 * TEST PLAN :
 * <ul>
 *  <li>{@link BatchMessageErrorType#INVALID_MESSAGE}
 *    <ul>
 *      <li>{@link this#givenInvalidMessages_whenPublished_thenDenied()}</li>
 *      <li>{@link this#givenInvalidAndValidMessages_whenPublished_thenDeniedOrAccepted()}</li>
 *    </ul>
 *  </li>
 *  <li>{@link BatchMessageErrorType#MISSING_TENANT}
 *    <ul>
 *      <li>{@link this#givenMissingTenantMessages_whenPublished_thenDenied()} </li>
 *      <li>{@link this#givenUnknownTenantMessages_whenPublished_thenIgnored()}</li>
 *    </ul>
 *  </li>
 *  <li>{@link BatchMessageErrorType#NOT_CONVERTED_MESSAGE}
 *    <ul>
 *      <li>{@link this#givenNotConvertedMessages_whenPublished_thenDenied()} ()} </li>
 *    </ul>
 *  </li>
 *  <li>{@link BatchMessageErrorType#UNEXPECTED_BATCH_FAILURE}
 *    <ul>
 *      <li>{@link this#givenPermanentUnexpectedException_whenPublished_thenFails()} </li>
 *      <li>{@link this#givenPermanentUnexpectedExceptionWithRetry_whenPublishedMaxRetried_thenFails()}</li>
 *    </ul>
 *  </li>
 * </ul>
 *
 * @author Iliana Ghazali
 */
class BatchErrorIT extends AbstractBatchIT {

    @Test
    void givenInvalidMessages_whenPublished_thenDenied() {
        // GIVEN
        // Create invalid messages
        List<TestedMessage> invalidMessages = List.of(TestedMessage.buildInvalidMessage(),
                                                      TestedMessage.buildInvalidMessage());
        // WHEN
        // Messages are published
        publisher.publish(invalidMessages, TEST_EXCHANGE_NAME, Optional.empty());

        // THEN
        // Wait for messages to be handled as invalid
        Awaitility.await().atMost(Durations.TEN_SECONDS).pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            Assertions.assertThat(batchHandlerWithResponses.getHandleCalls()).isZero();
            int numberInvalidMessages = invalidMessages.size();
            Assertions.assertThat(batchHandlerWithResponses.getInvalidCountByTenant(defaultTenant))
                      .isEqualTo(numberInvalidMessages);
        });
        // Check error notification was sent
        checkSentNotification(BatchMessageErrorType.INVALID_MESSAGE, true);
        // Wait for error responses to be published on another queue
        Awaitility.await()
                  .atMost(Durations.TEN_SECONDS.plusSeconds(5))
                  .pollInterval(200, TimeUnit.MILLISECONDS)
                  .untilAsserted(() -> {
                      Assertions.assertThat(testResponseBatchHandler.getHandleCalls()).isEqualTo(1);
                      List<ResponseTestedMessage> responseTestedMessages = testResponseBatchHandler.getResponseTestedMessages();
                      Assertions.assertThat(getMessagesRequestIds(responseTestedMessages))
                                .containsExactlyInAnyOrderElementsOf(getMessagesRequestIds(invalidMessages));
                      responseTestedMessages.forEach(response -> Assertions.assertThat(response.getMessage())
                                                                           .contains(BatchMessageErrorType.INVALID_MESSAGE.toString()));
                  });
    }

    @Test
    void givenInvalidAndValidMessages_whenPublished_thenDeniedOrAccepted() {
        // GIVEN
        // Create invalid messages
        List<TestedMessage> invalidMessages = List.of(TestedMessage.buildValidMessage(),
                                                      TestedMessage.buildInvalidMessage());

        // WHEN
        // Messages are published
        publisher.publish(invalidMessages, TEST_EXCHANGE_NAME, Optional.empty());

        // THEN
        // Wait for second message to be handled as invalid
        Awaitility.await().atMost(Durations.TEN_SECONDS).pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            Assertions.assertThat(batchHandlerWithResponses.getHandleCalls()).isEqualTo(1);
            Assertions.assertThat(batchHandlerWithResponses.getInvalidCountByTenant(defaultTenant)).isEqualTo(1);
            Assertions.assertThat(batchHandlerWithResponses.getValidCountByTenant(defaultTenant)).isEqualTo(1);
        });
        // Check error notification was sent
        checkSentNotification(BatchMessageErrorType.INVALID_MESSAGE, true);
        // Wait for error response to be published on another queue
        Awaitility.await()
                  .atMost(Durations.TEN_SECONDS.plusSeconds(5))
                  .pollInterval(200, TimeUnit.MILLISECONDS)
                  .untilAsserted(() -> {
                      Assertions.assertThat(testResponseBatchHandler.getHandleCalls()).isEqualTo(1);
                      Assertions.assertThat(getMessagesRequestIds(testResponseBatchHandler.getResponseTestedMessages()))
                                .containsExactlyInAnyOrder(getMessageRequestId(invalidMessages.get(1)));
                  });
    }

    @Test
    void givenMissingTenantMessages_whenPublished_thenDenied() {
        // GIVEN
        List<TestedMessage> missingTenantMessages = List.of(TestedMessage.buildEmptyTenantMessage(),
                                                            TestedMessage.buildEmptyTenantMessage());

        // WHEN
        // Send messages with empty tenant header
        runtimeTenantResolver.forceTenant(PROJECT1_TENANT);
        publisher.publish(missingTenantMessages, TEST_EXCHANGE_NAME, Optional.empty());
        runtimeTenantResolver.clearTenant();

        // THEN
        // Check error notification was sent
        checkSentNotification(BatchMessageErrorType.MISSING_TENANT, false);
        // no response message should have been published
        Assertions.assertThat(batchHandlerWithResponses.getHandleCalls()).isZero();
    }

    @Test
    void givenUnknownTenantMessages_whenPublished_thenIgnored() {
        // GIVEN
        List<TestedMessage> validMessages = List.of(TestedMessage.buildValidMessage(),
                                                    TestedMessage.buildValidMessage());

        // WHEN
        // Publish first message on an unknown tenant
        try {
            runtimeTenantResolver.forceTenant(FAKE_TENANT);
            publisher.publish(validMessages.get(0), TEST_EXCHANGE_NAME, Optional.empty());
        } finally {
            runtimeTenantResolver.clearTenant();
        }
        // Publish second message on the default tenant
        publisher.publish(validMessages.get(1), TEST_EXCHANGE_NAME, Optional.empty());

        // THEN
        // Check that the second message was successfully processed.
        // The first message was simply ignored because the tenant is not known
        Awaitility.await().atMost(Durations.TEN_SECONDS).pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            Assertions.assertThat(batchHandlerWithResponses.getHandleCalls()).isEqualTo(1);
            Assertions.assertThat(batchHandlerWithResponses.getValidCount())
                      .containsExactlyInAnyOrderEntriesOf(Map.of(defaultTenant, 1));
        });
    }

    @Test
    void givenNotConvertedMessages_whenPublished_thenDenied() {
        // GIVEN
        int nbNotConvertibleMessages = 3;
        List<Message> notConvertibleMessages = new ArrayList<>(nbNotConvertibleMessages);
        for (int i = 0; i < nbNotConvertibleMessages; i++) {
            // An AMQP message with a content that is invalid
            String body = "Not a json content";
            Message message = new Message(body.getBytes(), new MessageProperties());
            message.getMessageProperties().setHeader(AmqpConstants.REGARDS_TENANT_HEADER, defaultTenant);
            message.getMessageProperties()
                   .setHeader(AmqpConstants.REGARDS_REQUEST_ID_HEADER, RandomStringUtils.randomAlphanumeric(6));
            notConvertibleMessages.add(message);
        }

        // WHEN
        // Messages are published
        notConvertibleMessages.forEach(message -> publisher.basicPublish(defaultTenant,
                                                                         TEST_EXCHANGE_NAME,
                                                                         "",
                                                                         message));

        // THEN
        // Wait for error notification to be sent
        checkSentNotification(BatchMessageErrorType.NOT_CONVERTED_MESSAGE, true);
        // Wait for error response to be published on another queue
        Awaitility.await()
                  .atMost(Durations.TEN_SECONDS.plusSeconds(5))
                  .pollInterval(200, TimeUnit.MILLISECONDS)
                  .untilAsserted(() -> {
                      Assertions.assertThat(testResponseBatchHandler.getHandleCalls()).isEqualTo(1);
                      List<ResponseTestedMessage> responseTestedMessages = testResponseBatchHandler.getResponseTestedMessages();
                      Assertions.assertThat(getMessagesRequestIds(responseTestedMessages))
                                .containsExactlyInAnyOrderElementsOf(notConvertibleMessages.stream()
                                                                                           .map(m -> (String) m.getMessageProperties()
                                                                                                               .getHeader(
                                                                                                                   AmqpConstants.REGARDS_REQUEST_ID_HEADER))
                                                                                           .toList());
                      responseTestedMessages.forEach(response -> Assertions.assertThat(response.getMessage())
                                                                           .contains(BatchMessageErrorType.NOT_CONVERTED_MESSAGE.toString()));
                  });
    }

    @Test
    void givenPermanentUnexpectedException_whenPublished_thenFails() {
        // GIVEN
        // at least one message which will trigger an unexpected exception
        List<TestedMessage> messages = List.of(TestedMessage.buildPermanentUnexpectedExceptionMessage(),
                                               TestedMessage.buildValidMessage(),
                                               TestedMessage.buildValidMessage());

        // WHEN
        // all the messages of the batch will fail as an unexpected exception is triggered
        publisher.publish(messages, TEST_EXCHANGE_NAME, Optional.empty());

        // THEN
        // handle batch should fail on first call
        Awaitility.await()
                  .atMost(Durations.TEN_SECONDS)
                  .untilAsserted(() -> Assertions.assertThat(batchHandlerWithResponses.getHandleCalls()).isEqualTo(1));
        checkSentNotification(BatchMessageErrorType.UNEXPECTED_BATCH_FAILURE, true);
        checkDeadLetterMessages(defaultTenant,
                                TEST_QUEUE_NAME,
                                messages.size(),
                                -1,
                                getMessagesRequestIds(messages),
                                false);
    }

    @Test
    void givenPermanentUnexpectedExceptionWithRetry_whenPublishedMaxRetried_thenFails() {
        // GIVEN
        // at least one message which will trigger an unexpected exception
        List<TestedMessage> messages = List.of(TestedMessage.buildPermanentUnexpectedExceptionMessage(),
                                               TestedMessage.buildValidMessage(),
                                               TestedMessage.buildValidMessage());

        // WHEN
        // all the messages of the batch will be retried with a maximum number of attempts because an unexpected
        // exception is triggered
        publisher.publish(messages, TEST_EXCHANGE_WITH_RETRY_NAME, Optional.empty());

        // THEN
        // maximum retry attempts must be reached as the handle batch keeps failing
        int expectedNumberOfRetries = retryProperties.getMaxRetries();
        Awaitility.await()
                  .atMost(20, TimeUnit.SECONDS)
                  .untilAsserted(() -> Assertions.assertThat(testBatchHandlerWithRetry.getHandleCalls())
                                                 .isEqualTo(expectedNumberOfRetries));
        checkSentNotification(BatchMessageErrorType.UNEXPECTED_BATCH_FAILURE, true);
        checkDeadLetterMessages(defaultTenant,
                                TEST_QUEUE_WITH_RETRY_NAME,
                                messages.size(),
                                expectedNumberOfRetries,
                                getMessagesRequestIds(messages),
                                true);
    }

    private void checkSentNotification(BatchMessageErrorType batchMessageErrorType, boolean tenantPresent) {
        ArgumentCaptor<? extends ISubscribable> eventCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        IPublisherContract sendingMock = tenantPresent ? publisher : instancePublisher;
        Mockito.verify(sendingMock, Mockito.timeout(10_000).atLeastOnce()).publish(eventCaptor.capture());
        List<NotificationEvent> notification = eventCaptor.getAllValues()
                                                          .stream()
                                                          .filter(event -> event instanceof NotificationEvent)
                                                          .map(event -> (NotificationEvent) event)
                                                          .toList();
        Assertions.assertThat(notification).hasSize(1);
        Assertions.assertThat(notification.get(0).getNotification().getMessage())
                  .contains(batchMessageErrorType.getLabel());
    }

    private void checkDeadLetterMessages(String tenant,
                                         String originQueueName,
                                         int expectedNumberOfDeadMessages,
                                         int maxRetries,
                                         List<String> messageRequestIds,
                                         boolean dedicatedQueue) {
        ArgumentCaptor<Message> eventCaptor = ArgumentCaptor.forClass(Message.class);
        // determine the routing key depending on if the batch enabled the dedicated DLQ option
        String routingKey = dedicatedQueue ?
            amqpAdmin.getDedicatedDLRKFromQueueName(originQueueName) :
            amqpAdmin.getDefaultDLQName();
        // Wait for the messages to be published on the DLX
        Mockito.verify(publisher, Mockito.timeout(10_000).times(expectedNumberOfDeadMessages))
               .basicPublish(ArgumentMatchers.eq(tenant),
                             ArgumentMatchers.eq(amqpAdmin.getDefaultDLXName()),
                             ArgumentMatchers.eq(routingKey),
                             eventCaptor.capture());

        // Check message headers were properly added
        List<Message> deadMessages = eventCaptor.getAllValues();
        List<String> requestIds = new ArrayList<>(expectedNumberOfDeadMessages);
        for (Message deadMessage : deadMessages) {
            Map<String, Object> headers = deadMessage.getMessageProperties().getHeaders();
            Assertions.assertThat((String) headers.get(RepublishMessageRecoverer.X_EXCEPTION_MESSAGE)).isNotBlank();
            requestIds.add((String) headers.get(AmqpConstants.REGARDS_REQUEST_ID_HEADER));
            // headers are slightly different if the retry feature was enabled
            if (maxRetries >= 0) {
                Assertions.assertThat((Integer) headers.get(RetryBatchMessageHandler.X_RETRY_HEADER))
                          .isEqualTo(maxRetries);
                Assertions.assertThat((String) headers.get(RepublishErrorBatchMessageRecover.X_EXCEPTION_STACKTRACE))
                          .contains(List.of(MaxRetriesReachedException.class.getName(),
                                            TestRuntimeException.class.getName()));
            } else {
                Assertions.assertThat((String) headers.get(RepublishErrorBatchMessageRecover.X_EXCEPTION_STACKTRACE))
                          .contains(TestRuntimeException.class.getName());
            }
        }
        // Check that all the messages were properly re-routed to the DLX with requestIds
        Assertions.assertThat(requestIds)
                  .as("Unexpected request ids headers")
                  .containsExactlyInAnyOrderElementsOf(messageRequestIds);
    }

}
