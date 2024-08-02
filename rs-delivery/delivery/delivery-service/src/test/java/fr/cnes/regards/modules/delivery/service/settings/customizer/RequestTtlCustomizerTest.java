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
 * Test for {@link fr.cnes.regards.modules.delivery.service.settings.customizers.RequestTtlCustomizer}
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class RequestTtlCustomizerTest extends AbstractDeliveryCustomizerTest {

    @Test
    public void givenValidRequestTtl_whenCreate_thenCreated()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        // GIVEN
        DynamicTenantSetting testedRequestTtl = new DynamicTenantSetting(1L,
                                                                         DeliverySettings.REQUEST_TTL_HOURS,
                                                                         "request time to live",
                                                                         DeliverySettings.DEFAULT_REQUEST_TTL_HOURS,
                                                                         23,
                                                                         false);
        // WHEN
        DynamicTenantSetting savedRequestTtl = dynamicTenantSettingService.create(testedRequestTtl);
        // THEN
        DynamicTenantSetting expectedRequestTtl = new DynamicTenantSetting(1L,
                                                                           DeliverySettings.REQUEST_TTL_HOURS,
                                                                           "request time to live",
                                                                           DeliverySettings.DEFAULT_REQUEST_TTL_HOURS,
                                                                           23,
                                                                           false);
        Assertions.assertThat((Integer) savedRequestTtl.getValue())
                  .as("Unexpected saved requestTtl value.")
                  .isEqualTo(expectedRequestTtl.getValue());
    }

    @Test
    public void givenInvalidRequestTtl_whenCreate_thenError() {
        // GIVEN
        DynamicTenantSetting testedRequestTtl = new DynamicTenantSetting(1L,
                                                                         DeliverySettings.REQUEST_TTL_HOURS,
                                                                         "request time to live",
                                                                         DeliverySettings.DEFAULT_REQUEST_TTL_HOURS,
                                                                         -1,
                                                                         false);
        // WHEN
        Assertions.assertThatExceptionOfType(EntityInvalidException.class)
                  .isThrownBy(() -> dynamicTenantSettingService.create(testedRequestTtl));
    }

    @Test
    public void givenValidRequestTtl_whenUpdate_thenUpdated()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        // GIVEN
        Mockito.when(dynamicTenantSettingRepositoryService.findByNameWithExceptionOnNotFound(DeliverySettings.REQUEST_TTL_HOURS,
                                                                                             false))
               .thenReturn(new DynamicTenantSetting(1L,
                                                    DeliverySettings.REQUEST_TTL_HOURS,
                                                    "request time to live",
                                                    DeliverySettings.DEFAULT_REQUEST_TTL_HOURS,
                                                    2,
                                                    false));
        // WHEN
        DynamicTenantSetting updatedRequestTtl = dynamicTenantSettingService.update(DeliverySettings.REQUEST_TTL_HOURS,
                                                                                    1);
        // THEN
        DynamicTenantSetting expectedRequestTtl = new DynamicTenantSetting(1L,
                                                                           DeliverySettings.REQUEST_TTL_HOURS,
                                                                           "request time to live",
                                                                           DeliverySettings.DEFAULT_REQUEST_TTL_HOURS,
                                                                           1,
                                                                           false);
        Assertions.assertThat((Integer) updatedRequestTtl.getValue())
                  .as("Unexpected updated requestTtl value.")
                  .isEqualTo(expectedRequestTtl.getValue());
    }

}
