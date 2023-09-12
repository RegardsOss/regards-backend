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

import fr.cnes.regards.framework.encryption.exception.EncryptionException;

import javax.annotation.Nullable;

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
     * Encrypt sensitive values contained in an object recursively.
     *
     * @param objectWithSensitiveValues    object to update with encrypted sensitive values
     * @param oldObjectWithSensitiveValues if old object value should be compared to object to encrypt in order to not
     *                                     encrypt sensitive values twice
     * @throws EncryptionException if a sensitive parameter could not be encrypted
     */
    Object encryptObjectWithSensitiveValues(Object objectWithSensitiveValues,
                                          @Nullable Object oldObjectWithSensitiveValues) throws EncryptionException;

    /**
     * Simplified version of {@link this#encryptObjectWithSensitiveValues(Object, Object)} where object will be updated with encrypted
     * values
     */
    default Object encryptObjectWithSensitiveValues(Object objectWithSensitiveValues) throws EncryptionException {
        return encryptObjectWithSensitiveValues(objectWithSensitiveValues, null);
    }

    /**
     * Decrypt or mask sensitive values contained in an object recursively.
     *
     * @throws EncryptionException if a sensitive parameter could not be decrypted
     */
    Object decryptOrMaskObjectWithSensitiveValues(Object objectWithSensitiveValues, boolean maskValues)
        throws EncryptionException;

    /**
     * Decrypt all sensitive values contained in the object.
     */
    default Object decryptObjectWithSensitiveValues(Object objectWithSensitiveValues) throws EncryptionException {
        return decryptOrMaskObjectWithSensitiveValues(objectWithSensitiveValues, false);
    }

    /**
     * Mask all sensitive values by replacing them with a predetermined pattern.
     */
    default Object maskObjectWithSensitiveValues(Object objectWithSensitiveValues) throws EncryptionException {
       return decryptOrMaskObjectWithSensitiveValues(objectWithSensitiveValues, true);
    }
}
