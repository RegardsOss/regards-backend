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
import fr.cnes.regards.modules.feature.domain.DisseminationFilterStatusEnum;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link FeatureServiceIT}
 *
 * @author Kevin Marchois
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_data_object" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles(value = { "noscheduler", "noFemHandler" })
public class FeatureServiceIT extends AbstractFeatureMultitenantServiceIT {

    @Autowired
    private IFeatureService featureService;

    private FeatureEntity firstFeature;

    private FeatureEntity secondFeature;

    private OffsetDateTime dateAfterCreatedFirstFeature;

    @Before
    public void init() {
        firstFeature = FeatureEntity.build("owner",
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

        firstFeature.setProviderId("providerId1");
        firstFeature.setDisseminationPending(true);
        featureRepo.save(firstFeature);

        dateAfterCreatedFirstFeature = OffsetDateTime.now();

        secondFeature = FeatureEntity.build("owner",
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
        secondFeature.getFeature().addProperty(IProperty.buildString("data_type", "TYPE01"));
        secondFeature.setProviderId("providerId2");

        featureRepo.save(secondFeature);
    }

    @After
    public void reset() {
        featureRepo.deleteAll();
    }

    @Test
    public void test_findAll() {
        // Given
        Pageable page = PageRequest.of(0, 10);
        SearchFeatureSimpleEntityParameters selection = new SearchFeatureSimpleEntityParameters().withModel(
            featureModelName);
        // When
        Page<FeatureEntityDto> results = featureService.findAll(selection, page);
        // Then
        assertEquals(2, results.getNumberOfElements());

        verifyFeature(results);
    }

    private void verifyFeature(Page<FeatureEntityDto> results) {
        // When
        Optional<FeatureEntityDto> dofOpt = results.getContent()
                                                   .stream()
                                                   .filter(fed -> fed.getId().equals(secondFeature.getId()))
                                                   .findFirst();
        // Then
        assertTrue(dofOpt.isPresent(), "Entity researched not found");
        // compare values inside the DataObjectFeature and those of the FeatureEntity should be the same
        assertEquals(secondFeature.getFeature().getProperties(), dofOpt.get().getFeature().getProperties());
        assertEquals(secondFeature.getSession(), dofOpt.get().getSession());
        assertEquals(secondFeature.getSessionOwner(), dofOpt.get().getSource());
        assertEquals(secondFeature.getFeature().getModel(), dofOpt.get().getFeature().getModel());
    }

    @Test
    public void test_findAll_with_id() {
        // Given
        Pageable page = PageRequest.of(0, 10);

        SearchFeatureSimpleEntityParameters selection = new SearchFeatureSimpleEntityParameters().withFeatureIdsIncluded(
            List.of(firstFeature.getId(), secondFeature.getId()));
        // When
        Page<FeatureEntityDto> results = featureService.findAll(selection, page);
        // Then
        assertEquals(2, results.getNumberOfElements());

        // Given
        selection = new SearchFeatureSimpleEntityParameters().withFeatureIdsIncluded(List.of(secondFeature.getId()));
        // When
        results = featureService.findAll(selection, page);
        // Then
        assertEquals(1, results.getNumberOfElements());

        // Given
        selection = selection.withFeatureIdsExcluded(List.of(firstFeature.getId(), secondFeature.getId()));
        // When
        results = featureService.findAll(selection, page);
        // Then
        assertEquals(0, results.getNumberOfElements());

        // Given
        selection = new SearchFeatureSimpleEntityParameters().withFeatureIdsExcluded(List.of(secondFeature.getId()));
        // When
        results = featureService.findAll(selection, page);
        // Then
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_findAll_with_model() {
        // Given
        Pageable page = PageRequest.of(0, 10);
        SearchFeatureSimpleEntityParameters selection = new SearchFeatureSimpleEntityParameters().withModel("unknown");
        // When
        Page<FeatureEntityDto> results = featureService.findAll(selection, page);
        // Then
        assertEquals(0, results.getNumberOfElements());

        // Given
        selection = new SearchFeatureSimpleEntityParameters().withModel(featureModelName);
        // When
        results = featureService.findAll(selection, page);
        // Then
        assertEquals(2, results.getNumberOfElements());
    }

    @Test
    public void test_findAll_with_model_lastUpdate() {
        // Given
        Pageable page = PageRequest.of(0, 10);

        SearchFeatureSimpleEntityParameters selection = new SearchFeatureSimpleEntityParameters().withModel(
            featureModelName).withLastUpdateAfter(dateAfterCreatedFirstFeature);
        // When
        Page<FeatureEntityDto> results = featureService.findAll(selection, page);
        // When
        assertEquals(1, results.getNumberOfElements());
    }

    @Test
    public void test_findAll_with_providerId() {
        // Given
        Pageable page = PageRequest.of(0, 10);

        SearchFeatureSimpleEntityParameters selection = new SearchFeatureSimpleEntityParameters().withProviderIdsIncluded(
            List.of("providerId1"));
        // When
        Page<FeatureEntityDto> results = featureService.findAll(selection, page);
        // When
        assertEquals(1, results.getNumberOfElements());

        // Given
        selection.withProviderIdsIncluded(List.of("Providerid1"));
        // When
        results = featureService.findAll(selection, page);
        // When
        assertEquals(1, results.getNumberOfElements());

        // Given
        selection.withProviderIdsIncluded(List.of("provider"));
        // When
        results = featureService.findAll(selection, page);
        // When
        assertEquals(2, results.getNumberOfElements());

        // Given
        selection.withProviderIdsIncluded(List.of("Id1"));
        // When
        results = featureService.findAll(selection, page);
        // When
        assertEquals(0, results.getNumberOfElements());
    }

    @Test
    public void test_findAll_with_disseminationPending() {
        // Given
        Pageable page = PageRequest.of(0, 10);

        SearchFeatureSimpleEntityParameters selection = new SearchFeatureSimpleEntityParameters();
        selection.withDisseminationStatus(DisseminationFilterStatusEnum.DONE);
        SearchFeatureSimpleEntityParameters filters = new SearchFeatureSimpleEntityParameters().withDisseminationStatus(
            DisseminationFilterStatusEnum.DONE);
        // When
        Page<FeatureEntityDto> results = featureService.findAll(selection, page);
        Page<FeatureEntityDto> featureEntityDtos = featureService.findAll(filters, page);
        // When
        assertEquals(1, results.getNumberOfElements());
        assertEquals(1, featureEntityDtos.getNumberOfElements());
        assertEquals("providerId2", results.getContent().get(0).getProviderId());

        // Given
        selection = new SearchFeatureSimpleEntityParameters();
        selection.withDisseminationStatus(DisseminationFilterStatusEnum.PENDING);
        filters = new SearchFeatureSimpleEntityParameters().withDisseminationStatus(DisseminationFilterStatusEnum.PENDING);
        // When
        results = featureService.findAll(selection, page);
        featureEntityDtos = featureService.findAll(filters, page);
        // When
        assertEquals(1, results.getNumberOfElements());
        assertEquals(1, featureEntityDtos.getNumberOfElements());
        assertEquals("providerId1", results.getContent().get(0).getProviderId());
    }

}
