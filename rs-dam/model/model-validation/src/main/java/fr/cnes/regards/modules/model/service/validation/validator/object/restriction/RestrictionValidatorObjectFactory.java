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
package fr.cnes.regards.modules.model.service.validation.validator.object.restriction;

import fr.cnes.regards.modules.model.domain.attributes.restriction.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Validator;

/**
 * Restriction validator factory for Object validators
 *
 * @author Thibaud Michaudel
 */
public final class RestrictionValidatorObjectFactory {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RestrictionValidatorObjectFactory.class);

    private RestrictionValidatorObjectFactory() {
    }

    public static Validator getValidator(AbstractRestriction restriction, String attributeKey) {
        if (restriction instanceof EnumerationRestriction enumerationRestriction) {
            return getValidator(enumerationRestriction, attributeKey);
        }
        if (restriction instanceof DoubleRangeRestriction doubleRangeRestriction) {
            return getValidator(doubleRangeRestriction, attributeKey);
        }
        if (restriction instanceof IntegerRangeRestriction integerRangeRestriction) {
            return getValidator(integerRangeRestriction, attributeKey);
        }
        if (restriction instanceof LongRangeRestriction longRangeRestriction) {
            return getValidator(longRangeRestriction, attributeKey);
        }
        if (restriction instanceof PatternRestriction patternRestriction) {
            return getValidator(patternRestriction, attributeKey);
        }
        if (restriction instanceof JsonSchemaRestriction jsonSchemaRestriction) {
            return getValidator(jsonSchemaRestriction, attributeKey);
        }

        String errorMessage = String.format("No validator found for restriction type %s and attribute %s.",
                                            restriction.getType(),
                                            attributeKey);
        LOGGER.debug(errorMessage);
        throw new UnsupportedOperationException(errorMessage);
    }

    public static Validator getValidator(EnumerationRestriction pRestriction, String pAttributeKey) {
        return new EnumerationObjectValidator(pRestriction, pAttributeKey);
    }

    public static Validator getValidator(DoubleRangeRestriction pRestriction, String pAttributeKey) {
        return new DoubleRangeObjectValidator(pRestriction, pAttributeKey);
    }

    public static Validator getValidator(IntegerRangeRestriction pRestriction, String pAttributeKey) {
        return new IntegerRangeObjectValidator(pRestriction, pAttributeKey);
    }

    public static Validator getValidator(LongRangeRestriction pRestriction, String pAttributeKey) {
        return new LongRangeObjectValidator(pRestriction, pAttributeKey);
    }

    public static Validator getValidator(PatternRestriction pRestriction, String pAttributeKey) {
        return new PatternObjectValidator(pRestriction, pAttributeKey);
    }

    public static Validator getValidator(JsonSchemaRestriction pRestriction, String pAttributeKey) {
        return new JsonSchemaObjectValidator(pRestriction, pAttributeKey);
    }
}
