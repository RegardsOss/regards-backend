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

import com.google.common.collect.Range;
import fr.cnes.regards.modules.model.domain.attributes.restriction.LongRangeRestriction;
import fr.cnes.regards.modules.model.service.validation.validator.common.AbstractValidator;
import org.springframework.validation.Errors;

/**
 * Validates Long range with a {@link LongRangeRestriction}
 *
 * @author Thibaud Michaudel
 **/
public abstract class AbstractLongRangeValidator extends AbstractValidator {

    /**
     * Configured restriction
     */
    protected final LongRangeRestriction restriction;

    public AbstractLongRangeValidator(LongRangeRestriction pRestriction, String pAttributeKey) {
        super(pAttributeKey);
        restriction = pRestriction;
    }

    protected abstract Long getLongValue(Object pTarget);

    protected abstract Long[] getLongArrayValue(Object pTarget);

    protected abstract Range<Long> getLongIntervalValue(Object pTarget);

    protected abstract boolean isLong(Class<?> clazz);

    protected abstract boolean isLongArray(Class<?> clazz);

    protected abstract boolean isLongInterval(Class<?> clazz);

    @Override
    public void validate(Object pTarget, Errors pErrors) {
        if (isLong(pTarget.getClass())) {
            validate(getLongValue(pTarget), pErrors);
        } else if (isLongArray(pTarget.getClass())) {
            validate(getLongArrayValue(pTarget), pErrors);
        } else if (isLongInterval(pTarget.getClass())) {
            validate(getLongIntervalValue(pTarget), pErrors);
        } else {
            rejectUnsupported(pErrors);
        }
    }

    public void validate(Long pTarget, Errors pErrors) {
        checkRange(pTarget, pErrors);
    }

    public void validate(Long[] pTarget, Errors pErrors) {
        for (Long value : pTarget) {
            checkRange(value, pErrors);
        }
    }

    public void validate(Range<Long> pTarget, Errors pErrors) {
        checkRange(pTarget.lowerEndpoint(), pErrors);
        checkRange(pTarget.upperEndpoint(), pErrors);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return isLong(clazz) || isLongArray(clazz) || isLongInterval(clazz);
    }

    /**
     * Check value is in restriction range
     *
     * @param pValue  value
     * @param pErrors errors
     */
    private void checkRange(Long pValue, Errors pErrors) {
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
                            "error.Long.value.not.in.required.range",
                            "Value not constistent with restriction range.");
    }
}
