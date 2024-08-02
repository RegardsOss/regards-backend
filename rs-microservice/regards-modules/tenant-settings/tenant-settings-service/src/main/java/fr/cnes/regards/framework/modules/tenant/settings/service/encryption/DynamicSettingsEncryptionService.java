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
package fr.cnes.regards.framework.modules.tenant.settings.service.encryption;

import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.encryption.sensitive.ISensitiveAnnotationEncryptionService;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nullable;

/**
 * Encrypt, decrypt or mask string sensitive values contained in {@link DynamicTenantSetting}s.
 * <p>
 * Note that sensitive values are only handled in string settings or in REGARDS objects with string sensitive
 * annotations.
 * </p>
 *
 * @author Iliana Ghazali
 **/
public class DynamicSettingsEncryptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSettingsEncryptionService.class);

    private final IEncryptionService encryptionService;

    private final ISensitiveAnnotationEncryptionService sensitiveEncryptionService;

    public DynamicSettingsEncryptionService(IEncryptionService encryptionService,
                                            ISensitiveAnnotationEncryptionService sensitiveEncryptionService) {
        this.encryptionService = encryptionService;
        this.sensitiveEncryptionService = sensitiveEncryptionService;
    }

    // ---------------
    // --- ENCRYPT ---
    // ---------------

    /**
     * Encrypt string sensitive {@link DynamicTenantSetting} values recursively in case of create or update. If
     * oldDynamicTenantSetting is provided, a comparison is done with the current dynamicTenantSetting to see if the
     * value has actually changed and needs to be updated.
     *
     * @param dynamicTenantSetting    current {@link DynamicTenantSetting} with sensitive values to create or update
     * @param oldDynamicTenantSetting previous value of {@link DynamicTenantSetting}. Can be nullable.
     * @return new {@link DynamicTenantSetting} with sensitive values encrypted.
     */
    public DynamicTenantSetting encryptSensitiveValues(DynamicTenantSetting dynamicTenantSetting,
                                                       @Nullable DynamicTenantSetting oldDynamicTenantSetting) {
        Object encryptedValue;
        String fullyQualifiedClassName = dynamicTenantSetting.getClassName();
        // in case of string value, encrypt directly dynamic setting
        if (fullyQualifiedClassName.equals(String.class.getName())) {
            encryptedValue = encryptStringSetting(dynamicTenantSetting, oldDynamicTenantSetting);
        } else if (fullyQualifiedClassName.startsWith(ISensitiveAnnotationEncryptionService.CLASS_IN_REGARDS_PACKAGE_STARTS)) {
            // in case of object value, search for sensitive annotations to encrypt setting
            // provide old dynamic setting value for comparison in case of update
            if (oldDynamicTenantSetting != null) {
                encryptedValue = sensitiveEncryptionService.encryptObjectWithSensitiveValues(dynamicTenantSetting.getValue(),
                                                                                             oldDynamicTenantSetting.getValue());
            } else {
                encryptedValue = sensitiveEncryptionService.encryptObjectWithSensitiveValues(dynamicTenantSetting.getValue());
            }
        } else {
            // by default, leave value as it is if it cannot be handled
            encryptedValue = dynamicTenantSetting.getValue();
        }
        return new DynamicTenantSetting(dynamicTenantSetting.getId(),
                                        dynamicTenantSetting.getName(),
                                        dynamicTenantSetting.getDescription(),
                                        dynamicTenantSetting.getDefaultValue(),
                                        encryptedValue,
                                        dynamicTenantSetting.isContainsSensitiveParameters());
    }

    private String encryptStringSetting(DynamicTenantSetting dynamicTenantSetting,
                                        @Nullable DynamicTenantSetting oldDynamicTenantSetting) {
        String actualSettingValue = dynamicTenantSetting.getValue(String.class);
        String encryptedValue;
        try {
            if (oldDynamicTenantSetting != null) {
                String oldSettingValue = oldDynamicTenantSetting.getValue(String.class);
                // means value has to be updated if different from pattern or previous already encrypted value
                if (!actualSettingValue.equals(sensitiveEncryptionService.MASK_PATTERN) && !actualSettingValue.equals(
                    oldSettingValue)) {
                    encryptedValue = encryptionService.encrypt(actualSettingValue);

                } else {
                    // means value has not changed
                    encryptedValue = oldSettingValue;
                }
            } else {
                // means value is set for the first time
                encryptedValue = encryptionService.encrypt(actualSettingValue);
            }
        } catch (EncryptionException e) {
            String errorMsg = String.format("Error encrypting sensitive parameter '%s'. Cause : %s.",
                                            dynamicTenantSetting.getName(),
                                            e.getMessage());
            LOGGER.error(errorMsg, e);
            encryptedValue = ISensitiveAnnotationEncryptionService.DEFAULT_ERROR_ENCODING_VALUE;
        }
        return encryptedValue;
    }

    // ---------------
    // --- DECRYPT ---
    // ---------------

    /**
     * Decrypt string sensitive {@link DynamicTenantSetting} values recursively.
     *
     * @param dynamicTenantSetting current {@link DynamicTenantSetting} with encrypted sensitive values
     * @return new {@link DynamicTenantSetting} with sensitive values decrypted.
     */
    public DynamicTenantSetting decryptSensitiveValues(DynamicTenantSetting dynamicTenantSetting) {
        Object decryptedValue;
        String fullyQualifiedClassName = dynamicTenantSetting.getClassName();
        // in case of string value, decrypt directly dynamic setting
        if (fullyQualifiedClassName.equals(String.class.getName())) {
            decryptedValue = decryptStringSetting(dynamicTenantSetting);
        } else if (fullyQualifiedClassName.startsWith(ISensitiveAnnotationEncryptionService.CLASS_IN_REGARDS_PACKAGE_STARTS)) {
            // in case of object value, search for sensitive annotations to decrypt setting
            decryptedValue = sensitiveEncryptionService.decryptObjectWithSensitiveValues(dynamicTenantSetting.getValue());
        } else {
            // by default, leave value as is if it cannot be handled
            decryptedValue = dynamicTenantSetting.getValue();
        }
        return new DynamicTenantSetting(dynamicTenantSetting.getId(),
                                        dynamicTenantSetting.getName(),
                                        dynamicTenantSetting.getDescription(),
                                        dynamicTenantSetting.getDefaultValue(),
                                        decryptedValue,
                                        dynamicTenantSetting.isContainsSensitiveParameters());
    }

    private String decryptStringSetting(DynamicTenantSetting dynamicTenantSetting) {
        String decryptedValue;
        try {
            decryptedValue = encryptionService.decrypt(dynamicTenantSetting.getValue(String.class));
        } catch (EncryptionException e) {
            String errorMsg = String.format("Error decrypting sensitive parameter '%s'. Cause : %s",
                                            dynamicTenantSetting.getName(),
                                            e.getMessage());
            LOGGER.error(errorMsg, e);
            decryptedValue = ISensitiveAnnotationEncryptionService.DEFAULT_ERROR_ENCODING_VALUE;
        }
        return decryptedValue;
    }

    // ------------
    // --- MASK ---
    // ------------

    /**
     * Mask string sensitive {@link DynamicTenantSetting}s recursively by replacing them with a predetermined pattern.
     *
     * @param dynamicTenantSetting current {@link DynamicTenantSetting} with sensitive values
     * @return new {@link DynamicTenantSetting} with sensitive values masked.
     */
    public DynamicTenantSetting maskSensitiveValues(DynamicTenantSetting dynamicTenantSetting) {
        Object maskedValue;
        String fullyQualifiedClassName = dynamicTenantSetting.getClassName();
        // in case of string value, mask directly dynamic setting
        if (fullyQualifiedClassName.equals(String.class.getName())) {
            maskedValue = sensitiveEncryptionService.MASK_PATTERN;
        } else if (fullyQualifiedClassName.startsWith(ISensitiveAnnotationEncryptionService.CLASS_IN_REGARDS_PACKAGE_STARTS)) {
            // in case of object value, search for sensitive annotations to mask setting
            maskedValue = sensitiveEncryptionService.maskObjectWithSensitiveValues(dynamicTenantSetting.getValue());
        } else {
            // by default, leave value as is if it cannot be handled
            maskedValue = dynamicTenantSetting.getValue();
        }
        return new DynamicTenantSetting(dynamicTenantSetting.getId(),
                                        dynamicTenantSetting.getName(),
                                        dynamicTenantSetting.getDescription(),
                                        dynamicTenantSetting.getDefaultValue(),
                                        maskedValue,
                                        dynamicTenantSetting.isContainsSensitiveParameters());
    }

}
