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
import fr.cnes.regards.modules.delivery.domain.settings.S3DeliveryServer;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

/**
 * Test for {@link fr.cnes.regards.modules.delivery.service.settings.customizers.S3ServerCustomizer}
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class S3ServerCustomizerTest extends AbstractDeliveryCustomizerTest {

    @Test
    public void givenValidS3Server_whenCreate_thenCreated()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        // GIVEN
        DynamicTenantSetting testedS3Server = new DynamicTenantSetting(1L,
                                                                       DeliverySettings.S3_SERVER,
                                                                       "s3 server",
                                                                       DeliverySettings.DEFAULT_S3_SERVER,
                                                                       new S3DeliveryServer("rs-s3-server",
                                                                                            9000,
                                                                                            "fr-regards-1",
                                                                                            "regards",
                                                                                            "password"));
        // WHEN
        DynamicTenantSetting savedS3Server = dynamicTenantSettingService.create(testedS3Server);
        // THEN
        DynamicTenantSetting expectedS3Server = new DynamicTenantSetting(1L,
                                                                         DeliverySettings.S3_SERVER,
                                                                         "s3 server",
                                                                         DeliverySettings.DEFAULT_S3_SERVER,
                                                                         new S3DeliveryServer("rs-s3-server",
                                                                                              9000,
                                                                                              "fr-regards-1",
                                                                                              "regards",
                                                                                              "password"));
        Assertions.assertThat((S3DeliveryServer) savedS3Server.getValue())
                  .as("Unexpected saved s3Server value.")
                  .isEqualTo(expectedS3Server.getValue());
    }

    @Test
    public void givenValidS3Server_whenUpdate_thenUpdated()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        // GIVEN
        Mockito.when(settingRepository.findByName(DeliverySettings.S3_SERVER))
               .thenReturn(Optional.of(new DynamicTenantSetting(1L,
                                                                DeliverySettings.S3_SERVER,
                                                                "s3 server",
                                                                DeliverySettings.DEFAULT_S3_SERVER,
                                                                new S3DeliveryServer("rs-s3-server",
                                                                                     9000,
                                                                                     "fr-regards-1",
                                                                                     "regards",
                                                                                     "password"))));
        // WHEN
        DynamicTenantSetting updatedS3Server = dynamicTenantSettingService.update(DeliverySettings.S3_SERVER,
                                                                                  new S3DeliveryServer("rs-s3-server",
                                                                                                       9000,
                                                                                                       "fr-regards-2",
                                                                                                       "regards",
                                                                                                       "password"));
        // THEN
        DynamicTenantSetting expectedS3Server = new DynamicTenantSetting(1L,
                                                                         DeliverySettings.S3_SERVER,
                                                                         "s3 server",
                                                                         DeliverySettings.DEFAULT_S3_SERVER,
                                                                         new S3DeliveryServer("rs-s3-server",
                                                                                              9000,
                                                                                              "fr-regards-2",
                                                                                              "regards",
                                                                                              "password"));
        Assertions.assertThat((S3DeliveryServer) updatedS3Server.getValue())
                  .as("Unexpected updated s3Server value.")
                  .isEqualTo(expectedS3Server.getValue());
    }
}
