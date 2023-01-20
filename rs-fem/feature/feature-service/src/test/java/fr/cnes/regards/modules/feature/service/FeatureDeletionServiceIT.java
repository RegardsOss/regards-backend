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
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureDeletionRequestParameters;
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
public class FeatureDeletionServiceIT extends AbstractFeatureMultitenantServiceIT {

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

        FeatureDeletionRequest featureDeletionRequest0 = new FeatureDeletionRequest();
        featureDeletionRequest0.setRequestId("request_id0");
        featureDeletionRequest0.setRequestOwner("request_owner0");
        featureDeletionRequest0.setState(RequestState.GRANTED);
        featureDeletionRequest0.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureDeletionRequest0.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureDeletionRequest0.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureDeletionRequest0.setPriority(PriorityLevel.NORMAL);
        featureDeletionRequest0.setUrn(URN0);

        featureDeletionRequestRepo.save(featureDeletionRequest0);

        FeatureDeletionRequest featureDeletionRequest1 = new FeatureDeletionRequest();
        featureDeletionRequest1.setRequestId("request_id1");
        featureDeletionRequest1.setRequestOwner("request_owner1");
        featureDeletionRequest1.setState(RequestState.GRANTED);
        featureDeletionRequest1.setRegistrationDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureDeletionRequest1.setRequestDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureDeletionRequest1.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureDeletionRequest1.setPriority(PriorityLevel.NORMAL);

        featureDeletionRequestRepo.save(featureDeletionRequest1);

        FeatureDeletionRequest featureDeletionRequest2 = new FeatureDeletionRequest();
        featureDeletionRequest2.setRequestId("request_id2");
        featureDeletionRequest2.setRequestOwner("request_owner2");
        featureDeletionRequest2.setState(RequestState.SUCCESS);
        featureDeletionRequest2.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureDeletionRequest2.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureDeletionRequest2.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureDeletionRequest2.setPriority(PriorityLevel.NORMAL);
        featureDeletionRequest2.setUrn(URN1);

        featureDeletionRequestRepo.save(featureDeletionRequest2);

        FeatureDeletionRequest featureDeletionRequest21 = new FeatureDeletionRequest();
        featureDeletionRequest21.setRequestId("request_id21");
        featureDeletionRequest21.setRequestOwner("request_owner21");
        featureDeletionRequest21.setState(RequestState.SUCCESS);
        featureDeletionRequest21.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureDeletionRequest21.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureDeletionRequest21.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureDeletionRequest21.setPriority(PriorityLevel.NORMAL);

        featureDeletionRequestRepo.save(featureDeletionRequest21);

        FeatureDeletionRequest featureDeletionRequest3 = new FeatureDeletionRequest();
        featureDeletionRequest3.setRequestId("request_id3");
        featureDeletionRequest3.setRequestOwner("request_owner3");
        featureDeletionRequest3.setState(RequestState.ERROR);
        featureDeletionRequest3.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureDeletionRequest3.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureDeletionRequest3.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureDeletionRequest3.setPriority(PriorityLevel.NORMAL);

        featureDeletionRequestRepo.save(featureDeletionRequest3);
    }

    @After
    public void reset() {
        featureDeletionRequestRepo.deleteAll();
    }

    @Test
    public void test_findRequests_with_state() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureDeletionRequestParameters searchFeatureDeletionRequestParameters = new SearchFeatureDeletionRequestParameters().withStatesIncluded(
            Arrays.asList(RequestState.GRANTED));
        // When
        Page<FeatureDeletionRequest> oldResults = featureDeletionService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withState(
                                                                                                                     RequestState.GRANTED),
                                                                                      pageable);
        Page<FeatureDeletionRequest> results = featureDeletionService.findRequests(
            searchFeatureDeletionRequestParameters,
            pageable);
        // Then
        assertEquals(2, oldResults.getNumberOfElements());
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureDeletionRequestParameters = new SearchFeatureDeletionRequestParameters().withStatesIncluded(Arrays.asList(
            RequestState.SUCCESS,
            RequestState.GRANTED));
        // When
        results = featureDeletionService.findRequests(searchFeatureDeletionRequestParameters, pageable);
        // Then
        assertEquals(4, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_provider_id() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureDeletionRequestParameters searchFeatureDeletionRequestParameters = new SearchFeatureDeletionRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider_id0"));
        // When
        Page<FeatureDeletionRequest> oldResults = featureDeletionService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withProviderId(
                                                                                                                     "provider_id0"),
                                                                                      pageable);
        Page<FeatureDeletionRequest> results = featureDeletionService.findRequests(
            searchFeatureDeletionRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureDeletionRequestParameters = new SearchFeatureDeletionRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider"));
        // When
        oldResults = featureDeletionService.findRequests(FeatureRequestsSelectionDTO.build().withProviderId("provider"),
                                                         pageable);
        results = featureDeletionService.findRequests(searchFeatureDeletionRequestParameters, pageable);
        // Then
        assertEquals(2, oldResults.getNumberOfElements());
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureDeletionRequestParameters = new SearchFeatureDeletionRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider_id0", "provider_id1"));
        // When
        results = featureDeletionService.findRequests(searchFeatureDeletionRequestParameters, pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureDeletionRequestParameters = new SearchFeatureDeletionRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider_id0", "provider_id2"));
        // When
        results = featureDeletionService.findRequests(searchFeatureDeletionRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureDeletionRequestParameters = new SearchFeatureDeletionRequestParameters().withProviderIdsIncluded(
            Arrays.asList("Provider_ID0", "provIDer_iD2"));
        // When
        results = featureDeletionService.findRequests(searchFeatureDeletionRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_session() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureDeletionRequestParameters searchFeatureDeletionRequestParameters = new SearchFeatureDeletionRequestParameters().withSession(
            "session0");
        // When
        Page<FeatureDeletionRequest> oldResults = featureDeletionService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withSession(
                                                                                                                     "session0"),
                                                                                      pageable);
        Page<FeatureDeletionRequest> results = featureDeletionService.findRequests(
            searchFeatureDeletionRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_source() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureDeletionRequestParameters searchFeatureDeletionRequestParameters = new SearchFeatureDeletionRequestParameters().withSource(
            SESSION_OWNER_0);
        // When
        Page<FeatureDeletionRequest> oldResults = featureDeletionService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withSource(
                                                                                                                     SESSION_OWNER_0),
                                                                                      pageable);
        Page<FeatureDeletionRequest> results = featureDeletionService.findRequests(
            searchFeatureDeletionRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

}
