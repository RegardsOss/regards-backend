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
package fr.cnes.regards.modules.model.service.validation.validator.iproperty.restriction;

import com.google.common.collect.Range;
import fr.cnes.regards.modules.model.domain.attributes.restriction.IntegerRangeRestriction;
import fr.cnes.regards.modules.model.dto.properties.IntegerArrayProperty;
import fr.cnes.regards.modules.model.dto.properties.IntegerIntervalProperty;
import fr.cnes.regards.modules.model.dto.properties.IntegerProperty;
import fr.cnes.regards.modules.model.service.validation.validator.common.restriction.AbstractIntegerRangeValidator;

/**
 * Validate {@link IntegerProperty}, {@link IntegerArrayProperty} or {@link IntegerIntervalProperty} with a
 * {@link IntegerRangeRestriction}
 *
 * @author Marc Sordi
 */
public class IntegerRangePropertyValidator extends AbstractIntegerRangeValidator {

    public IntegerRangePropertyValidator(IntegerRangeRestriction pRestriction, String pAttributeKey) {
        super(pRestriction, pAttributeKey);
    }

    @Override
    protected Integer getIntegerValue(Object pTarget) {
        return ((IntegerProperty) pTarget).getValue();
    }

    @Override
    protected Integer[] getIntegerArrayValue(Object pTarget) {
        return ((IntegerArrayProperty) pTarget).getValue();
    }

    @Override
    protected Range<Integer> getIntegerIntervalValue(Object pTarget) {
        return ((IntegerIntervalProperty) pTarget).getValue();
    }

    @Override
    protected boolean isInteger(Class clazz) {
        return clazz == IntegerProperty.class;
    }

    @Override
    protected boolean isIntegerArray(Class clazz) {
        return clazz == IntegerArrayProperty.class;
    }

    @Override
    protected boolean isIntegerInterval(Class clazz) {
        return clazz == IntegerIntervalProperty.class;
    }

}
