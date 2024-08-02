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
package fr.cnes.regards.modules.feature.service.dump;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureSaveMetadataRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceIT;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Stephane Cortine
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_version",
                                   "regards.amqp.enabled=true" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
public class SearchFeatureMetadataServiceIT extends AbstractFeatureMultitenantServiceIT {

    private final static String PROVIDER_ID0 = "provider_id0";

    private final static String PROVIDER_ID2 = "provider_id2";

    private final static String SESSION_0 = "session0";

    private final static String SESSION_OWNER_0 = "session_owner0";

    @Autowired
    private IFeatureMetadataService featureMetadataSrv;

    private FeatureUniformResourceName createPseudoRandomUrn() {
        return FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE, EntityType.DATA, "tenant0", 1);
    }

    @Before
    public void init() {
        FeatureUniformResourceName URN0 = createPseudoRandomUrn();
        FeatureUniformResourceName URN1 = createPseudoRandomUrn();
        featureRepo.save(FeatureEntity.build(SESSION_OWNER_0,
                                             SESSION_0,
                                             Feature.build(PROVIDER_ID0,
                                                           "owner",
                                                           URN0,
                                                           IGeometry.point(IGeometry.position(10.0, 20.0)),
                                                           EntityType.DATA,
                                                           featureModelName),
                                             null,
                                             featureModelName));

        featureRepo.save(FeatureEntity.build("owner",
                                             "session",
                                             Feature.build(PROVIDER_ID2,
                                                           "owner",
                                                           URN1,
                                                           IGeometry.point(IGeometry.position(10.0, 20.0)),
                                                           EntityType.DATA,
                                                           featureModelName),
                                             null,
                                             featureModelName));

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

        SearchFeatureRequestParameters searchFeatureRequestParameters = new SearchFeatureRequestParameters().withStatesIncluded(
            List.of(RequestState.GRANTED));

        // When
        Page<FeatureSaveMetadataRequest> results = featureMetadataSrv.findRequests(searchFeatureRequestParameters,
                                                                                   pageable);

        // Then
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureRequestParameters = new SearchFeatureRequestParameters().withStatesIncluded(Arrays.asList(
            RequestState.SUCCESS,
            RequestState.GRANTED));
        // When
        results = featureMetadataSrv.findRequests(searchFeatureRequestParameters, pageable);
        // Then
        assertEquals(4, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_provider_id() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureRequestParameters searchFeatureRequestParameters = new SearchFeatureRequestParameters().withProviderIdsIncluded(
            List.of(PROVIDER_ID0));

        // When
        Page<FeatureSaveMetadataRequest> results = featureMetadataSrv.findRequests(searchFeatureRequestParameters,
                                                                                   pageable);

        // Then
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureRequestParameters = new SearchFeatureRequestParameters().withProviderIdsIncluded(Arrays.asList(
            PROVIDER_ID0,
            "provider_id1"));
        //When
        results = featureMetadataSrv.findRequests(searchFeatureRequestParameters, pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureRequestParameters = new SearchFeatureRequestParameters().withProviderIdsIncluded(Arrays.asList(
            PROVIDER_ID0,
            PROVIDER_ID2));
        // When
        results = featureMetadataSrv.findRequests(searchFeatureRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());

        // When
        results = featureMetadataSrv.findRequests(new SearchFeatureRequestParameters().withProviderIdsIncluded(List.of(
            "Provider_ID0")), pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureRequestParameters = new SearchFeatureRequestParameters().withProviderIdsIncluded(Arrays.asList(
            "Provider_ID0",
            "provIDer_iD2"));
        // When
        results = featureMetadataSrv.findRequests(searchFeatureRequestParameters, pageable);
        //Then
        assertEquals(2, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_session() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureRequestParameters searchFeatureRequestParameters = new SearchFeatureRequestParameters().withSession(
            "session0");
        //When
        Page<FeatureSaveMetadataRequest> results = featureMetadataSrv.findRequests(searchFeatureRequestParameters,
                                                                                   pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_source() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureRequestParameters searchFeatureRequestParameters = new SearchFeatureRequestParameters().withSource(
            SESSION_OWNER_0);
        //When
        Page<FeatureSaveMetadataRequest> results = featureMetadataSrv.findRequests(searchFeatureRequestParameters,
                                                                                   pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_last_update() {
        // Given
        OffsetDateTime begin = OffsetDateTime.of(2022, 10, 9, 14, 30, 30, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2022, 12, 9, 14, 30, 30, 0, ZoneOffset.UTC);
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureRequestParameters searchFeatureRequestParameters = new SearchFeatureRequestParameters().withLastUpdateBefore(
            end).withLastUpdateAfter(begin);
        // When
        Page<FeatureSaveMetadataRequest> results = featureMetadataSrv.findRequests(searchFeatureRequestParameters,
                                                                                   pageable);
        // Then
        assertEquals(5, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_all_criterias() {
        // Given
        OffsetDateTime begin = OffsetDateTime.of(2022, 10, 9, 14, 30, 30, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2022, 12, 9, 14, 30, 30, 0, ZoneOffset.UTC);

        Pageable pageable = PageRequest.of(0, 100);

        SearchFeatureRequestParameters searchFeatureRequestParameters = new SearchFeatureRequestParameters().withSource(
                                                                                                                SESSION_OWNER_0)
                                                                                                            .withSession(
                                                                                                                SESSION_0)
                                                                                                            .withProviderIdsIncluded(
                                                                                                                List.of(
                                                                                                                    PROVIDER_ID0))
                                                                                                            .withStatesIncluded(
                                                                                                                List.of(
                                                                                                                    RequestState.GRANTED))
                                                                                                            .withLastUpdateBefore(
                                                                                                                end)
                                                                                                            .withLastUpdateAfter(
                                                                                                                begin);
        // When
        Page<FeatureSaveMetadataRequest> results = featureMetadataSrv.findRequests(searchFeatureRequestParameters,
                                                                                   pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_getInfo_with_state() {
        // Given
        SearchFeatureRequestParameters searchFeatureRequestParameters = new SearchFeatureRequestParameters().withStatesIncluded(
            List.of(RequestState.GRANTED));
        // When
        RequestsInfo results = featureMetadataSrv.getInfo(searchFeatureRequestParameters);
        // Then
        assertEquals(0, results.getNbErrors());

        // Given
        searchFeatureRequestParameters = new SearchFeatureRequestParameters().withStatesIncluded(List.of(RequestState.ERROR));
        // When
        results = featureMetadataSrv.getInfo(searchFeatureRequestParameters);
        // Then
        assertEquals(1, results.getNbErrors());
    }

}

