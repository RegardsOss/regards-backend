/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.framework.oais.OAISDataObject;

/**
 * Validate an {@link OAISDataObject}
 * @author Marc Sordi *
 */

public class OAISDataObjectValidator implements ConstraintValidator<ValidOAISDataObject, OAISDataObject> {

    @Override
    public void initialize(ValidOAISDataObject constraintAnnotation) {
        // Nothing to do
    }

    @Override
    public boolean isValid(OAISDataObject value, ConstraintValidatorContext context) {

        // If reference is null, @NotNull annotation on OAISDataObject will throw a constraint violation
        if (value.isReference() != null) {
            if (value.isReference()) {
                return true;
            } else {
                boolean isValid = true;
                // Validate algorithm
                if ((value.getAlgorithm() == null) || value.getAlgorithm().isEmpty()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Data file checksum algorithm is required")
                            .addPropertyNode("algorithm").addConstraintViolation();
                    isValid = false;
                }
                // Validate checksum
                if ((value.getChecksum() == null) || value.getChecksum().isEmpty()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Data file checksum is required")
                            .addPropertyNode("checksum").addConstraintViolation();
                    isValid = false;
                }
                return isValid;
            }
        }
        return true;
    }

}
