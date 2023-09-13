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
 * Test for {@link fr.cnes.regards.modules.delivery.service.settings.customizers.DeliveryBucketCustomizer}
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class DeliveryBucketCustomizerTest extends AbstractDeliveryCustomizerTest {

    @Test
    public void givenValidDeliveryBucket_whenCreate_thenCreated()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        // GIVEN
        DynamicTenantSetting testedDeliveryBucket = new DynamicTenantSetting(1L,
                                                                             DeliverySettings.DELIVERY_BUCKET,
                                                                             "delivery bucket",
                                                                             DeliverySettings.DEFAULT_DELIVERY_BUCKET,
                                                                             "delivery-bucket",
                                                                             false);
        // WHEN
        DynamicTenantSetting savedDeliveryBucket = dynamicTenantSettingService.create(testedDeliveryBucket);
        // THEN
        DynamicTenantSetting expectedDeliveryBucket = new DynamicTenantSetting(1L,
                                                                               DeliverySettings.DELIVERY_BUCKET,
                                                                               "delivery bucket",
                                                                               DeliverySettings.DEFAULT_DELIVERY_BUCKET,
                                                                               "delivery-bucket",
                                                                               false);
        Assertions.assertThat((String) savedDeliveryBucket.getValue())
                  .as("Unexpected saved delivery bucket value.")
                  .isEqualTo(expectedDeliveryBucket.getValue());
    }

    @Test
    public void givenNullDeliveryBucket_whenCreate_thenError() {
        // GIVEN
        DynamicTenantSetting testedDeliveryBucket = new DynamicTenantSetting(1L,
                                                                             DeliverySettings.DELIVERY_BUCKET,
                                                                             "delivery bucket",
                                                                             DeliverySettings.DEFAULT_DELIVERY_BUCKET,
                                                                             null,
                                                                             false);
        // WHEN
        Assertions.assertThatExceptionOfType(EntityInvalidException.class)
                  .isThrownBy(() -> dynamicTenantSettingService.create(testedDeliveryBucket));
    }

    @Test
    public void givenValidDeliveryBucket_whenUpdate_thenUpdated()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        // GIVEN
        Mockito.when(dynamicTenantSettingRepositoryService.findByNameWithExceptionOnNotFound(DeliverySettings.DELIVERY_BUCKET,
                                                                                             false))
               .thenReturn(new DynamicTenantSetting(1L,
                                                    DeliverySettings.DELIVERY_BUCKET,
                                                    "delivery bucket",
                                                    DeliverySettings.DEFAULT_DELIVERY_BUCKET,
                                                    "updated-delivery-bucket",
                                                    false));
        // WHEN
        DynamicTenantSetting updatedDeliveryBucket = dynamicTenantSettingService.update(DeliverySettings.DELIVERY_BUCKET,
                                                                                        "updated-delivery-bucket");
        // THEN
        DynamicTenantSetting expectedDeliveryBucket = new DynamicTenantSetting(1L,
                                                                               DeliverySettings.DELIVERY_BUCKET,
                                                                               "delivery bucket",
                                                                               DeliverySettings.DEFAULT_DELIVERY_BUCKET,
                                                                               "updated-delivery-bucket",
                                                                               false);
        Assertions.assertThat((String) updatedDeliveryBucket.getValue())
                  .as("Unexpected updated delivery bucket value")
                  .isEqualTo(expectedDeliveryBucket.getValue());
    }
}
