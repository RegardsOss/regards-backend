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
package fr.cnes.regards.framework.encryption.sensitive;

import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Encrypt, decrypt or mask recursively annotated {@link StringSensitive} fields within an object.
 *
 * @author Iliana Ghazali
 **/
public class StringSensitiveAnnotationEncryptionService implements ISensitiveAnnotationEncryptionService {

    public static final String CLASS_IN_REGARDS_PACKAGE_STARTS = "fr.cnes.regards.";

    private final IEncryptionService encryptionService;

    public StringSensitiveAnnotationEncryptionService(IEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public Object encryptObjectWithSensitiveValues(Object objectWithSensitiveValues,
                                                   @Nullable Object oldObjectWithSensitiveValues)
        throws EncryptionException {
        Field[] fields = objectWithSensitiveValues.getClass().getDeclaredFields();
        for (Field field : fields) {
            // update string field to encrypt
            if (field.isAnnotationPresent(StringSensitive.class) && String.class.isAssignableFrom(field.getType())) {
                encryptStringSensitiveField(objectWithSensitiveValues, field, oldObjectWithSensitiveValues);
            } else if (field.getType().getName().startsWith(CLASS_IN_REGARDS_PACKAGE_STARTS)) {
                // recursively find field to encrypt
                encryptObjectWithSensitiveValues(getField(objectWithSensitiveValues, field),
                                                 oldObjectWithSensitiveValues != null ?
                                                     getField(oldObjectWithSensitiveValues, field) :
                                                     null);
            }
        }
        return objectWithSensitiveValues;
    }

    @Override
    public Object decryptOrMaskObjectWithSensitiveValues(Object objectWithSensitiveValues, boolean maskValues)
        throws EncryptionException {
        Field[] fields = objectWithSensitiveValues.getClass().getDeclaredFields();
        for (Field field : fields) {
            // update string field to decrypt
            if (field.isAnnotationPresent(StringSensitive.class) && String.class.isAssignableFrom(field.getType())) {
                decryptOrMaskStringSensitiveField(objectWithSensitiveValues, field, maskValues);
            } else if (field.getType().getName().startsWith(CLASS_IN_REGARDS_PACKAGE_STARTS)) {
                // recursively find field to decrypt
                decryptOrMaskObjectWithSensitiveValues(getField(objectWithSensitiveValues, field), maskValues);
            }
        }
        return objectWithSensitiveValues;
    }

    private void encryptStringSensitiveField(Object objectWithSensitiveValues,
                                             Field field,
                                             @Nullable Object oldObjectWithSensitiveValues) throws EncryptionException {
        String fieldValue = (String) getField(objectWithSensitiveValues, field);
        if (oldObjectWithSensitiveValues != null) {
            String oldEncryptedFieldValue = (String) getField(oldObjectWithSensitiveValues, field);
            // means value has to be updated if different from pattern or previous already encrypted value
            if (!fieldValue.equals(MASK_PATTERN) && !fieldValue.equals(oldEncryptedFieldValue)) {
                updateField(objectWithSensitiveValues, field, encryptionService.encrypt(fieldValue));
            } else {
                // means value has not changed
                updateField(objectWithSensitiveValues, field, oldEncryptedFieldValue);
            }
        } else {
            // means value is set for the first time
            updateField(objectWithSensitiveValues, field, encryptionService.encrypt(fieldValue));
        }
    }

    private void decryptOrMaskStringSensitiveField(Object objectWithSensitiveValues, Field field, boolean maskValues)
        throws EncryptionException {
        String decryptedOrMaskedValue = maskValues ?
            MASK_PATTERN :
            encryptionService.decrypt((String) getField(objectWithSensitiveValues, field));
        updateField(objectWithSensitiveValues, field, decryptedOrMaskedValue);

    }

    private void updateField(Object objectWithSensitiveValues, Field field, String fieldValue)
        throws EncryptionException {
        try {
            field.setAccessible(true);
            field.set(objectWithSensitiveValues, fieldValue);
        } catch (IllegalAccessException e) {
            throw new EncryptionException(String.format("Could not update field '%s' by reflection.", field.getName()),
                                          e);
        }
    }

    private Object getField(Object objectWithSensitiveValues, Field field) throws EncryptionException {
        try {
            field.setAccessible(true);
            return field.get(objectWithSensitiveValues);
        } catch (IllegalAccessException e) {
            throw new EncryptionException(String.format("Could not access field '%s' by reflection.", field.getName()),
                                          e);
        }
    }
}
