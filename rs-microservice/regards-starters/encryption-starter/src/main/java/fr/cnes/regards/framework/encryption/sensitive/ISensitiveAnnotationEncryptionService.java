package fr.cnes.regards.framework.encryption.sensitive;/*
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

import jakarta.annotation.Nullable;

/**
 * Interface to encrypt, decrypt or mask recursively annotated sensitive values.
 *
 * @author Iliana Ghazali
 **/

public interface ISensitiveAnnotationEncryptionService {

    /**
     * Generic pattern to mask secret values without revealing them
     */
    String MASK_PATTERN = "*******";

    /**
     * Value to replace in case the encoding or decoding could not be performed
     */
    String DEFAULT_ERROR_ENCODING_VALUE = "encoding-error";

    /**
     * To check if object belongs to REGARDS to search for sensitive annotations
     */
    String CLASS_IN_REGARDS_PACKAGE_STARTS = "fr.cnes.regards.";

    /**
     * Encrypt sensitive values contained in an object recursively.
     *
     * @param objectWithSensitiveValues    object to update with encrypted sensitive values
     * @param oldObjectWithSensitiveValues if old object value should be compared to object to encrypt in order to not
     *                                     encrypt sensitive values twice
     */
    Object encryptObjectWithSensitiveValues(Object objectWithSensitiveValues,
                                            @Nullable Object oldObjectWithSensitiveValues);

    /**
     * Simplified version of {@link this#encryptObjectWithSensitiveValues(Object, Object)} where object will be updated with encrypted
     * values
     */
    default Object encryptObjectWithSensitiveValues(Object objectWithSensitiveValues) {
        return encryptObjectWithSensitiveValues(objectWithSensitiveValues, null);
    }

    /**
     * Decrypt or mask sensitive values contained in an object recursively.
     */
    Object decryptOrMaskObjectWithSensitiveValues(Object objectWithSensitiveValues, boolean maskValues);

    /**
     * Decrypt all sensitive values contained in the object.
     */
    default Object decryptObjectWithSensitiveValues(Object objectWithSensitiveValues) {
        return decryptOrMaskObjectWithSensitiveValues(objectWithSensitiveValues, false);
    }

    /**
     * Mask all sensitive values by replacing them with a predetermined pattern.
     */
    default Object maskObjectWithSensitiveValues(Object objectWithSensitiveValues) {
        return decryptOrMaskObjectWithSensitiveValues(objectWithSensitiveValues, true);
    }
}
