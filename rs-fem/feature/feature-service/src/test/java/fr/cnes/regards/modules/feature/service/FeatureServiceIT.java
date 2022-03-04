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
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;
import fr.cnes.regards.modules.feature.dto.SearchSelectionMode;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link FeatureServiceIT}
 * @author Kevin Marchois
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_data_object" }, locations = {
        "classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties" })
@ActiveProfiles(value = { "noscheduler", "noFemHandler" })
public class FeatureServiceIT extends AbstractFeatureMultitenantServiceTest {

    @Autowired
    private IFeatureService featureService;

    @Test
    public void testFindAll() {
        String model = mockModelClient("feature_model_01.xml", cps, factory, this.getDefaultTenant(),
                                       modelAttrAssocClientMock);

        FeatureEntity firstFeature = FeatureEntity
                .build("owner", "session",
                       Feature.build("id2", "owner",
                                     FeatureUniformResourceName.build(FeatureIdentifier.FEATURE, EntityType.DATA,
                                                                      "peps", UUID.randomUUID(), 1),
                                     IGeometry.point(IGeometry.position(10.0, 20.0)), EntityType.DATA, model),
                       null, model);

        this.featureRepo.save(firstFeature);

        OffsetDateTime date = OffsetDateTime.now();

        FeatureEntity secondFeature = FeatureEntity
                .build("owner", "session",
                       Feature.build("id2", "owner",
                                     FeatureUniformResourceName.build(FeatureIdentifier.FEATURE, EntityType.DATA,
                                                                      "peps", UUID.randomUUID(), 1),
                                     IGeometry.point(IGeometry.position(10.0, 20.0)), EntityType.DATA, model),
                       null, model);
        secondFeature.getFeature().addProperty(IProperty.buildString("data_type", "TYPE01"));

        this.featureRepo.save(secondFeature);

        FeaturesSelectionDTO selection = FeaturesSelectionDTO.build().withModel(model);
        // Retrieve all from model
        Pageable page = PageRequest.of(0, 10);
        Page<FeatureEntityDto> results = featureService.findAll(selection, page);
        assertEquals(2, results.getNumberOfElements());

        Optional<FeatureEntityDto> dofOpt = results.getContent().stream().filter(fed -> fed.getId().equals(secondFeature.getId())).findFirst();
        assertTrue(dofOpt.isPresent(), "Entity researched not found");
        FeatureEntityDto dof = dofOpt.get();
        // compare values inside the DataObjectFeature and those of the FeatureEntity should be the same
        assertEquals(secondFeature.getFeature().getProperties(), dof.getFeature().getProperties());
        assertEquals(secondFeature.getSession(), dof.getSession());
        assertEquals(secondFeature.getSessionOwner(), dof.getSource());
        assertEquals(secondFeature.getFeature().getModel(), dof.getFeature().getModel());

        // Retrieve with an unknown model
        selection = FeaturesSelectionDTO.build()
                .withModel("unknown");
        results = featureService.findAll(selection, page);
        assertEquals(0, results.getNumberOfElements());

        // Retrieve from model and  lastUpdateDate to retrieve only second feature
        selection = FeaturesSelectionDTO.build()
                .withModel(model)
                .withFrom(date);
        results = featureService.findAll(selection, page);
        assertEquals(1, results.getNumberOfElements());

        selection = FeaturesSelectionDTO.build()
                .withId(firstFeature.getId())
                .withId(secondFeature.getId());
        results = featureService.findAll(selection, page);
        assertEquals(2, results.getNumberOfElements());

        selection = FeaturesSelectionDTO.build()
                .withId(secondFeature.getId());
        results = featureService.findAll(selection, page);
        assertEquals(1, results.getNumberOfElements());

        selection = FeaturesSelectionDTO.build()
                .withSelectionMode(SearchSelectionMode.EXCLUDE)
                .withId(firstFeature.getId())
                .withId(secondFeature.getId());
        results = featureService.findAll(selection, page);
        assertEquals(0, results.getNumberOfElements());

        selection = FeaturesSelectionDTO.build()
                .withSelectionMode(SearchSelectionMode.EXCLUDE)
                .withId(secondFeature.getId());
        results = featureService.findAll(selection, page);
        assertEquals(1, results.getNumberOfElements());

    }
}
