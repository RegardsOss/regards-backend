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
package fr.cnes.regards.modules.feature.service.request;

import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureUpdateDisseminationRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationInfoType;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationRequest;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceIT;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import fr.cnes.regards.modules.notifier.dto.out.RecipientStatus;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_notifier_listener",
                                   "regards.amqp.enabled=true",
                                   "spring.task.scheduling.pool.size=2",
                                   "regards.feature.metrics.enabled=true" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles({ "testAmqp", "noscheduler", "noFemHandler" })
public class FeatureNotifierListenerIT extends AbstractFeatureMultitenantServiceIT {

    @Autowired
    private IFeatureCreationRequestRepository featureCreationRequestRepository;

    @Autowired
    private FeatureNotifierListener featureNotifierListener;

    @Autowired
    private IFeatureUpdateDisseminationRequestRepository featureUpdateDisseminationRequestRepository;

    private boolean isToNotify;

    @Override
    @Before
    public void before() throws Exception {
        super.before();
        this.isToNotify = initDefaultNotificationSettings();
    }

    @Test
    public void testReceivingNotifierEventThatCreatesFeatureUpdateDisseminant() {
        // Given
        initData(1);

        assertEquals(1, featureCreationRequestRepository.count());
        mockStorageHelper.mockFeatureCreationStorageSuccess();

        List<AbstractFeatureRequest> abstractFeatureRequests = mockNotificationSent();

        assertFalse("should retrieve request", abstractFeatureRequests.isEmpty());

        String requestId = abstractFeatureRequests.stream().findFirst().get().getRequestId();
        String requestOwner = abstractFeatureRequests.stream().findFirst().get().getRequestOwner();
        String recipientLabelRequired = "recipientLabelRequired";
        String recipientLabelNotRequired = "recipientLabelNotRequired";
        HashSet<Recipient> recipients = Sets.newLinkedHashSet(new Recipient(recipientLabelRequired,
                                                                            RecipientStatus.SUCCESS,
                                                                            true,
                                                                            true),
                                                              new Recipient(recipientLabelNotRequired,
                                                                            RecipientStatus.SUCCESS,
                                                                            false,
                                                                            false));
        List<NotifierEvent> notifierEvents = Lists.newArrayList(new NotifierEvent(requestId,
                                                                                  requestOwner,
                                                                                  NotificationState.SUCCESS,
                                                                                  recipients,
                                                                                  OffsetDateTime.now()));
        // When
        featureNotifierListener.onRequestSuccess(notifierEvents);

        // Then
        // the FeatureCreationRequest must be deleted
        assertEquals(0, featureCreationRequestRepository.count());

        List<FeatureUpdateDisseminationRequest> featureUpdateDisseminationRequests = featureUpdateDisseminationRequestRepository.findAll();
        assertEquals("should have two update dissemination requests", 2, featureUpdateDisseminationRequests.size());
        Optional<FeatureUpdateDisseminationRequest> recipientLabelRequiredRequestOpt = featureUpdateDisseminationRequests.stream()
                                                                                                                         .filter(
                                                                                                                             request -> request.getRecipientLabel()
                                                                                                                                               .equals(
                                                                                                                                                   recipientLabelRequired))
                                                                                                                         .findFirst();
        Optional<FeatureUpdateDisseminationRequest> recipientLabelNotRequiredRequestOpt = featureUpdateDisseminationRequests.stream()
                                                                                                                            .filter(
                                                                                                                                request -> request.getRecipientLabel()
                                                                                                                                                  .equals(
                                                                                                                                                      recipientLabelNotRequired))
                                                                                                                            .findFirst();

        assertTrue("should retrieve corresponding event", recipientLabelRequiredRequestOpt.isPresent());
        assertTrue("should retrieve corresponding event", recipientLabelNotRequiredRequestOpt.isPresent());
        FeatureUpdateDisseminationRequest recipientLabelRequiredRequest = recipientLabelRequiredRequestOpt.get();
        FeatureUpdateDisseminationRequest recipientLabelNotRequiredRequest = recipientLabelNotRequiredRequestOpt.get();
        assertEquals("should get good mode",
                     FeatureUpdateDisseminationInfoType.PUT,
                     recipientLabelRequiredRequest.getUpdateType());
        assertEquals("should get good mode",
                     FeatureUpdateDisseminationInfoType.PUT,
                     recipientLabelNotRequiredRequest.getUpdateType());
        assertTrue("should ack be required", recipientLabelRequiredRequest.getAckRequired());
        assertFalse("should ack not be required", recipientLabelNotRequiredRequest.getAckRequired());

        assertEquals("should retrieve feature urn",
                     abstractFeatureRequests.get(0).getUrn(),
                     recipientLabelRequiredRequest.getUrn());
        assertEquals("should retrieve feature urn",
                     abstractFeatureRequests.get(0).getUrn(),
                     recipientLabelNotRequiredRequest.getUrn());
    }

    protected List<AbstractFeatureRequest> mockNotificationSent() {
        List<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo.findAll();
        if (!requestsToSend.isEmpty()) {
            featureNotificationService.sendToNotifier();
        }
        return requestsToSend;
    }
}