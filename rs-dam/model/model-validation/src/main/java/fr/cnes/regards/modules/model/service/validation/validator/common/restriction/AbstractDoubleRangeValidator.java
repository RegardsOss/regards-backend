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
import fr.cnes.regards.modules.model.domain.attributes.restriction.DoubleRangeRestriction;
import fr.cnes.regards.modules.model.service.validation.validator.common.AbstractValidator;
import org.springframework.validation.Errors;

/**
 * Validates double range with a {@link DoubleRangeRestriction}
 *
 * @author Thibaud Michaudel
 **/
public abstract class AbstractDoubleRangeValidator extends AbstractValidator {

    /**
     * Configured restriction
     */
    protected final DoubleRangeRestriction restriction;

    public AbstractDoubleRangeValidator(DoubleRangeRestriction pRestriction, String pAttributeKey) {
        super(pAttributeKey);
        restriction = pRestriction;
    }

    protected abstract Double getDoubleValue(Object pTarget);

    protected abstract Double[] getDoubleArrayValue(Object pTarget);

    protected abstract Range<Double> getDoubleIntervalValue(Object pTarget);

    protected abstract boolean isDouble(Class<?> clazz);

    protected abstract boolean isDoubleArray(Class<?> clazz);

    protected abstract boolean isDoubleInterval(Class<?> clazz);

    @Override
    public void validate(Object pTarget, Errors pErrors) {
        if (isDouble(pTarget.getClass())) {
            validate(getDoubleValue(pTarget), pErrors);
        } else if (isDoubleArray(pTarget.getClass())) {
            validate(getDoubleArrayValue(pTarget), pErrors);
        } else if (isDoubleInterval(pTarget.getClass())) {
            validate(getDoubleIntervalValue(pTarget), pErrors);
        } else {
            rejectUnsupported(pErrors);
        }
    }

    public void validate(Double pTarget, Errors pErrors) {
        checkRange(pTarget, pErrors);
    }

    public void validate(Double[] pTarget, Errors pErrors) {
        for (Double value : pTarget) {
            checkRange(value, pErrors);
        }
    }

    public void validate(Range<Double> pTarget, Errors pErrors) {
        checkRange(pTarget.lowerEndpoint(), pErrors);
        checkRange(pTarget.upperEndpoint(), pErrors);
    }

    /**
     * Check value is in restriction range
     *
     * @param pValue  value
     * @param pErrors errors
     */
    protected void checkRange(Double pValue, Errors pErrors) {
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

    @Override
    public boolean supports(Class<?> clazz) {
        return isDouble(clazz) || isDoubleArray(clazz) || isDoubleInterval(clazz);
    }

    protected void reject(Errors pErrors) {
        pErrors.reject("error.double.value.not.in.required.range",
                       String.format("Value not consistent with restriction range for attribute \"%s\".",
                                     attributeKey));
    }
}
