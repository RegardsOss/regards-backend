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
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.dto.TestNotificationMetadata;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.service.flow.NotificationRequestEventHandler;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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
                                   "regards.amqp.enabled=true" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class NotificationRequestEventHandlerIT extends AbstractNotificationMultitenantServiceIT {

    @Autowired
    private NotificationRequestEventHandler notificationRequestEventHandler;

    @Test
    public void test_duplicatedRequestIdSameBatch() throws InterruptedException {
        // GIVEN 3 requests with same request id
        List<NotificationRequestEvent> notificationEvents = createNotificationEvents(3);
        for (NotificationRequestEvent notificationEvent : notificationEvents) {
            notificationEvent.setRequestId("UniqueRequestId");
        }
        ArgumentCaptor<NotifierEvent> captorPublished = ArgumentCaptor.forClass(NotifierEvent.class);
        // WHEN publishing these requests
        Mockito.reset(publisher);
        notificationRequestEventHandler.handleBatch(notificationEvents);
        Mockito.verify(publisher, Mockito.timeout(5000L).times(1)).publish(captorPublished.capture());
        List<NotifierEvent> allValues = captorPublished.getAllValues();
        long countDenied = allValues.stream()
                                    .map(NotifierEvent::getState)
                                    .filter(NotificationState.DENIED::equals)
                                    .count();
        long countGranted = allValues.stream()
                                     .map(NotifierEvent::getState)
                                     .filter(NotificationState.GRANTED::equals)
                                     .count();
        // THEN only the first requests is well managed
        Assert.assertEquals(0, countDenied);
        // The first request is granted
        Assert.assertEquals(1, countGranted);
    }

    @Test
    public void test_duplicatedRequestIdDifferentBatch() throws InterruptedException {
        // GIVEN 2 requests with same request id
        List<NotificationRequestEvent> notificationEvents = createNotificationEvents(2);
        for (NotificationRequestEvent notificationEvent : notificationEvents) {
            notificationEvent.setRequestId("UniqueRequestId");
        }
        ArgumentCaptor<NotifierEvent> captorPublished = ArgumentCaptor.forClass(NotifierEvent.class);
        // WHEN publishing the first request
        Mockito.reset(publisher);
        notificationRequestEventHandler.handleBatch(List.of(notificationEvents.get(0)));
        Mockito.verify(publisher, Mockito.times(1)).publish(captorPublished.capture());
        List<NotifierEvent> allValues = captorPublished.getAllValues();
        // THEN the first request is granted
        Assert.assertEquals(1, allValues.size());
        Assert.assertEquals(NotificationState.GRANTED, allValues.get(0).getState());

        // WHEN publishing another request with same id (request is not managed because scheduler is not active in test)
        Mockito.reset(publisher);
        notificationRequestEventHandler.handleBatch(List.of(notificationEvents.get(1)));
        Mockito.verify(publisher, Mockito.never()).publish(captorPublished.capture());
        // THEN publish is not called because new event is not managed, request id must be unique
        Assert.assertEquals(1, notificationRequestRepository.count());
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
