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
package fr.cnes.regards.framework.modules.tenant.settings.service;

import fr.cnes.regards.framework.encryption.AESEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.encryption.sensitive.ISensitiveAnnotationEncryptionService;
import fr.cnes.regards.framework.encryption.sensitive.StringSensitiveAnnotationEncryptionService;
import fr.cnes.regards.framework.gson.GsonCustomizer;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.encryption.DynamicSettingsEncryptionService;
import fr.cnes.regards.framework.modules.tenant.settings.service.encryption.SensitiveDynamicSettingConverter;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

/**
 * The purpose of this test is to verify if sensitive parameters are masked with
 * {@link DynamicTenantSettingWithMaskService}.
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class DynamicTenantSettingsWithMaskServiceTest {

    private DynamicTenantSettingWithMaskService dynamicTenantSettingWithMaskService; // service under test

    @Mock
    private DynamicTenantSettingService dynamicTenantSettingService;

    @Mock
    private IDynamicTenantSettingRepository dynamicTenantSettingRepository;

    @Before
    public void init() throws Exception {
        // init gson
        GsonUtil.setGson(GsonCustomizer.gsonBuilder(Optional.empty(), Optional.empty()).create());
        // init encryption service
        AESEncryptionService aesEncryptionService = new AESEncryptionService();
        aesEncryptionService.init(new CipherProperties("src/test/resources/testKey", "1234567812345678"));
        DynamicSettingsEncryptionService dynamicSettingsEncryptionService = new DynamicSettingsEncryptionService(
            aesEncryptionService,
            new StringSensitiveAnnotationEncryptionService(aesEncryptionService));
        SensitiveDynamicSettingConverter sensitiveDynamicSettingConverter = new SensitiveDynamicSettingConverter(
            dynamicSettingsEncryptionService);
        // init repository service
        DynamicTenantSettingRepositoryService dynamicTenantSettingRepositoryService = new DynamicTenantSettingRepositoryService(
            dynamicTenantSettingRepository,
            sensitiveDynamicSettingConverter);

        // init mask service
        dynamicTenantSettingWithMaskService = new DynamicTenantSettingWithMaskService(dynamicTenantSettingService,
                                                                                      dynamicTenantSettingRepositoryService,
                                                                                      sensitiveDynamicSettingConverter);
        // mock behaviours
        mockDynamicTenantSettingServiceBehaviour();
    }

    @Test
    public void givenCreateSensitiveObjectSetting_whenCreated_thenMasked()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        // GIVEN
        SensitiveObject defaultSensitiveObject = new SensitiveObject("default random name",
                                                                     new SensitiveComponent(1123, "default secret"));
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       "settingTest",
                                                                       "this is not a description",
                                                                       defaultSensitiveObject,
                                                                       new SensitiveObject("random name",
                                                                                           new SensitiveComponent(1123,
                                                                                                                  "it's a secret!")),
                                                                       true);
        // WHEN
        DynamicTenantSetting dynamicSettingCreated = dynamicTenantSettingWithMaskService.create(dynamicSetting);

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 "settingTest",
                                                                                 "this is not a description",
                                                                                 defaultSensitiveObject,
                                                                                 new SensitiveObject("random name",
                                                                                                     new SensitiveComponent(
                                                                                                         1123,
                                                                                                         ISensitiveAnnotationEncryptionService.MASK_PATTERN)),
                                                                                 true);
        Assertions.assertThat((SensitiveObject) dynamicSettingCreated.getValue())
                  .isEqualTo(expectedEncryptedSetting.getValue());
    }

    private void mockDynamicTenantSettingServiceBehaviour()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        Mockito.when(dynamicTenantSettingService.create(any())).thenAnswer(ans -> ans.getArgument(0));
    }

}
