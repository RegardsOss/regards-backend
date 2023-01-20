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
package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureNotificationRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureNotificationRequestParameters;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Stephane Cortine
 */
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=feature_version", "regards.amqp.enabled=true" },
    locations = { "classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
public class SearchFeatureNotificationServiceIT extends AbstractFeatureMultitenantServiceIT {

    private final static String PROVIDER_ID0 = "provider_id0";

    private final static String PROVIDER_ID2 = "provider_id2";

    private final static String SESSION_0 = "session0";

    private final static String SESSION_OWNER_0 = "session_owner0";

    @Before
    public void init() {
        FeatureUniformResourceName URN0 = FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                                     EntityType.DATA,
                                                                                     "tenant0",
                                                                                     1);
        FeatureUniformResourceName URN1 = FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                                     EntityType.DATA,
                                                                                     "tenant1",
                                                                                     1);

        FeatureEntity feature = FeatureEntity.build(SESSION_OWNER_0,
                                                    SESSION_0,
                                                    Feature.build(PROVIDER_ID0,
                                                                  "owner",
                                                                  URN0,
                                                                  IGeometry.point(IGeometry.position(10.0, 20.0)),
                                                                  EntityType.DATA,
                                                                  featureModelName),
                                                    null,
                                                    featureModelName);
        featureRepo.save(feature);

        feature = FeatureEntity.build("owner",
                                      "session",
                                      Feature.build(PROVIDER_ID2,
                                                    "owner",
                                                    URN1,
                                                    IGeometry.point(IGeometry.position(10.0, 20.0)),
                                                    EntityType.DATA,
                                                    featureModelName),
                                      null,
                                      featureModelName);
        featureRepo.save(feature);

        FeatureNotificationRequest featureNotificationRequest0 = new FeatureNotificationRequest();
        featureNotificationRequest0.setRequestId("request_id0");
        featureNotificationRequest0.setRequestOwner("request_owner0");
        featureNotificationRequest0.setState(RequestState.GRANTED);
        featureNotificationRequest0.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureNotificationRequest0.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureNotificationRequest0.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureNotificationRequest0.setPriority(PriorityLevel.NORMAL);
        featureNotificationRequest0.setUrn(URN0);

        notificationRequestRepo.save(featureNotificationRequest0);

        FeatureNotificationRequest featureNotificationRequest1 = new FeatureNotificationRequest();
        featureNotificationRequest1.setRequestId("request_id1");
        featureNotificationRequest1.setRequestOwner("request_owner1");
        featureNotificationRequest1.setState(RequestState.GRANTED);
        featureNotificationRequest1.setRegistrationDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureNotificationRequest1.setRequestDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureNotificationRequest1.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureNotificationRequest1.setPriority(PriorityLevel.NORMAL);

        notificationRequestRepo.save(featureNotificationRequest1);

        FeatureNotificationRequest featureNotificationRequest2 = new FeatureNotificationRequest();
        featureNotificationRequest2.setRequestId("request_id2");
        featureNotificationRequest2.setRequestOwner("request_owner2");
        featureNotificationRequest2.setState(RequestState.SUCCESS);
        featureNotificationRequest2.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureNotificationRequest2.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureNotificationRequest2.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureNotificationRequest2.setPriority(PriorityLevel.NORMAL);
        featureNotificationRequest2.setUrn(URN1);

        notificationRequestRepo.save(featureNotificationRequest2);

        FeatureNotificationRequest featureNotificationRequest21 = new FeatureNotificationRequest();
        featureNotificationRequest21.setRequestId("request_id21");
        featureNotificationRequest21.setRequestOwner("request_owner21");
        featureNotificationRequest21.setState(RequestState.SUCCESS);
        featureNotificationRequest21.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureNotificationRequest21.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureNotificationRequest21.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureNotificationRequest21.setPriority(PriorityLevel.NORMAL);

        notificationRequestRepo.save(featureNotificationRequest21);

        FeatureNotificationRequest featureNotificationRequest3 = new FeatureNotificationRequest();
        featureNotificationRequest3.setRequestId("request_id3");
        featureNotificationRequest3.setRequestOwner("request_owner3");
        featureNotificationRequest3.setState(RequestState.ERROR);
        featureNotificationRequest3.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureNotificationRequest3.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureNotificationRequest3.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureNotificationRequest3.setPriority(PriorityLevel.NORMAL);

        notificationRequestRepo.save(featureNotificationRequest3);
    }

    @After
    public void reset() {
        notificationRequestRepo.deleteAll();
    }

    @Test
    public void test_findRequests_with_state() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureNotificationRequestParameters searchFeatureNotificationRequestParameters = new SearchFeatureNotificationRequestParameters().withStatesIncluded(
            Arrays.asList(RequestState.GRANTED));
        // When
        Page<FeatureNotificationRequest> oldResults = featureNotificationService.findRequests(
            FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED),
            pageable);

        Page<FeatureNotificationRequest> results = featureNotificationService.findRequests(
            searchFeatureNotificationRequestParameters,
            pageable);
        // Then
        assertEquals(2, oldResults.getNumberOfElements());
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureNotificationRequestParameters = new SearchFeatureNotificationRequestParameters().withStatesIncluded(
            Arrays.asList(RequestState.SUCCESS, RequestState.GRANTED));
        // When
        results = featureNotificationService.findRequests(searchFeatureNotificationRequestParameters, pageable);
        // Then
        assertEquals(4, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_provider_id() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureNotificationRequestParameters searchFeatureNotificationRequestParameters = new SearchFeatureNotificationRequestParameters().withProviderIdsIncluded(
            Arrays.asList(PROVIDER_ID0));
        // When
        Page<FeatureNotificationRequest> oldResults = featureNotificationService.findRequests(
            FeatureRequestsSelectionDTO.build().withProviderId(PROVIDER_ID0),
            pageable);
        Page<FeatureNotificationRequest> results = featureNotificationService.findRequests(
            searchFeatureNotificationRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureNotificationRequestParameters = new SearchFeatureNotificationRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider"));
        // When
        oldResults = featureNotificationService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                        .withProviderId("provider"),
                                                             pageable);
        results = featureNotificationService.findRequests(searchFeatureNotificationRequestParameters, pageable);
        // Then
        assertEquals(2, oldResults.getNumberOfElements());
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureNotificationRequestParameters = new SearchFeatureNotificationRequestParameters().withProviderIdsIncluded(
            Arrays.asList(PROVIDER_ID0, "provider_id1"));
        // When
        results = featureNotificationService.findRequests(searchFeatureNotificationRequestParameters, pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureNotificationRequestParameters = new SearchFeatureNotificationRequestParameters().withProviderIdsIncluded(
            Arrays.asList(PROVIDER_ID0, PROVIDER_ID2));
        // When
        results = featureNotificationService.findRequests(searchFeatureNotificationRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureNotificationRequestParameters = new SearchFeatureNotificationRequestParameters().withProviderIdsIncluded(
            Arrays.asList("Provider_ID0"));
        // When
        oldResults = featureNotificationService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                        .withProviderId("Provider_ID0"),
                                                             pageable);
        results = featureNotificationService.findRequests(searchFeatureNotificationRequestParameters, pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureNotificationRequestParameters = new SearchFeatureNotificationRequestParameters().withProviderIdsIncluded(
            Arrays.asList("Provider_ID0", "provIDer_iD2"));
        // When
        results = featureNotificationService.findRequests(searchFeatureNotificationRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_session() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureNotificationRequestParameters searchFeatureNotificationRequestParameters = new SearchFeatureNotificationRequestParameters().withSession(
            SESSION_0);
        // When
        Page<FeatureNotificationRequest> oldResults = featureNotificationService.findRequests(
            FeatureRequestsSelectionDTO.build().withSession(SESSION_0),
            pageable);

        Page<FeatureNotificationRequest> results = featureNotificationService.findRequests(
            searchFeatureNotificationRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_source() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureNotificationRequestParameters searchFeatureNotificationRequestParameters = new SearchFeatureNotificationRequestParameters().withSource(
            SESSION_OWNER_0);
        // When
        Page<FeatureNotificationRequest> oldResults = featureNotificationService.findRequests(
            FeatureRequestsSelectionDTO.build().withSource(SESSION_OWNER_0),
            pageable);

        Page<FeatureNotificationRequest> results = featureNotificationService.findRequests(
            searchFeatureNotificationRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

}
