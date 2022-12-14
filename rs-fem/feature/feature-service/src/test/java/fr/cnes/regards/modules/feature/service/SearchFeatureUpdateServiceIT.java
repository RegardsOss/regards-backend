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
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureUpdateRequestParameters;
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
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Stephane Cortine
 */
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=feature_version", "regards.amqp.enabled=true" },
    locations = { "classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
public class SearchFeatureUpdateServiceIT extends AbstractFeatureMultitenantServiceIT {

    private final static String PROVIDER_ID0 = "provider_id0";

    private final static String PROVIDER_ID1 = "provider_id1";

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
        String model = mockModelClient("feature_model_01.xml",
                                       cps,
                                       factory,
                                       this.getDefaultTenant(),
                                       modelAttrAssocClientMock);

        Feature feature0 = Feature.build(PROVIDER_ID0,
                                         "owner",
                                         URN0,
                                         IGeometry.point(IGeometry.position(10.0, 20.0)),
                                         EntityType.DATA,
                                         model);
        FeatureEntity featureEntity = FeatureEntity.build(SESSION_OWNER_0, SESSION_0, feature0, null, model);
        featureRepo.save(featureEntity);

        Feature feature1 = Feature.build(PROVIDER_ID2,
                                         "owner",
                                         URN1,
                                         IGeometry.point(IGeometry.position(10.0, 20.0)),
                                         EntityType.DATA,
                                         model);
        featureEntity = FeatureEntity.build("owner", "session", feature1, null, model);
        featureRepo.save(featureEntity);

        FeatureUpdateRequest featureUpdateRequest0 = new FeatureUpdateRequest();
        featureUpdateRequest0.setFeature(feature0);
        featureUpdateRequest0.setRequestId("request_id0");
        featureUpdateRequest0.setRequestOwner("request_owner0");
        featureUpdateRequest0.setState(RequestState.GRANTED);
        featureUpdateRequest0.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureUpdateRequest0.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureUpdateRequest0.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureUpdateRequest0.setPriority(PriorityLevel.NORMAL);
        featureUpdateRequest0.setUrn(URN0);
        featureUpdateRequest0.setProviderId(PROVIDER_ID0);
        featureUpdateRequest0.setMetadata(new FeatureCreationMetadataEntity().build("session_owner0",
                                                                                    "session0",
                                                                                    new ArrayList<>(),
                                                                                    false));

        featureUpdateRequestRepo.save(featureUpdateRequest0);

        FeatureUpdateRequest featureUpdateRequest1 = new FeatureUpdateRequest();
        featureUpdateRequest1.setRequestId("request_id1");
        featureUpdateRequest1.setRequestOwner("request_owner1");
        featureUpdateRequest1.setState(RequestState.GRANTED);
        featureUpdateRequest1.setRegistrationDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureUpdateRequest1.setRequestDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureUpdateRequest1.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureUpdateRequest1.setPriority(PriorityLevel.NORMAL);
        featureUpdateRequest1.setProviderId(PROVIDER_ID1);
        featureUpdateRequest1.setMetadata(new FeatureCreationMetadataEntity().build("session_owner1",
                                                                                    "session1",
                                                                                    new ArrayList<>(),
                                                                                    false));

        featureUpdateRequestRepo.save(featureUpdateRequest1);

        FeatureUpdateRequest featureUpdateRequest2 = new FeatureUpdateRequest();
        featureUpdateRequest2.setFeature(feature1);
        featureUpdateRequest2.setRequestId("request_id2");
        featureUpdateRequest2.setRequestOwner("request_owner2");
        featureUpdateRequest2.setState(RequestState.SUCCESS);
        featureUpdateRequest2.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureUpdateRequest2.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureUpdateRequest2.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureUpdateRequest2.setPriority(PriorityLevel.NORMAL);
        featureUpdateRequest2.setUrn(URN1);
        featureUpdateRequest2.setProviderId("provider_id2");
        featureUpdateRequest2.setMetadata(new FeatureCreationMetadataEntity().build("session_owner2",
                                                                                    "session2",
                                                                                    new ArrayList<>(),
                                                                                    false));

        featureUpdateRequestRepo.save(featureUpdateRequest2);

        FeatureUpdateRequest featureUpdateRequest21 = new FeatureUpdateRequest();
        featureUpdateRequest21.setRequestId("request_id21");
        featureUpdateRequest21.setRequestOwner("request_owner21");
        featureUpdateRequest21.setState(RequestState.SUCCESS);
        featureUpdateRequest21.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureUpdateRequest21.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureUpdateRequest21.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureUpdateRequest21.setPriority(PriorityLevel.NORMAL);
        featureUpdateRequest21.setProviderId("provider_id2");
        featureUpdateRequest21.setMetadata(new FeatureCreationMetadataEntity().build("session_owner2",
                                                                                     "session2",
                                                                                     new ArrayList<>(),
                                                                                     false));

        featureUpdateRequestRepo.save(featureUpdateRequest21);

        FeatureUpdateRequest featureUpdateRequest3 = new FeatureUpdateRequest();
        featureUpdateRequest3.setRequestId("request_id3");
        featureUpdateRequest3.setRequestOwner("request_owner3");
        featureUpdateRequest3.setState(RequestState.ERROR);
        featureUpdateRequest3.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureUpdateRequest3.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureUpdateRequest3.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureUpdateRequest3.setPriority(PriorityLevel.NORMAL);
        featureUpdateRequest3.setProviderId("provider_id3");
        featureUpdateRequest3.setMetadata(new FeatureCreationMetadataEntity().build("session_owner3",
                                                                                    "session3",
                                                                                    new ArrayList<>(),
                                                                                    false));

        featureUpdateRequestRepo.save(featureUpdateRequest3);
    }

    @After
    public void reset() {
        featureUpdateRequestRepo.deleteAll();
    }

    @Test
    public void test_findRequests_with_state() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureUpdateRequestParameters searchFeatureUpdateRequestParameters = new SearchFeatureUpdateRequestParameters().withStatesIncluded(
            Arrays.asList(RequestState.GRANTED));
        // When
        Page<FeatureUpdateRequest> oldResults = featureUpdateService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                             .withState(
                                                                                                                 RequestState.GRANTED),
                                                                                  pageable);
        Page<FeatureUpdateRequest> results = featureUpdateService.findRequests(searchFeatureUpdateRequestParameters,
                                                                               pageable);
        // Then
        assertEquals(2, oldResults.getNumberOfElements());
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureUpdateRequestParameters = new SearchFeatureUpdateRequestParameters().withStatesIncluded(Arrays.asList(
            RequestState.SUCCESS,
            RequestState.GRANTED));
        // When
        results = featureUpdateService.findRequests(searchFeatureUpdateRequestParameters, pageable);
        // Then
        assertEquals(4, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_provider_id() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureUpdateRequestParameters searchFeatureUpdateRequestParameters = new SearchFeatureUpdateRequestParameters().withProviderIdsIncluded(
            Arrays.asList(PROVIDER_ID0));
        // When
        Page<FeatureUpdateRequest> oldResults = featureUpdateService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                             .withProviderId(
                                                                                                                 PROVIDER_ID0),
                                                                                  pageable);
        Page<FeatureUpdateRequest> results = featureUpdateService.findRequests(searchFeatureUpdateRequestParameters,
                                                                               pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureUpdateRequestParameters = new SearchFeatureUpdateRequestParameters().withProviderIdsIncluded(Arrays.asList(
            "provider"));
        // When
        oldResults = featureUpdateService.findRequests(FeatureRequestsSelectionDTO.build().withProviderId("provider"),
                                                       pageable);
        results = featureUpdateService.findRequests(searchFeatureUpdateRequestParameters, pageable);
        // Then
        assertEquals(2, oldResults.getNumberOfElements());
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureUpdateRequestParameters = new SearchFeatureUpdateRequestParameters().withProviderIdsIncluded(Arrays.asList(
            PROVIDER_ID1));
        // When
        oldResults = featureUpdateService.findRequests(FeatureRequestsSelectionDTO.build().withProviderId(PROVIDER_ID1),
                                                       pageable);
        results = featureUpdateService.findRequests(searchFeatureUpdateRequestParameters, pageable);
        // Then
        assertEquals(0, oldResults.getNumberOfElements());
        assertEquals(0, results.getNumberOfElements());

        // Given
        searchFeatureUpdateRequestParameters = new SearchFeatureUpdateRequestParameters().withProviderIdsIncluded(Arrays.asList(
            PROVIDER_ID0,
            PROVIDER_ID1));
        // When
        results = featureUpdateService.findRequests(searchFeatureUpdateRequestParameters, pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureUpdateRequestParameters = new SearchFeatureUpdateRequestParameters().withProviderIdsIncluded(Arrays.asList(
            PROVIDER_ID0,
            PROVIDER_ID2));
        // When
        results = featureUpdateService.findRequests(searchFeatureUpdateRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureUpdateRequestParameters = new SearchFeatureUpdateRequestParameters().withProviderIdsIncluded(Arrays.asList(
            "Provider_ID0",
            "provIDer_iD2"));
        // When
        oldResults = featureUpdateService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                  .withProviderId("Provider_ID0"),
                                                       pageable);
        results = featureUpdateService.findRequests(searchFeatureUpdateRequestParameters, pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(2, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_session() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureUpdateRequestParameters searchFeatureUpdateRequestParameters = new SearchFeatureUpdateRequestParameters().withSession(
            "session0");
        // When
        Page<FeatureUpdateRequest> oldResults = featureUpdateService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                             .withSession(
                                                                                                                 "session0"),
                                                                                  pageable);
        Page<FeatureUpdateRequest> results = featureUpdateService.findRequests(searchFeatureUpdateRequestParameters,
                                                                               pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_source() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureUpdateRequestParameters searchFeatureUpdateRequestParameters = new SearchFeatureUpdateRequestParameters().withSource(
            SESSION_OWNER_0);
        // When
        Page<FeatureUpdateRequest> oldResults = featureUpdateService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                             .withSource(
                                                                                                                 SESSION_OWNER_0),
                                                                                  pageable);
        Page<FeatureUpdateRequest> results = featureUpdateService.findRequests(searchFeatureUpdateRequestParameters,
                                                                               pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

}
