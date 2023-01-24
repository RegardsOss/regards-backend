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
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureCreationRequestParameters;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Stephane Cortine
 */
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=feature_version", "regards.amqp.enabled=true" },
    locations = { "classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
public class FeatureCreationServiceIT extends AbstractFeatureMultitenantServiceIT {

    @Before
    public void init() {
        FeatureEntity feature = FeatureEntity.build("owner",
                                                    "session",
                                                    Feature.build("id2",
                                                                  "owner",
                                                                  FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                                                                   EntityType.DATA,
                                                                                                   "peps",
                                                                                                   UUID.randomUUID(),
                                                                                                   1),
                                                                  IGeometry.point(IGeometry.position(10.0, 20.0)),
                                                                  EntityType.DATA,
                                                                  featureModelName),
                                                    null,
                                                    featureModelName);
        featureRepo.save(feature);

        FeatureCreationRequest featureCreationRequest0 = new FeatureCreationRequest();
        featureCreationRequest0.setFeatureEntity(feature);
        featureCreationRequest0.setRequestId("request_id0");
        featureCreationRequest0.setRequestOwner("request_owner0");
        featureCreationRequest0.setState(RequestState.GRANTED);
        featureCreationRequest0.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCreationRequest0.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCreationRequest0.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureCreationRequest0.setPriority(PriorityLevel.NORMAL);
        featureCreationRequest0.setProviderId("provider_id0");
        featureCreationRequest0.setMetadata(new FeatureCreationMetadataEntity().build("session_owner0",
                                                                                      "session0",
                                                                                      new ArrayList<>(),
                                                                                      false));

        featureCreationRequestRepo.save(featureCreationRequest0);

        FeatureCreationRequest featureCreationRequest1 = new FeatureCreationRequest();
        featureCreationRequest1.setFeatureEntity(feature);
        featureCreationRequest1.setRequestId("request_id100");
        featureCreationRequest1.setRequestOwner("request_owner1");
        featureCreationRequest1.setState(RequestState.GRANTED);
        featureCreationRequest1.setRegistrationDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCreationRequest1.setRequestDate(OffsetDateTime.of(2022, 11, 8, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCreationRequest1.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureCreationRequest1.setPriority(PriorityLevel.NORMAL);
        featureCreationRequest1.setProviderId("provider_id1");
        featureCreationRequest1.setMetadata(new FeatureCreationMetadataEntity().build("session_owner1",
                                                                                      "session1",
                                                                                      new ArrayList<>(),
                                                                                      false));

        featureCreationRequestRepo.save(featureCreationRequest1);

        FeatureCreationRequest featureCreationRequest2 = new FeatureCreationRequest();
        featureCreationRequest2.setFeatureEntity(feature);
        featureCreationRequest2.setRequestId("request_id2");
        featureCreationRequest2.setRequestOwner("request_owner2");
        featureCreationRequest2.setState(RequestState.SUCCESS);
        featureCreationRequest2.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCreationRequest2.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCreationRequest2.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureCreationRequest2.setPriority(PriorityLevel.NORMAL);
        featureCreationRequest2.setProviderId("provider_id2");
        featureCreationRequest2.setMetadata(new FeatureCreationMetadataEntity().build("session_owner2",
                                                                                      "session2",
                                                                                      new ArrayList<>(),
                                                                                      false));

        featureCreationRequestRepo.save(featureCreationRequest2);

        FeatureCreationRequest featureCreationRequest21 = new FeatureCreationRequest();
        featureCreationRequest21.setFeatureEntity(feature);
        featureCreationRequest21.setRequestId("request_id21");
        featureCreationRequest21.setRequestOwner("request_owner21");
        featureCreationRequest21.setState(RequestState.SUCCESS);
        featureCreationRequest21.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCreationRequest21.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCreationRequest21.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureCreationRequest21.setPriority(PriorityLevel.NORMAL);
        featureCreationRequest21.setProviderId("provider_id2");
        featureCreationRequest21.setMetadata(new FeatureCreationMetadataEntity().build("session_owner2",
                                                                                       "session2",
                                                                                       new ArrayList<>(),
                                                                                       false));

        featureCreationRequestRepo.save(featureCreationRequest21);

        FeatureCreationRequest featureCreationRequest3 = new FeatureCreationRequest();
        featureCreationRequest3.setFeatureEntity(feature);
        featureCreationRequest3.setRequestId("request_id3");
        featureCreationRequest3.setRequestOwner("request_owner3");
        featureCreationRequest3.setState(RequestState.ERROR);
        featureCreationRequest3.setRegistrationDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCreationRequest3.setRequestDate(OffsetDateTime.of(2022, 11, 9, 14, 30, 30, 0, ZoneOffset.UTC));
        featureCreationRequest3.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        featureCreationRequest3.setPriority(PriorityLevel.NORMAL);
        featureCreationRequest3.setProviderId("provider_id3");
        featureCreationRequest3.setMetadata(new FeatureCreationMetadataEntity().build("session_owner3",
                                                                                      "session3",
                                                                                      new ArrayList<>(),
                                                                                      false));

        featureCreationRequestRepo.save(featureCreationRequest3);
    }

    @After
    public void reset() {
        featureCreationRequestRepo.deleteAll();
    }

    @Test
    public void test_findRequests_with_state() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureCreationRequestParameters searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withStatesIncluded(
            Arrays.asList(RequestState.GRANTED));
        // When
        Page<FeatureCreationRequest> oldResults = featureCreationService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withState(
                                                                                                                     RequestState.GRANTED),
                                                                                      pageable);
        Page<FeatureCreationRequest> results = featureCreationService.findRequests(
            searchFeatureCreationRequestParameters,
            pageable);
        // Then
        assertEquals(2, oldResults.getNumberOfElements());
        assertEquals(2, results.getNumberOfElements());

        // Given
        searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withStatesIncluded(Arrays.asList(
            RequestState.SUCCESS,
            RequestState.GRANTED));
        // When
        results = featureCreationService.findRequests(searchFeatureCreationRequestParameters, pageable);
        // Then
        assertEquals(4, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_source() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureCreationRequestParameters searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withSource(
            "session_owner0");
        // When
        Page<FeatureCreationRequest> oldResults = featureCreationService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withSource(
                                                                                                                     "session_owner0"),
                                                                                      pageable);
        Page<FeatureCreationRequest> results = featureCreationService.findRequests(
            searchFeatureCreationRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_session() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureCreationRequestParameters searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withSession(
            "session0");
        // When
        Page<FeatureCreationRequest> oldResults = featureCreationService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withSession(
                                                                                                                     "session0"),
                                                                                      pageable);
        Page<FeatureCreationRequest> results = featureCreationService.findRequests(
            searchFeatureCreationRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_provider_id() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureCreationRequestParameters searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider_id1"));
        // When
        Page<FeatureCreationRequest> oldResults = featureCreationService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withProviderId(
                                                                                                                     "provider_id1"),
                                                                                      pageable);
        Page<FeatureCreationRequest> results = featureCreationService.findRequests(
            searchFeatureCreationRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());

        // Given
        searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withProviderIdsIncluded(
            Arrays.asList("provider"));
        // When
        oldResults = featureCreationService.findRequests(FeatureRequestsSelectionDTO.build().withProviderId("provider"),
                                                         pageable);
        results = featureCreationService.findRequests(searchFeatureCreationRequestParameters, pageable);
        // Then
        assertEquals(5, oldResults.getNumberOfElements());
        assertEquals(5, results.getNumberOfElements());

        // Given
        searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withProviderIdsIncluded(
            Arrays.asList("id0"));
        // When
        oldResults = featureCreationService.findRequests(FeatureRequestsSelectionDTO.build().withProviderId("id0"),
                                                         pageable);
        results = featureCreationService.findRequests(searchFeatureCreationRequestParameters, pageable);
        // Then
        assertEquals(0, oldResults.getNumberOfElements());
        assertEquals(0, results.getNumberOfElements());

        // Given
        searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withProviderIdsIncluded(
            Arrays.asList("Provider_id0", "provider_Id1"));
        // When
        results = featureCreationService.findRequests(searchFeatureCreationRequestParameters, pageable);
        // Then
        assertEquals(2, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_with_last_update() {
        // Given
        OffsetDateTime begin = OffsetDateTime.of(2022, 10, 9, 14, 30, 30, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2022, 12, 9, 14, 30, 30, 0, ZoneOffset.UTC);
        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureCreationRequestParameters searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withLastUpdateBefore(
            end).withLastUpdateAfter(begin);
        // When
        Page<FeatureCreationRequest> oldResults = featureCreationService.findRequests(FeatureRequestsSelectionDTO.build()
                                                                                                                 .withStart(
                                                                                                                     begin)
                                                                                                                 .withEnd(
                                                                                                                     end),
                                                                                      pageable);
        Page<FeatureCreationRequest> results = featureCreationService.findRequests(
            searchFeatureCreationRequestParameters,
            pageable);
        // Then
        assertEquals(5, oldResults.getNumberOfElements());
        assertEquals(5, results.getNumberOfElements());
    }

    @Test
    public void test_findRequests_all_criterias() {
        // Given
        OffsetDateTime begin = OffsetDateTime.of(2022, 10, 9, 14, 30, 30, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2022, 12, 9, 14, 30, 30, 0, ZoneOffset.UTC);

        Pageable pageable = PageRequest.of(0, 100);
        SearchFeatureCreationRequestParameters searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withSource(
                                                                                                                                        "session_owner0")
                                                                                                                                    .withSession(
                                                                                                                                        "session0")
                                                                                                                                    .withProviderIdsIncluded(
                                                                                                                                        Arrays.asList(
                                                                                                                                            "provider_id0"))
                                                                                                                                    .withStatesIncluded(
                                                                                                                                        Arrays.asList(
                                                                                                                                            RequestState.GRANTED))
                                                                                                                                    .withLastUpdateBefore(
                                                                                                                                        end)
                                                                                                                                    .withLastUpdateAfter(
                                                                                                                                        begin);
        // When
        Page<FeatureCreationRequest> oldResults = featureCreationService.findRequests(FeatureRequestsSelectionDTO.build()

                                                                                                                 .withSource(
                                                                                                                     "session_owner0")
                                                                                                                 .withSession(
                                                                                                                     "session0")
                                                                                                                 .withProviderId(
                                                                                                                     "provider_id0")
                                                                                                                 .withState(
                                                                                                                     RequestState.GRANTED)
                                                                                                                 .withStart(
                                                                                                                     begin)
                                                                                                                 .withEnd(
                                                                                                                     end),
                                                                                      pageable);
        Page<FeatureCreationRequest> results = featureCreationService.findRequests(
            searchFeatureCreationRequestParameters,
            pageable);
        // Then
        assertEquals(1, oldResults.getNumberOfElements());
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_getInfo_with_state() {
        // Given
        SearchFeatureCreationRequestParameters searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withStatesIncluded(
            Arrays.asList(RequestState.GRANTED));
        // When
        RequestsInfo results = featureCreationService.getInfo(searchFeatureCreationRequestParameters);
        // Then
        assertEquals(0, results.getNbErrors());

        // Given
        searchFeatureCreationRequestParameters = new SearchFeatureCreationRequestParameters().withStatesIncluded(Arrays.asList(
            RequestState.ERROR));
        // When
        results = featureCreationService.getInfo(searchFeatureCreationRequestParameters);
        // Then
        assertEquals(1, results.getNbErrors());
    }

}