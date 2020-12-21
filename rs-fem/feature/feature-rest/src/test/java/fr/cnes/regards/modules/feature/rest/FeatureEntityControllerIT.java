/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.rest;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.IFeatureCreationService;

/**
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature",
        "regards.amqp.enabled=true", "spring.jpa.properties.hibernate.jdbc.batch_size=1024",
        "spring.jpa.properties.hibernate.order_inserts=true" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
@ContextConfiguration(classes = { AbstractMultitenantServiceTest.ScanningConfiguration.class })
public class FeatureEntityControllerIT extends AbstractFeatureIT {

    @Autowired
    private IFeatureCreationService featureService;

    @Test
    public void getFeatures() throws Exception {
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        Feature featureToAdd = initValidFeature();
        FeatureCreationMetadataEntity meta = FeatureCreationMetadataEntity.build("test", "1", Lists.newArrayList(),
                                                                                 true);
        meta.setStorages(Lists.newArrayList(StorageMetadata.build("disk")));
        FeatureCreationRequest request = FeatureCreationRequest
                .build(UUID.randomUUID().toString(), "owner", OffsetDateTime.now(), RequestState.GRANTED, null,
                       featureToAdd, meta, FeatureRequestStep.LOCAL_SCHEDULED, PriorityLevel.NORMAL);

        Set<Long> ids = Sets.newHashSet(request.getId());
        featureService.processRequests(ids, null);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("model", "FEATURE01");

        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT, requestBuilderCustomizer,
                          "Error retrieving features");

    }

}
