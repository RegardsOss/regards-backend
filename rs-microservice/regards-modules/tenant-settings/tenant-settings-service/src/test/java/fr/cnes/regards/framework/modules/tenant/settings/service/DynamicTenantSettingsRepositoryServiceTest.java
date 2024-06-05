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
package fr.cnes.regards.framework.modules.tenant.settings.service;

import fr.cnes.regards.framework.encryption.AESEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.encryption.sensitive.StringSensitiveAnnotationEncryptionService;
import fr.cnes.regards.framework.gson.GsonCustomizer;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
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

/**
 * The purpose of this test is to verify if sensitive parameters are correctly found and handled if they are
 * decrypted or masked {@link DynamicTenantSettingWithMaskService}.
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class DynamicTenantSettingsRepositoryServiceTest {

    private DynamicTenantSettingRepositoryService dynamicTenantSettingRepositoryService; // service under test

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

        // init repository service
        dynamicTenantSettingRepositoryService = new DynamicTenantSettingRepositoryService(dynamicTenantSettingRepository,
                                                                                          new SensitiveDynamicSettingConverter(
                                                                                              dynamicSettingsEncryptionService));
    }

    @Test
    public void givenSensitiveStringSetting_whenFindByNameWithDecrypt_thenDecrypted() {
        // GIVEN
        String settingTestName = "settingTest";
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       settingTestName,
                                                                       "this is not a description",
                                                                       "default value",
                                                                       "fEHy7CYSuN/ubYXHDTUcxxPtJH/KZWAJbpAPu0kOC0A=",
                                                                       false);
        dynamicSetting.setContainsSensitiveParameters(true);

        // mock returned value
        Mockito.when(dynamicTenantSettingRepository.findByName(settingTestName))
               .thenReturn(Optional.of(dynamicSetting));

        // WHEN
        Optional<DynamicTenantSetting> dynamicSettingFoundWithMask = dynamicTenantSettingRepositoryService.findByName(
            settingTestName,
            true);

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 settingTestName,
                                                                                 "this is not a description",
                                                                                 "default value",
                                                                                 "that's a secret!",
                                                                                 false);

        Assertions.assertThat((String) dynamicSettingFoundWithMask.get().getValue())
                  .isEqualTo(expectedEncryptedSetting.getValue());
    }

    @Test
    public void givenStringSetting_whenFindByName_thenReturned() {
        // GIVEN
        String settingTestName = "settingTest";
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       settingTestName,
                                                                       "this is not a description",
                                                                       "default value",
                                                                       "public value",
                                                                       false);
        // mock returned value
        Mockito.when(dynamicTenantSettingRepository.findByName(settingTestName))
               .thenReturn(Optional.of(dynamicSetting));

        // WHEN
        Optional<DynamicTenantSetting> dynamicSettingFoundWithMask = dynamicTenantSettingRepositoryService.findByName(
            settingTestName,
            false);

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 settingTestName,
                                                                                 "this is not a description",
                                                                                 "default value",
                                                                                 "public value",
                                                                                 false);

        Assertions.assertThat((String) dynamicSettingFoundWithMask.get().getValue())
                  .isEqualTo(expectedEncryptedSetting.getValue());
    }

}
