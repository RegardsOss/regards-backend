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

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test for {@link fr.cnes.regards.modules.delivery.service.settings.customizers.BuildBucketCustomizer}
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class BuildBucketCustomizerTest extends AbstractDeliveryCustomizerTest {

    @Test
    public void givenValidBuildBucket_whenCreate_thenCreated()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        // GIVEN
        DynamicTenantSetting testedBuildBucket = new DynamicTenantSetting(1L,
                                                                          DeliverySettings.BUILD_BUCKET,
                                                                          "build bucket",
                                                                          DeliverySettings.DEFAULT_BUILD_BUCKET,
                                                                          "build-bucket",
                                                                          false);
        // WHEN
        DynamicTenantSetting savedBuildBucket = dynamicTenantSettingService.create(testedBuildBucket);
        // THEN
        DynamicTenantSetting expectedBuildBucket = new DynamicTenantSetting(1L,
                                                                            DeliverySettings.BUILD_BUCKET,
                                                                            "build bucket",
                                                                            DeliverySettings.DEFAULT_BUILD_BUCKET,
                                                                            "build-bucket",
                                                                            false);
        Assertions.assertThat((String) savedBuildBucket.getValue())
                  .as("Unexpected saved build bucket value.")
                  .isEqualTo(expectedBuildBucket.getValue());
    }

    @Test
    public void givenNullBuildBucket_whenCreate_thenError() {
        // GIVEN
        DynamicTenantSetting testedBuildBucket = new DynamicTenantSetting(1L,
                                                                          DeliverySettings.BUILD_BUCKET,
                                                                          "build bucket",
                                                                          DeliverySettings.DEFAULT_BUILD_BUCKET,
                                                                          null,
                                                                          false);
        // WHEN
        Assertions.assertThatExceptionOfType(EntityInvalidException.class)
                  .isThrownBy(() -> dynamicTenantSettingService.create(testedBuildBucket));
    }

    @Test
    public void givenValidBuildBucket_whenUpdate_thenUpdated()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        // GIVEN
        Mockito.when(dynamicTenantSettingRepositoryService.findByNameWithExceptionOnNotFound(DeliverySettings.BUILD_BUCKET,
                                                                                             false))
               .thenReturn(new DynamicTenantSetting(1L,
                                                    DeliverySettings.BUILD_BUCKET,
                                                    "build bucket",
                                                    DeliverySettings.DEFAULT_BUILD_BUCKET,
                                                    "updated-build-bucket",
                                                    false));
        // WHEN
        DynamicTenantSetting updatedBuildBucket = dynamicTenantSettingService.update(DeliverySettings.BUILD_BUCKET,
                                                                                     "updated-build-bucket");
        // THEN
        DynamicTenantSetting expectedBuildBucket = new DynamicTenantSetting(1L,
                                                                            DeliverySettings.BUILD_BUCKET,
                                                                            "build bucket",
                                                                            DeliverySettings.DEFAULT_BUILD_BUCKET,
                                                                            "updated-build-bucket",
                                                                            false);
        Assertions.assertThat((String) updatedBuildBucket.getValue())
                  .as("Unexpected updated build bucket value")
                  .isEqualTo(expectedBuildBucket.getValue());
    }
}
