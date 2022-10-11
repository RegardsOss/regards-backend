/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.model.service.validation.validator.common.restriction;

import fr.cnes.regards.modules.model.domain.attributes.restriction.PatternRestriction;
import fr.cnes.regards.modules.model.service.validation.validator.common.AbstractValidator;
import org.springframework.validation.Errors;

import java.util.regex.Pattern;

/**
 * Validates à String against a pattern
 *
 * @author Thibaud Michaudel
 **/
public abstract class AbstractPatternValidator extends AbstractValidator {

    /**
     * Configured restriction
     */
    private final PatternRestriction restriction;

    public AbstractPatternValidator(PatternRestriction pRestriction, String pAttributeKey) {
        super(pAttributeKey);
        this.restriction = pRestriction;
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
        if (!Pattern.matches(restriction.getPattern(), pTarget)) {
            reject(pErrors);
        }
    }

    public void validate(String[] pTarget, Errors pErrors) {
        for (String val : pTarget) {
            if (!Pattern.matches(restriction.getPattern(), val)) {
                reject(pErrors);
            }
        }
    }

    private void reject(Errors pErrors) {
        pErrors.reject("error.value.not.conform.to.pattern",
                       String.format("Value of attribute %s is not conform to pattern %s.",
                                     attributeKey,
                                     restriction.getPattern()));

    }

}
