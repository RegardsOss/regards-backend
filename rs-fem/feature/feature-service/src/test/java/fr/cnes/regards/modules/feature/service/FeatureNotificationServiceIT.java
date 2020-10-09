/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureManagementAction;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import static org.junit.Assert.assertEquals;

/**
 * Test for {@link FeatureNotificationRequestEvent} publishing
 * @author Kevin Marchois
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_notif", "regards.amqp.enabled=true" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties",
                "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class FeatureNotificationServiceIT extends AbstractFeatureMultitenantServiceTest {

    @Autowired
    private IFeatureNotificationService notificationService;

    @SpyBean
    private IPublisher publisher;

    @Autowired
    private Gson gson;

    @Captor
    private ArgumentCaptor<List<NotificationRequestEvent>> recordsCaptor;

    @Test
    public void testNotification() {

        // mock the publish method to not broke other tests
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));

        // use it only to initialize Feature
        List<FeatureCreationRequestEvent> list = initFeatureCreationRequestEvent(2, true);
        list.get(0).getFeature().setUrn(FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                                   EntityType.DATA,
                                                                                   "tenant",
                                                                                   1));
        list.get(1).getFeature().setUrn(FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                                   EntityType.DATA,
                                                                                   "tenant",
                                                                                   1));
        FeatureEntity createdEntity = FeatureEntity
                .build("moi", "session", list.get(0).getFeature(), null, list.get(0).getFeature().getModel());
        FeatureEntity updatedEntity = FeatureEntity
                .build("moi", "session", list.get(1).getFeature(), null, list.get(1).getFeature().getModel());
        updatedEntity.setLastUpdate(OffsetDateTime.now().plusSeconds(1));

        createdEntity.setUrn(list.get(0).getFeature().getUrn());
        updatedEntity.setUrn(list.get(1).getFeature().getUrn());

        // to skip creation and update process
        this.featureRepo.save(createdEntity);
        this.featureRepo.save(updatedEntity);

        this.publisher.publish(FeatureNotificationRequestEvent.build("notifier", createdEntity.getUrn(), PriorityLevel.LOW));
        this.publisher.publish(FeatureNotificationRequestEvent.build("notifier", updatedEntity.getUrn(), PriorityLevel.LOW));

        this.waitRequest(notificationRequestRepo, 2, 30000);
        assertEquals(2, notificationService.sendToNotifier());
        //simulate that notification has been handle with success
        Page<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(
                FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED,
                OffsetDateTime.now().plusDays(1),
                PageRequest.of(0, 2, Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("requestDate"))));
        featureNotificationService.handleNotificationSuccess(requestsToSend.toSet());
        this.waitRequest(notificationRequestRepo, 0, 30000);

        Mockito.verify(publisher).publish(recordsCaptor.capture());
        // the first publish message to be intercepted must be the creation of createdEntity
        assertEquals(gson.toJson(new CreateNotificationActionEventVisitor.NotificationActionEventMetadata(
                FeatureManagementAction.NOTIFIED)), recordsCaptor.getValue().get(0).getMetadata());
        assertEquals(gson.toJson(createdEntity.getFeature()), recordsCaptor.getValue().get(0).getPayload().toString());
        // the second message is the update of updatedEntity
        assertEquals(gson.toJson(new CreateNotificationActionEventVisitor.NotificationActionEventMetadata(
                FeatureManagementAction.NOTIFIED)), recordsCaptor.getValue().get(1).getMetadata());
        assertEquals(gson.toJson(updatedEntity.getFeature()), recordsCaptor.getValue().get(1).getPayload().toString());

    }
}
