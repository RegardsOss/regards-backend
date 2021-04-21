/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.model.dto.properties.IProperty;

/**
 * Test of {@link IDataObjectFeatureServiceIT}
 * @author Kevin Marchois
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_data_object" }, locations = {
        "classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties" })
@ActiveProfiles(value = { "noscheduler", "nohandler" })
public class IDataObjectFeatureServiceIT extends AbstractFeatureMultitenantServiceTest {

    @Autowired
    private IDataObjectFeatureService dataObjectService;

    @Test
    public void testFindAll() {
        String model = mockModelClient("feature_model_01.xml", cps, factory, this.getDefaultTenant(),
                                       modelAttrAssocClientMock);

        FeatureEntity feature = FeatureEntity
                .build("owner", "session",
                       Feature.build("id2", "owner",
                                     FeatureUniformResourceName.build(FeatureIdentifier.FEATURE, EntityType.DATA,
                                                                      "peps", UUID.randomUUID(), 1),
                                     IGeometry.point(IGeometry.position(10.0, 20.0)), EntityType.DATA, model),
                       null, model);

        this.featureRepo.save(feature);

        FeatureEntity featureUpdated = FeatureEntity
                .build("owner", "session",
                       Feature.build("id2", "owner",
                                     FeatureUniformResourceName.build(FeatureIdentifier.FEATURE, EntityType.DATA,
                                                                      "peps", UUID.randomUUID(), 1),
                                     IGeometry.point(IGeometry.position(10.0, 20.0)), EntityType.DATA, model),
                       null, model);
        featureUpdated.getFeature().addProperty(IProperty.buildString("data_type", "TYPE01"));

        OffsetDateTime date = featureUpdated.getLastUpdate();
        date = date.plusSeconds(1000);
        featureUpdated.setLastUpdate(date);
        this.featureRepo.save(featureUpdated);

        Pageable page = PageRequest.of(0, 10);
        Page<FeatureEntityDto> pageDof = dataObjectService.findAll(model, page, OffsetDateTime.now());
        // the first feateure created in this test should not be return so we must have only one reseult
        assertEquals(1, pageDof.getNumberOfElements());
        FeatureEntityDto dof = pageDof.getContent().get(0);
        // compare values inside the DataObjectFeature and those of the FeatureEntity should be the same
        assertEquals(featureUpdated.getFeature().getProperties(), dof.getFeature().getProperties());
        assertEquals(featureUpdated.getSession(), dof.getSession());
        assertEquals(featureUpdated.getSessionOwner(), dof.getSessionOwner());
        assertEquals(featureUpdated.getFeature().getModel(), dof.getFeature().getModel());

    }
}
