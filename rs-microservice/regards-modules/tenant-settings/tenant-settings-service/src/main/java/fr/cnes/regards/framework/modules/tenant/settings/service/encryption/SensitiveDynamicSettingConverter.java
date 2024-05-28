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
package fr.cnes.regards.framework.modules.tenant.settings.service.encryption;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import jakarta.annotation.Nullable;

/**
 * Convert {@link DynamicTenantSetting}s by encrypting, decrypting or masking sensitive values.
 *
 * @author Iliana Ghazali
 **/
public class SensitiveDynamicSettingConverter {

    private final DynamicSettingsEncryptionService dynamicSettingsEncryptionService;

    public SensitiveDynamicSettingConverter(DynamicSettingsEncryptionService dynamicSettingsEncryptionService) {
        this.dynamicSettingsEncryptionService = dynamicSettingsEncryptionService;
    }

    public DynamicTenantSetting getDynamicSettingWithSensitiveValues(DynamicTenantSetting dynamicTenantSetting,
                                                                     boolean decryptSensitiveValues) {
        if (dynamicTenantSetting.isContainsSensitiveParameters()
            && decryptSensitiveValues
            && dynamicTenantSetting.getValue() != null) {
            return dynamicSettingsEncryptionService.decryptSensitiveValues(dynamicTenantSetting);
        } else {
            return dynamicTenantSetting;
        }
    }

    public DynamicTenantSetting encryptDynamicSettingWithSensitiveValues(DynamicTenantSetting dynamicTenantSetting,
                                                                         @Nullable
                                                                         DynamicTenantSetting oldDynamicTenantSetting) {
        if (dynamicTenantSetting.isContainsSensitiveParameters() && dynamicTenantSetting.getValue() != null) {
            // in case of update, get old setting value to compare with new setting value
            return dynamicSettingsEncryptionService.encryptSensitiveValues(dynamicTenantSetting,
                                                                           oldDynamicTenantSetting);
        } else {
            // if setting does not contain sensitive setting
            return dynamicTenantSetting;
        }
    }

    public DynamicTenantSetting maskDynamicSettingWithSensitiveValues(DynamicTenantSetting dynamicTenantSetting) {
        if (dynamicTenantSetting.isContainsSensitiveParameters() && dynamicTenantSetting.getValue() != null) {
            return dynamicSettingsEncryptionService.maskSensitiveValues(dynamicTenantSetting);
        } else {
            return dynamicTenantSetting;
        }
    }
}
