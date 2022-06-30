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
package fr.cnes.regards.modules.feature.client;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureDisseminationInfoDto;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=fem_client" })
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
public class IFeatureEntityClientIT extends AbstractRegardsWebIT {

    private static final Logger LOG = LoggerFactory.getLogger(IFeatureEntityClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    /**
     * Client to test
     */
    private IFeatureEntityClient client;

    @Autowired
    private IFeatureEntityRepository featureEntityRepository;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    private Gson gson;

    @Before
    public void init() {
        jwtService.injectMockToken(getDefaultTenant(), DEFAULT_ROLE);
        client = FeignClientBuilder.build(new TokenClientProvider<>(IFeatureEntityClient.class,
                                                                    "http://" + serverAddress + ":" + getPort(),
                                                                    feignSecurityManager), gson);
        FeignSecurityManager.asSystem();
    }

    /**
     * Check that the access right Feign Client handle the pagination parameters.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testFindAll() {
        Feature feature = Feature.build("featureEntity1",
                                        "owner",
                                        FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                                         EntityType.DATA,
                                                                         getDefaultTenant(),
                                                                         UUID.randomUUID(),
                                                                         1),
                                        IGeometry.unlocated(),
                                        EntityType.DATA,
                                        "model");
        FeatureEntity featureEntity1 = featureEntityRepository.save(FeatureEntity.build("sessionOwner",
                                                                                        "session",
                                                                                        feature,
                                                                                        null,
                                                                                        "model"));
        feature = Feature.build("featureEntity2",
                                "owner2",
                                FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                                 EntityType.DATA,
                                                                 getDefaultTenant(),
                                                                 UUID.randomUUID(),
                                                                 1),
                                IGeometry.unlocated(),
                                EntityType.DATA,
                                "model");
        FeatureEntity featureEntity2 = featureEntityRepository.save(FeatureEntity.build("sessionOwner2",
                                                                                        "session2",
                                                                                        feature,
                                                                                        null,
                                                                                        "model"));

        ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> response = client.findAll("model",
                                                                                            OffsetDateTime.now()
                                                                                                          .minusDays(2),
                                                                                            //to get what was just created
                                                                                            0,
                                                                                            2,
                                                                                            Sort.by("sessionOwner",
                                                                                                    "model"));
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        List<FeatureEntityDto> body = HateoasUtils.unwrapCollection(Objects.requireNonNull(response.getBody())
                                                                           .getContent());
        Assert.assertEquals("first should be featureEntity1, actual is " + body.get(0).getProviderId(),
                            toDto(featureEntity1),
                            body.get(0));
        Assert.assertEquals("second should be featureEntity2, actual is " + body.get(1).getProviderId(),
                            toDto(featureEntity2),
                            body.get(1));
    }

    @After
    public void cleanUp() {
        featureEntityRepository.deleteAll();
    }

    private FeatureEntityDto toDto(FeatureEntity entity) {
        FeatureEntityDto dto = new FeatureEntityDto();
        dto.setSession(entity.getSession());
        dto.setSource(entity.getSessionOwner());
        dto.setProviderId(entity.getProviderId());
        dto.setVersion(entity.getVersion());
        dto.setLastUpdate(entity.getLastUpdate());
        dto.setUrn(entity.getUrn());
        dto.setId(entity.getId());
        dto.setDisseminationPending(entity.isDisseminationPending());
        dto.setDisseminationsInfo(entity.getDisseminationsInfo()
                                        .stream()
                                        .map(featureDisseminationInfo -> new FeatureDisseminationInfoDto(
                                            featureDisseminationInfo.getLabel(),
                                            featureDisseminationInfo.getRequestDate(),
                                            featureDisseminationInfo.getAckDate()))
                                        .collect(Collectors.toSet()));
        dto.setFeature(entity.getFeature());
        return dto;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
