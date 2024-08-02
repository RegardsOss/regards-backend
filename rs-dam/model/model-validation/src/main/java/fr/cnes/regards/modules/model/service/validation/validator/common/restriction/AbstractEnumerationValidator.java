package fr.cnes.regards.modules.model.service.validation.validator.common.restriction;/*
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

import fr.cnes.regards.modules.model.domain.attributes.restriction.EnumerationRestriction;
import fr.cnes.regards.modules.model.service.validation.validator.common.AbstractValidator;
import org.springframework.validation.Errors;

/**
 * Validate a string or string array with an {@link EnumerationRestriction}
 *
 * @author Thibaud Michaudel
 **/
public abstract class AbstractEnumerationValidator extends AbstractValidator {

    private final EnumerationRestriction restriction;

    public AbstractEnumerationValidator(EnumerationRestriction pRestriction, String pAttributeKey) {
        super(pAttributeKey);
        restriction = pRestriction;
    }

    protected abstract String getStringValue(Object pTarget);

    protected abstract String[] getStringArrayValue(Object pTarget);

    protected abstract boolean isString(Class<?> clazz);

    protected abstract boolean isStringArray(Class<?> clazz);

    @Override
    public boolean supports(Class<?> clazz) {
        return isString(clazz) || isStringArray(clazz);
    }

    @Override
    public void validate(Object pTarget, Errors pErrors) {
        if (isString(pTarget.getClass())) {
            validate(getStringValue(pTarget), pErrors);
        } else if (isStringArray(pTarget.getClass())) {
            validate(getStringArrayValue(pTarget), pErrors);
        } else {
            rejectUnsupported(pErrors);
        }
    }

    public void validate(String pTarget, Errors pErrors) {
        if (!restriction.getAcceptableValues().contains(pTarget)) {
            reject(pErrors);
        }
    }

    public void validate(String[] pTarget, Errors pErrors) {
        for (String val : pTarget) {
            if (!restriction.getAcceptableValues().contains(val)) {
                reject(pErrors);
            }
        }
    }

    private void reject(Errors pErrors) {
        pErrors.reject("error.enum.value.does.not.exist",
                       String.format("Value not acceptable for attribute \"%s\".", attributeKey));
    }

}
