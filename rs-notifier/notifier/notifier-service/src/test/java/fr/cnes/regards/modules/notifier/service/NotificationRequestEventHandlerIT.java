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
package fr.cnes.regards.modules.notifier.service;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.dto.TestNotificationMetadata;
import fr.cnes.regards.modules.notifier.service.flow.NotificationRequestEventHandler;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link NotificationRequestEventHandler} to verify the retry system.
 * <p>{@link NotificationRequest}s should be created after receiving AMQP events with retry.</p>
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema= notification_handler_it",
                                   "regards.amqp.enabled=true" },
                    locations = { "classpath:retry.properties", "classpath:batch.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class NotificationRequestEventHandlerIT extends AbstractNotificationMultitenantServiceIT {

    @MockBean
    private INotificationRequestRepository notificationRequestRepo;

    @Test
    public void givenValidUpdateEvents_whenPublishedWithRetry_thenRequestsCreated() {
        // GIVEN
        int nbEvents = 3;
        // Simulate temporary exception to activate the retry of messages
        Mockito.when(notificationRequestRepo.saveAll(ArgumentMatchers.any()))
               .thenThrow(new DataAccessResourceFailureException("test exception to make the batch fail on "
                                                                 + "NOTIFICATION."))
               .thenAnswer(ans -> {
                   List<NotificationRequestEvent> registeredRequests = (List<NotificationRequestEvent>) ans.getArguments()[0];
                   Assertions.assertThat(registeredRequests).hasSize(nbEvents);
                   return registeredRequests;
               });

        // WHEN
        // publish update events
        publisher.publish(createNotificationEvents(nbEvents));

        // THEN
        // Retry header has to be updated
        verifyRetryHeaderAfterXFailures(nbEvents,
                                        1,
                                        amqpAdmin.getSubscriptionQueueName(NotificationRequestEventHandler.class,
                                                                           Target.ONE_PER_MICROSERVICE_TYPE));
    }

    public List<NotificationRequestEvent> createNotificationEvents(int nbEvents) {
        List<NotificationRequestEvent> events = new ArrayList<>(nbEvents);
        JsonObject payloadMatchR2 = initElement("elementRule2.json");
        for (int i = 0; i < nbEvents; i++) {
            events.add(new NotificationRequestEvent(payloadMatchR2,
                                                    gson.toJsonTree(TestNotificationMetadata.build("value"))
                                                        .getAsJsonObject(),
                                                    AbstractRequestEvent.generateRequestId(),
                                                    "owner"));
        }
        return events;
    }

}
