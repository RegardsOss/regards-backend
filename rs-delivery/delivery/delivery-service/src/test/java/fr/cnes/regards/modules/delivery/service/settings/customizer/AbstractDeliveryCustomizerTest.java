/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.settings.customizer;

import fr.cnes.regards.framework.gson.GsonCustomizer;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingRepositoryService;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingService;
import fr.cnes.regards.modules.delivery.service.settings.customizers.BuildBucketCustomizer;
import fr.cnes.regards.modules.delivery.service.settings.customizers.DeliveryBucketCustomizer;
import fr.cnes.regards.modules.delivery.service.settings.customizers.RequestTtlCustomizer;
import fr.cnes.regards.modules.delivery.service.settings.customizers.S3ServerCustomizer;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.validation.Validator;

import java.util.List;
import java.util.Optional;

/**
 * @author Iliana Ghazali
 **/
public abstract class AbstractDeliveryCustomizerTest {

    protected DynamicTenantSettingService dynamicTenantSettingService;

    @Mock
    protected DynamicTenantSettingRepositoryService dynamicTenantSettingRepositoryService;

    @Mock
    protected Validator validator;

    @Before
    public void init() throws EntityNotFoundException {
        // INIT
        // init gson
        GsonUtil.setGson(GsonCustomizer.gsonBuilder(Optional.empty(), Optional.empty()).create());

        // init customizers
        RequestTtlCustomizer requestTtlCustomizer = new RequestTtlCustomizer();
        S3ServerCustomizer s3Customizer = new S3ServerCustomizer(validator);
        BuildBucketCustomizer buildBucketCustomizer = new BuildBucketCustomizer();
        DeliveryBucketCustomizer deliveryBucketCustomizer = new DeliveryBucketCustomizer();

        // init settings service
        dynamicTenantSettingService = new DynamicTenantSettingService(List.of(requestTtlCustomizer,
                                                                              s3Customizer,
                                                                              buildBucketCustomizer,
                                                                              deliveryBucketCustomizer),
                                                                      dynamicTenantSettingRepositoryService);
        // MOCK BEHAVIOURS
        // mock setting repo
        Mockito.when(dynamicTenantSettingRepositoryService.save(Mockito.any(), Mockito.any()))
               .thenAnswer(ans -> ans.getArgument(0));
    }
}
