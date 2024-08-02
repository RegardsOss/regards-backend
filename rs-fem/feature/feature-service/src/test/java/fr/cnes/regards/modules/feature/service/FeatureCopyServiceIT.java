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
package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.IFeatureCopyRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCopyRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
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
public class FeatureCopyServiceIT extends AbstractFeatureMultitenantServiceIT {

    private final static String PROVIDER_ID0 = "provider_id0";

    private final static String PROVIDER_ID2 = "provider_id2";

    private final static String SESSION_0 = "session0";

    private final static String SESSION_OWNER_0 = "session_owner0";

    @Autowired
    protected IFeatureCopyRequestRepository featureCopyRequestRepo;

    @Autowired
    protected IFeatureCopyService featureCopyService;

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

        FeatureCopyRequest featureCopyRequest0 = new FeatureCopyRequest();
        featureCopyRequest0.setRequestId("request_id0");
        featureCopyRequest0.setRequestOwner("request_owner0");
        featureCopyRequest0.setState(RequestState.GRANTED);
        featureCopyRequest0.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCopyRequest0.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCopyRequest0.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureCopyRequest0.setPriority(PriorityLevel.NORMAL);
        featureCopyRequest0.setUrn(URN0);

        featureCopyRequestRepo.save(featureCopyRequest0);

        FeatureCopyRequest featureCopyRequest1 = new FeatureCopyRequest();
        featureCopyRequest1.setRequestId("request_id1");
        featureCopyRequest1.setRequestOwner("request_owner1");
        featureCopyRequest1.setState(RequestState.GRANTED);
        featureCopyRequest1.setRegistrationDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCopyRequest1.setRequestDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCopyRequest1.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureCopyRequest1.setPriority(PriorityLevel.NORMAL);

        featureCopyRequestRepo.save(featureCopyRequest1);

        FeatureCopyRequest featureCopyRequest2 = new FeatureCopyRequest();
        featureCopyRequest2.setRequestId("request_id2");
        featureCopyRequest2.setRequestOwner("request_owner2");
        featureCopyRequest2.setState(RequestState.SUCCESS);
        featureCopyRequest2.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCopyRequest2.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCopyRequest2.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureCopyRequest2.setPriority(PriorityLevel.NORMAL);
        featureCopyRequest2.setUrn(URN1);

        featureCopyRequestRepo.save(featureCopyRequest2);

        FeatureCopyRequest featureCopyRequest21 = new FeatureCopyRequest();
        featureCopyRequest21.setRequestId("request_id21");
        featureCopyRequest21.setRequestOwner("request_owner21");
        featureCopyRequest21.setState(RequestState.SUCCESS);
        featureCopyRequest21.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCopyRequest21.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCopyRequest21.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureCopyRequest21.setPriority(PriorityLevel.NORMAL);

        featureCopyRequestRepo.save(featureCopyRequest21);

        FeatureCopyRequest featureCopyRequest3 = new FeatureCopyRequest();
        featureCopyRequest3.setRequestId("request_id3");
        featureCopyRequest3.setRequestOwner("request_owner3");
        featureCopyRequest3.setState(RequestState.ERROR);
        featureCopyRequest3.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCopyRequest3.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCopyRequest3.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureCopyRequest3.setPriority(PriorityLevel.NORMAL);

        featureCopyRequestRepo.save(featureCopyRequest3);
    }

    @After
    public void reset() {
        featureCopyRequestRepo.deleteAll();
    }

    @Test
    public void test_findRequests_with_state() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureRequestParameters searchFeatureRequestParameters = new SearchFeatureRequestParameters().withStatesIncluded(
            List.of(RequestState.GRANTED));
        // When
        Page<FeatureCopyRequest> results = featureCopyService.findRequests(searchFeatureRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_provider_id() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureRequestParameters searchFeatureRequestParameters = new SearchFeatureRequestParameters().withProviderIdsIncluded(
            List.of(PROVIDER_ID0));
        // When
        Page<FeatureCopyRequest> results = featureCopyService.findRequests(searchFeatureRequestParameters, pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureRequestParameters = new SearchFeatureRequestParameters().withProviderIdsIncluded(List.of("provider"));
        // When
        results = featureCopyService.findRequests(searchFeatureRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureRequestParameters = new SearchFeatureRequestParameters().withProviderIdsIncluded(Arrays.asList(
            PROVIDER_ID0,
            "provider_id1"));
        // When
        results = featureCopyService.findRequests(searchFeatureRequestParameters, pageable);
        // Then
        assertEquals(1, results.getNumberOfElements());
    }
}
