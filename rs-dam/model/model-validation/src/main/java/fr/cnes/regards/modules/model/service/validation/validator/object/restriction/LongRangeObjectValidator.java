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

package fr.cnes.regards.modules.model.service.validation.validator.object.restriction;

import com.google.common.collect.Range;
import fr.cnes.regards.modules.model.domain.attributes.restriction.DoubleRangeRestriction;
import fr.cnes.regards.modules.model.domain.attributes.restriction.LongRangeRestriction;
import fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping;
import fr.cnes.regards.modules.model.service.validation.validator.common.restriction.AbstractLongRangeValidator;

import java.util.List;
import java.util.Map;

/**
 * Validates Long range given as a map with a {@link DoubleRangeRestriction}
 *
 * @author Thibaud Michaudel
 **/
public class LongRangeObjectValidator extends AbstractLongRangeValidator {

    public LongRangeObjectValidator(LongRangeRestriction pRestriction, String pAttributeKey) {
        super(pRestriction, pAttributeKey);
    }

    @Override
    protected Long getLongValue(Object pTarget) {
        return (Long) pTarget;
    }

    @Override
    protected Long[] getLongArrayValue(Object pTarget) {
        List numberList = (List) pTarget;
        if (numberList.isEmpty()) {
            return new Long[0];
        }
        return ((List<Long>) numberList).toArray(new Long[numberList.size()]);

    }

    @Override
    protected Range<Long> getLongIntervalValue(Object pTarget) {
        Map<String, Object> interval = (Map<String, Object>) pTarget;
        return Range.closed((Long) interval.get(IntervalMapping.RANGE_LOWER_BOUND),
                            (Long) interval.get(IntervalMapping.RANGE_UPPER_BOUND));
    }

    @Override
    protected boolean isLong(Class clazz) {
        return Long.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean isLongArray(Class clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean isLongInterval(Class clazz) {
        return Map.class.isAssignableFrom(clazz);
    }
}
