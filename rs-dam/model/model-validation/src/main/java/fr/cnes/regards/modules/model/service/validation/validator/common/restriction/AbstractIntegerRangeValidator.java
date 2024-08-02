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

package fr.cnes.regards.modules.model.service.validation.validator.common.restriction;

import com.google.common.collect.Range;
import fr.cnes.regards.modules.model.domain.attributes.restriction.IntegerRangeRestriction;
import fr.cnes.regards.modules.model.service.validation.validator.common.AbstractValidator;
import org.springframework.validation.Errors;

/**
 * Validates integer range with a {@link IntegerRangeRestriction}
 *
 * @author Thibaud Michaudel
 **/
public abstract class AbstractIntegerRangeValidator extends AbstractValidator {

    /**
     * Configured restriction
     */
    protected final IntegerRangeRestriction restriction;

    public AbstractIntegerRangeValidator(IntegerRangeRestriction pRestriction, String pAttributeKey) {
        super(pAttributeKey);
        restriction = pRestriction;
    }

    protected abstract Integer getIntegerValue(Object pTarget);

    protected abstract Integer[] getIntegerArrayValue(Object pTarget);

    protected abstract Range<Integer> getIntegerIntervalValue(Object pTarget);

    protected abstract boolean isInteger(Class<?> clazz);

    protected abstract boolean isIntegerArray(Class<?> clazz);

    protected abstract boolean isIntegerInterval(Class<?> clazz);

    @Override
    public void validate(Object pTarget, Errors pErrors) {
        if (isInteger(pTarget.getClass())) {
            validate(getIntegerValue(pTarget), pErrors);
        } else if (isIntegerArray(pTarget.getClass())) {
            validate(getIntegerArrayValue(pTarget), pErrors);
        } else if (isIntegerInterval(pTarget.getClass())) {
            validate(getIntegerIntervalValue(pTarget), pErrors);
        } else {
            rejectUnsupported(pErrors);
        }
    }

    public void validate(Integer pTarget, Errors pErrors) {
        checkRange(pTarget, pErrors);
    }

    public void validate(Integer[] pTarget, Errors pErrors) {
        for (Integer value : pTarget) {
            checkRange(value, pErrors);
        }
    }

    public void validate(Range<Integer> pTarget, Errors pErrors) {
        checkRange(pTarget.lowerEndpoint(), pErrors);
        checkRange(pTarget.upperEndpoint(), pErrors);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return isInteger(clazz) || isIntegerArray(clazz) || isIntegerInterval(clazz);
    }

    /**
     * Check value is in restriction range
     *
     * @param pValue  value
     * @param pErrors errors
     */
    private void checkRange(Integer pValue, Errors pErrors) {
        if (restriction.isMinExcluded()) {
            if (pValue <= restriction.getMin()) {
                reject(pErrors);
            }
        } else {
            if (pValue < restriction.getMin()) {
                reject(pErrors);
            }
        }
        if (restriction.isMaxExcluded()) {
            if (pValue >= restriction.getMax()) {
                reject(pErrors);
            }
        } else {
            if (pValue > restriction.getMax()) {
                reject(pErrors);
            }
        }

    }

    private void reject(Errors pErrors) {
        pErrors.rejectValue(attributeKey,
                            "error.integer.value.not.in.required.range",
                            "Value not constistent with restriction range.");
    }
}
