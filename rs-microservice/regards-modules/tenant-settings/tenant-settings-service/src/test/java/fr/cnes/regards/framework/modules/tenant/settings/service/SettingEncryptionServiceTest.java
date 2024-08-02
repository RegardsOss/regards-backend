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
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.encryption.DynamicSettingsEncryptionService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Optional;

/**
 * Test for {@link DynamicSettingsEncryptionService}
 * <p>The purpose is to verify that settings can be encrypted, decrypted or masked.</p>
 * <p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li> CREATE
 *        <ul>
 *          <li>{@link #givenCreatedStringSetting_whenEncryptSensitive_thenEncrypted()}</li>
 *          <li>{@link #givenCreatedObjectSetting_whenEncryptSensitive_thenEncrypted()}</li>
 *        </ul>
 *      </li>
 *      <li> UPDATE
 *        <ul>
 *           <li>{@link #givenUpdatedStringSetting_whenEncryptSensitive_thenEncrypted()}</li>
 *           <li>{@link #givenUpdatedObjectSetting_whenNoUpdateOnSensitive_thenNotEncrypted()}</li>
 *           <li>{@link #givenUpdatedObjectSetting_whenUpdateOnSensitiveObject_thenEncrypted()}</li>
 *        </ul>
 *      </li>
 *      <li> FIND WITH DECRYPT
 *        <ul>
 *           <li>{@link #givenFindStringSetting_whenDecryptSensitive_thenDecrypted()}</li>
 *           <li>{@link #givenFindObjectSetting_whenDecryptOnSensitiveObject_thenDecrypted()}</li>
 *        </ul>
 *      </li>
 *      <li> FIND WITH MASK
 *        <ul>
 *          <li>{@link #givenFindStringSetting_whenMaskSensitive_thenMasked()}</li>
 *          <li>{@link #givenFindObjectSetting_whenMaskOnSensitiveObject_thenMasked()}</li>
 *        </ul>
 *      </li>
 * </li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
public class SettingEncryptionServiceTest {

    private DynamicSettingsEncryptionService dynamicSettingsEncryptionService;

    @Before
    public void init() throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        // init gson
        GsonUtil.setGson(GsonCustomizer.gsonBuilder(Optional.empty(), Optional.empty()).create());
        // init encryption service
        AESEncryptionService aesEncryptionService = new AESEncryptionService();
        aesEncryptionService.init(new CipherProperties("src/test/resources/testKey", "1234567812345678"));
        this.dynamicSettingsEncryptionService = new DynamicSettingsEncryptionService(aesEncryptionService,
                                                                                     new StringSensitiveAnnotationEncryptionService(
                                                                                         aesEncryptionService));
    }

    @Test
    public void givenCreatedStringSetting_whenEncryptSensitive_thenEncrypted() {
        // GIVEN
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       "settingTest",
                                                                       "this is not a description",
                                                                       "default-value",
                                                                       "actual-value",
                                                                       false);
        // WHEN
        DynamicTenantSetting encryptedSetting = dynamicSettingsEncryptionService.encryptSensitiveValues(dynamicSetting,
                                                                                                        null);

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 "settingTest",
                                                                                 "this is not a description",
                                                                                 "default-value",
                                                                                 "6Dk7tXm5jRS73iHfwcXiuw==",
                                                                                 false);
        Assertions.assertThat((String) encryptedSetting.getValue()).isEqualTo(expectedEncryptedSetting.getValue());
    }

    @Test
    public void givenCreatedObjectSetting_whenEncryptSensitive_thenEncrypted() {
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
                                                                       false);
        // WHEN
        DynamicTenantSetting encryptedSetting = dynamicSettingsEncryptionService.encryptSensitiveValues(dynamicSetting,
                                                                                                        null);

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 "settingTest",
                                                                                 "this is not a description",
                                                                                 defaultSensitiveObject,
                                                                                 new SensitiveObject("random name",
                                                                                                     new SensitiveComponent(
                                                                                                         1123,
                                                                                                         "fDSu9y3dSnhBZMlCgTZynQ==")),
                                                                                 false);
        Assertions.assertThat((SensitiveObject) encryptedSetting.getValue())
                  .isEqualTo(expectedEncryptedSetting.getValue());
    }

    @Test
    public void givenUpdatedStringSetting_whenEncryptSensitive_thenEncrypted() {
        // GIVEN
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       "settingTest",
                                                                       "this is not a description",
                                                                       "default-value",
                                                                       "it's a new secret!",
                                                                       false);
        // WHEN
        DynamicTenantSetting encryptedSetting = dynamicSettingsEncryptionService.encryptSensitiveValues(dynamicSetting,
                                                                                                        new DynamicTenantSetting(
                                                                                                            1L,
                                                                                                            "settingTest",
                                                                                                            "this is not a description",
                                                                                                            "default-value",
                                                                                                            "6Dk7tXm5jRS73iHfwcXiuw==",
                                                                                                            false));

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 "settingTest",
                                                                                 "this is not a description",
                                                                                 "default-value",
                                                                                 "z/9HkDdTC7ZpRG5JDH3fD2iFu/0Yrcdp/KPk/LT70HI=",
                                                                                 false);
        Assertions.assertThat((String) encryptedSetting.getValue()).isEqualTo(expectedEncryptedSetting.getValue());
    }

    @Test
    public void givenUpdatedObjectSetting_whenNoUpdateOnSensitive_thenNotEncrypted() {
        // GIVEN
        SensitiveObject defaultSensitiveObject = new SensitiveObject("default random name",
                                                                     new SensitiveComponent(1123, "default secret"));
        // just update object name, secret must remain the same
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       "settingTest",
                                                                       "this is not a description",
                                                                       defaultSensitiveObject,
                                                                       new SensitiveObject("it's a new random name",
                                                                                           new SensitiveComponent(1123,
                                                                                                                  "6Dk7tXm5jRS73iHfwcXiuw==")),
                                                                       false);
        // WHEN
        DynamicTenantSetting encryptedSetting = dynamicSettingsEncryptionService.encryptSensitiveValues(dynamicSetting,
                                                                                                        new DynamicTenantSetting(
                                                                                                            1L,
                                                                                                            "settingTest",
                                                                                                            "this is not a description",
                                                                                                            defaultSensitiveObject,
                                                                                                            new SensitiveObject(
                                                                                                                "it was a random name",
                                                                                                                new SensitiveComponent(
                                                                                                                    1123,
                                                                                                                    "6Dk7tXm5jRS73iHfwcXiuw==")),
                                                                                                            false));

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 "settingTest",
                                                                                 "this is not a description",
                                                                                 defaultSensitiveObject,
                                                                                 new SensitiveObject(
                                                                                     "it's a new random name",
                                                                                     new SensitiveComponent(1123,
                                                                                                            "6Dk7tXm5jRS73iHfwcXiuw==")),
                                                                                 false);
        Assertions.assertThat((SensitiveObject) encryptedSetting.getValue())
                  .isEqualTo(expectedEncryptedSetting.getValue());
    }

    @Test
    public void givenUpdatedObjectSetting_whenUpdateOnSensitiveObject_thenEncrypted() {
        // GIVEN
        SensitiveObject defaultSensitiveObject = new SensitiveObject("default random name",
                                                                     new SensitiveComponent(1123, "default secret"));
        // just update object name, secret must remain the same
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       "settingTest",
                                                                       "this is not a description",
                                                                       defaultSensitiveObject,
                                                                       new SensitiveObject("random name",
                                                                                           new SensitiveComponent(1123,
                                                                                                                  "providing a new secret")),
                                                                       false);
        // WHEN
        DynamicTenantSetting encryptedSetting = dynamicSettingsEncryptionService.encryptSensitiveValues(dynamicSetting,
                                                                                                        new DynamicTenantSetting(
                                                                                                            1L,
                                                                                                            "settingTest",
                                                                                                            "this is not a description",
                                                                                                            defaultSensitiveObject,
                                                                                                            new SensitiveObject(
                                                                                                                "random name",
                                                                                                                new SensitiveComponent(
                                                                                                                    1123,
                                                                                                                    "6Dk7tXm5jRS73iHfwcXiuw==")),
                                                                                                            false));

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 "settingTest",
                                                                                 "this is not a description",
                                                                                 defaultSensitiveObject,
                                                                                 new SensitiveObject("random name",
                                                                                                     new SensitiveComponent(
                                                                                                         1123,
                                                                                                         "CzQ23bRBPgaGKMv9C19eb9OQtV2JEhNKbTCeEtPZFTs=")),
                                                                                 false);
        Assertions.assertThat((SensitiveObject) encryptedSetting.getValue())
                  .isEqualTo(expectedEncryptedSetting.getValue());
    }

    @Test
    public void givenFindStringSetting_whenDecryptSensitive_thenDecrypted() {
        // GIVEN
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       "settingTest",
                                                                       "this is not a description",
                                                                       "default-value",
                                                                       "bQMClYSU+Ol+0WXBSccKoyOt0ttAfMYqyLV+XpJABlw=",
                                                                       false);
        // WHEN
        DynamicTenantSetting decryptedSetting = dynamicSettingsEncryptionService.decryptSensitiveValues(dynamicSetting);

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 "settingTest",
                                                                                 "this is not a description",
                                                                                 "default-value",
                                                                                 "it's an ancient secret!",
                                                                                 false);
        Assertions.assertThat((String) decryptedSetting.getValue()).isEqualTo(expectedEncryptedSetting.getValue());
    }

    @Test
    public void givenFindObjectSetting_whenDecryptOnSensitiveObject_thenDecrypted() {
        // GIVEN
        SensitiveObject defaultSensitiveObject = new SensitiveObject("default random name",
                                                                     new SensitiveComponent(1123, "default secret"));
        // just update object name, secret must remain the same
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       "settingTest",
                                                                       "this is not a description",
                                                                       defaultSensitiveObject,
                                                                       new SensitiveObject("random name",
                                                                                           new SensitiveComponent(1123,
                                                                                                                  "fDSu9y3dSnhBZMlCgTZynQ==")),
                                                                       false);
        // WHEN
        DynamicTenantSetting decryptedSetting = dynamicSettingsEncryptionService.decryptSensitiveValues(dynamicSetting);

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 "settingTest",
                                                                                 "this is not a description",
                                                                                 defaultSensitiveObject,
                                                                                 new SensitiveObject("random name",
                                                                                                     new SensitiveComponent(
                                                                                                         1123,
                                                                                                         "it's a secret!")),
                                                                                 false);
        Assertions.assertThat((SensitiveObject) decryptedSetting.getValue())
                  .isEqualTo(expectedEncryptedSetting.getValue());
    }

    @Test
    public void givenFindStringSetting_whenMaskSensitive_thenMasked() {
        // GIVEN
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       "settingTest",
                                                                       "this is not a description",
                                                                       "default-value",
                                                                       "bQMClYSU+Ol+0WXBSccKoyOt0ttAfMYqyLV+XpJABlw=",
                                                                       false);
        // WHEN
        DynamicTenantSetting maskedSetting = dynamicSettingsEncryptionService.maskSensitiveValues(dynamicSetting);

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 "settingTest",
                                                                                 "this is not a description",
                                                                                 "default-value",
                                                                                 ISensitiveAnnotationEncryptionService.MASK_PATTERN,
                                                                                 false);
        Assertions.assertThat((String) maskedSetting.getValue()).isEqualTo(expectedEncryptedSetting.getValue());
    }

    @Test
    public void givenFindObjectSetting_whenMaskOnSensitiveObject_thenMasked() {
        // GIVEN
        SensitiveObject defaultSensitiveObject = new SensitiveObject("default random name",
                                                                     new SensitiveComponent(1123, "default secret"));
        // just update object name, secret must remain the same
        DynamicTenantSetting dynamicSetting = new DynamicTenantSetting(1L,
                                                                       "settingTest",
                                                                       "this is not a description",
                                                                       defaultSensitiveObject,
                                                                       new SensitiveObject("random name",
                                                                                           new SensitiveComponent(1123,
                                                                                                                  "fDSu9y3dSnhBZMlCgTZynQ==")),
                                                                       false);
        // WHEN
        DynamicTenantSetting maskedSetting = dynamicSettingsEncryptionService.maskSensitiveValues(dynamicSetting);

        // THEN
        DynamicTenantSetting expectedEncryptedSetting = new DynamicTenantSetting(1L,
                                                                                 "settingTest",
                                                                                 "this is not a description",
                                                                                 defaultSensitiveObject,
                                                                                 new SensitiveObject("random name",
                                                                                                     new SensitiveComponent(
                                                                                                         1123,
                                                                                                         ISensitiveAnnotationEncryptionService.MASK_PATTERN)),
                                                                                 false);
        Assertions.assertThat((SensitiveObject) maskedSetting.getValue())
                  .isEqualTo(expectedEncryptedSetting.getValue());
    }
}
