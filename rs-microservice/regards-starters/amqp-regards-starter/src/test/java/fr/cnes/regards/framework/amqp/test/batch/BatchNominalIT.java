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
package fr.cnes.regards.framework.amqp.test.batch;

import fr.cnes.regards.framework.amqp.test.batch.domain.TestedMessage;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Nominal tests to verify the receiving of AMQP messages by batch.
 * <p>When messages are published, they should be received on the corresponding batch handler, which was mounted
 * automatically. Sending of messages can be retried in case of failure if the option was enabled.</p>
 * TEST PLAN :
 * <ul>
 *   <li>{@link this#givenValidMessageBatch_whenPublished_thenReceived()}</li>
 *   <li>{@link this#givenValidMessageBatch_whenPublishedOnNamedExchange_thenReceived()}</li>
 *   <li>{@link this#givenValidMessageBatch_whenPublishedWithRetry_thenReceived()} ()}</li>
 * </ul>
 *
 * @author Iliana Ghazali
 */
class BatchNominalIT extends AbstractBatchIT {

    @Test
    void givenValidMessageBatch_whenPublished_thenReceived() {
        // GIVEN
        Map<String, List<TestedMessage>> validMessagesToPublish = Map.of(defaultTenant,
                                                                         List.of(TestedMessage.buildValidMessage(),
                                                                                 TestedMessage.buildValidMessage()),
                                                                         PROJECT1_TENANT,
                                                                         List.of(TestedMessage.buildValidMessage()));

        // WHEN
        // Publish messages to default projet
        publisher.publish(validMessagesToPublish.get(defaultTenant));
        // Publish messages to project 1
        try {
            runtimeTenantResolver.forceTenant(PROJECT1_TENANT);
            publisher.publish(validMessagesToPublish.get(PROJECT1_TENANT));
        } finally {
            runtimeTenantResolver.clearTenant();
        }

        // THEN
        int expectedNbCalls = 2;
        Map<String, Integer> expectedMessagesByTenant = Map.of(defaultTenant,
                                                               validMessagesToPublish.get(defaultTenant).size(),
                                                               PROJECT1_TENANT,
                                                               validMessagesToPublish.get(PROJECT1_TENANT).size());

        Awaitility.await().atMost(Durations.TEN_SECONDS).pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            Assertions.assertThat(testBatchHandler.getHandleCalls()).isEqualTo(expectedNbCalls);
            Assertions.assertThat(testBatchHandler.getValidCount())
                      .containsExactlyInAnyOrderEntriesOf(expectedMessagesByTenant);
        });
    }

    @Test
    void givenValidMessageBatch_whenPublishedOnNamedExchange_thenReceived() {
        // GIVEN
        Map<String, List<TestedMessage>> validMessagesToPublish = Map.of(defaultTenant,
                                                                         List.of(TestedMessage.buildValidMessage()),
                                                                         PROJECT1_TENANT,
                                                                         List.of(TestedMessage.buildValidMessage()));

        // WHEN
        // Publish messages to default projet
        publisher.publish(validMessagesToPublish.get(defaultTenant), TEST_EXCHANGE_NAME, Optional.empty());
        // Publish messages to project 1
        try {
            runtimeTenantResolver.forceTenant(PROJECT1_TENANT);
            publisher.publish(validMessagesToPublish.get(PROJECT1_TENANT), TEST_EXCHANGE_NAME, Optional.empty());
        } finally {
            runtimeTenantResolver.clearTenant();
        }

        // THEN
        int expectedNbCalls = 2;
        Map<String, Integer> expectedMessagesByTenant = Map.of(defaultTenant,
                                                               validMessagesToPublish.get(defaultTenant).size(),
                                                               PROJECT1_TENANT,
                                                               validMessagesToPublish.get(PROJECT1_TENANT).size());

        Awaitility.await().atMost(Durations.TEN_SECONDS).pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            Assertions.assertThat(batchHandlerWithResponses.getHandleCalls()).isEqualTo(expectedNbCalls);
            Assertions.assertThat(batchHandlerWithResponses.getValidCount())
                      .containsExactlyInAnyOrderEntriesOf(expectedMessagesByTenant);
        });
    }

    @Test
    void givenValidMessageBatch_whenPublishedWithRetry_thenReceived() {
        // GIVEN
        List<TestedMessage> validMessagesToPublish = List.of(TestedMessage.buildTemporaryUnexpectedExceptionMessage(),
                                                             TestedMessage.buildTemporaryUnexpectedExceptionMessage());

        // WHEN
        // Publish messages to default projet
        publisher.publish(validMessagesToPublish, TEST_EXCHANGE_WITH_RETRY_NAME, Optional.empty());

        // THEN
        int expectedNbCalls = 2;
        Awaitility.await()
                  .atMost(Durations.TEN_SECONDS.plusSeconds(5))
                  .pollInterval(200, TimeUnit.MILLISECONDS)
                  .untilAsserted(() -> {
                      Assertions.assertThat(testBatchHandlerWithRetry.getHandleCalls()).isEqualTo(expectedNbCalls);
                      Assertions.assertThat(testBatchHandlerWithRetry.getValidCountByTenant(defaultTenant))
                                .isEqualTo(validMessagesToPublish.size());
                  });
    }

}
