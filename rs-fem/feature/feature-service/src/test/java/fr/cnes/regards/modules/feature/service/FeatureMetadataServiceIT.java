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
import fr.cnes.regards.modules.feature.domain.request.FeatureSaveMetadataRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureSaveMetadataRequestParameters;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.dump.IFeatureMetadataService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
public class FeatureMetadataServiceIT extends AbstractFeatureMultitenantServiceIT {

    private final static String PROVIDER_ID0 = "provider_id0";

    private final static String PROVIDER_ID2 = "provider_id2";

    private final static String SESSION_0 = "session0";

    private final static String SESSION_OWNER_0 = "session_owner0";

    @Autowired
    private IFeatureMetadataService featureMetadataSrv;

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

        FeatureSaveMetadataRequest featureSaveMetadataRequest0 = new FeatureSaveMetadataRequest();
        featureSaveMetadataRequest0.setRequestId("request_id0");
        featureSaveMetadataRequest0.setRequestOwner("request_owner0");
        featureSaveMetadataRequest0.setState(RequestState.GRANTED);
        featureSaveMetadataRequest0.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureSaveMetadataRequest0.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureSaveMetadataRequest0.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureSaveMetadataRequest0.setPriority(PriorityLevel.NORMAL);
        featureSaveMetadataRequest0.setUrn(URN0);

        featureSaveMetadataRequestRepository.save(featureSaveMetadataRequest0);

        FeatureSaveMetadataRequest featureSaveMetadataRequest1 = new FeatureSaveMetadataRequest();
        featureSaveMetadataRequest1.setRequestId("request_id1");
        featureSaveMetadataRequest1.setRequestOwner("request_owner1");
        featureSaveMetadataRequest1.setState(RequestState.GRANTED);
        featureSaveMetadataRequest1.setRegistrationDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureSaveMetadataRequest1.setRequestDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureSaveMetadataRequest1.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureSaveMetadataRequest1.setPriority(PriorityLevel.NORMAL);

        featureSaveMetadataRequestRepository.save(featureSaveMetadataRequest1);

        FeatureSaveMetadataRequest featureSaveMetadataRequest2 = new FeatureSaveMetadataRequest();
        featureSaveMetadataRequest2.setRequestId("request_id2");
        featureSaveMetadataRequest2.setRequestOwner("request_owner2");
        featureSaveMetadataRequest2.setState(RequestState.SUCCESS);
        featureSaveMetadataRequest2.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureSaveMetadataRequest2.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureSaveMetadataRequest2.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureSaveMetadataRequest2.setPriority(PriorityLevel.NORMAL);
        featureSaveMetadataRequest2.setUrn(URN1);

        featureSaveMetadataRequestRepository.save(featureSaveMetadataRequest2);

        FeatureSaveMetadataRequest featureSaveMetadataRequest21 = new FeatureSaveMetadataRequest();
        featureSaveMetadataRequest21.setRequestId("request_id21");
        featureSaveMetadataRequest21.setRequestOwner("request_owner21");
        featureSaveMetadataRequest21.setState(RequestState.SUCCESS);
        featureSaveMetadataRequest21.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureSaveMetadataRequest21.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureSaveMetadataRequest21.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureSaveMetadataRequest21.setPriority(PriorityLevel.NORMAL);

        featureSaveMetadataRequestRepository.save(featureSaveMetadataRequest21);

        FeatureSaveMetadataRequest featureSaveMetadataRequest3 = new FeatureSaveMetadataRequest();
        //featureSaveMetadataRequest3.setFeatureEntity(feature);
        featureSaveMetadataRequest3.setRequestId("request_id3");
        featureSaveMetadataRequest3.setRequestOwner("request_owner3");
        featureSaveMetadataRequest3.setState(RequestState.ERROR);
        featureSaveMetadataRequest3.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureSaveMetadataRequest3.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureSaveMetadataRequest3.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureSaveMetadataRequest3.setPriority(PriorityLevel.NORMAL);

        featureSaveMetadataRequestRepository.save(featureSaveMetadataRequest3);
    }

    @After
    public void reset() {
        featureSaveMetadataRequestRepository.deleteAll();
    }

    @Test
    public void test_findRequests_with_state() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureSaveMetadataRequestParameters SearchFeatureSaveMetadataRequestParameters = new SearchFeatureSaveMetadataRequestParameters().withStatesIncluded(
            Arrays.asList(RequestState.GRANTED));
        // When
        Page<FeatureSaveMetadataRequest> oldResults = featureMetadataSrv.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withState(
                                                                                                                     RequestState.GRANTED),
                                                                                      pageable);
        Page<FeatureSaveMetadataRequest> results = featureMetadataSrv.findRequests(
            SearchFeatureSaveMetadataRequestParameters,
            pageable);
        // Then
        assertEquals(2, oldResults.getNumberOfElements());
        assertEquals(2, results.getNumberOfElements());

        // Given
        SearchFeatureSaveMetadataRequestParameters = new SearchFeatureSaveMetadataRequestParameters().withStatesIncluded(
            Arrays.asList(RequestState.SUCCESS, RequestState.GRANTED));
        // When
        results = featureMetadataSrv.findRequests(SearchFeatureSaveMetadataRequestParameters, pageable);
        // Then
        assertEquals(4, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_provider_id() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureSaveMetadataRequestParameters SearchFeatureSaveMetadataRequestParameters = new SearchFeatureSaveMetadataRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider_id0"));
        // When
        Page<FeatureSaveMetadataRequest> oldResults = featureMetadataSrv.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withProviderId(
                                                                                                                     "provider_id0"),
                                                                                      pageable);
        Page<FeatureSaveMetadataRequest> results = featureMetadataSrv.findRequests(
            SearchFeatureSaveMetadataRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());

        // Given
        SearchFeatureSaveMetadataRequestParameters = new SearchFeatureSaveMetadataRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider"));
        // When
        oldResults = featureMetadataSrv.findRequests(FeatureRequestsSelectionDTO.build().withProviderId("provider"),
                                                     pageable);
        results = featureMetadataSrv.findRequests(SearchFeatureSaveMetadataRequestParameters, pageable);
        // Then
        assertEquals(2, oldResults.getNumberOfElements());
        assertEquals(2, results.getNumberOfElements());

        // Given
        SearchFeatureSaveMetadataRequestParameters = new SearchFeatureSaveMetadataRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider_id0", "provider_id1"));
        // When
        results = featureMetadataSrv.findRequests(SearchFeatureSaveMetadataRequestParameters, pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());

        // Given
        SearchFeatureSaveMetadataRequestParameters = new SearchFeatureSaveMetadataRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider_id0", "provider_id2"));
        // When
        results = featureMetadataSrv.findRequests(SearchFeatureSaveMetadataRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());

        // Given
        SearchFeatureSaveMetadataRequestParameters = new SearchFeatureSaveMetadataRequestParameters().withProviderIdsIncluded(
            Arrays.asList("Provider_ID0", "provIDer_iD2"));
        // When
        results = featureMetadataSrv.findRequests(SearchFeatureSaveMetadataRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_session() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureSaveMetadataRequestParameters SearchFeatureSaveMetadataRequestParameters = new SearchFeatureSaveMetadataRequestParameters().withSession(
            "session0");
        // When
        Page<FeatureSaveMetadataRequest> oldResults = featureMetadataSrv.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withSession(
                                                                                                                     "session0"),
                                                                                      pageable);
        Page<FeatureSaveMetadataRequest> results = featureMetadataSrv.findRequests(
            SearchFeatureSaveMetadataRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_source() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureSaveMetadataRequestParameters SearchFeatureSaveMetadataRequestParameters = new SearchFeatureSaveMetadataRequestParameters().withSource(
            SESSION_OWNER_0);
        // When
        Page<FeatureSaveMetadataRequest> oldResults = featureMetadataSrv.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withSource(
                                                                                                                     SESSION_OWNER_0),
                                                                                      pageable);
        Page<FeatureSaveMetadataRequest> results = featureMetadataSrv.findRequests(
            SearchFeatureSaveMetadataRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

}

