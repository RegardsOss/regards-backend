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
import fr.cnes.regards.modules.model.domain.attributes.restriction.IntegerRangeRestriction;
import fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping;
import fr.cnes.regards.modules.model.service.validation.validator.common.restriction.AbstractIntegerRangeValidator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validates Integer range given as a map with a {@link DoubleRangeRestriction}
 *
 * @author Thibaud Michaudel
 **/
public class IntegerRangeObjectValidator extends AbstractIntegerRangeValidator {

    public IntegerRangeObjectValidator(IntegerRangeRestriction pRestriction, String pAttributeKey) {
        super(pRestriction, pAttributeKey);
    }

    @Override
    protected Integer getIntegerValue(Object pTarget) {
        if (Integer.class.isAssignableFrom(pTarget.getClass())) {
            return (Integer) pTarget;
        } else {
            return Math.toIntExact((Long) pTarget);
        }
    }

    @Override
    protected Integer[] getIntegerArrayValue(Object pTarget) {
        List numberList = (List) pTarget;
        if (numberList.isEmpty()) {
            return new Integer[0];
        }
        if (Integer.class.isAssignableFrom(numberList.get(0).getClass())) {
            return ((List<Integer>) numberList).toArray(new Integer[numberList.size()]);
        } else {
            //Long
            return ((List<Long>) numberList).stream()
                                            .map(n -> Math.toIntExact(n))
                                            .collect(Collectors.toList())
                                            .toArray(Integer[]::new);
        }

    }

    @Override
    protected Range<Integer> getIntegerIntervalValue(Object pTarget) {
        Map<String, Object> interval = (Map<String, Object>) pTarget;
        Object lowerBound = interval.get(IntervalMapping.RANGE_LOWER_BOUND);
        Object upperBound = interval.get(IntervalMapping.RANGE_UPPER_BOUND);
        if (Integer.class.isAssignableFrom(lowerBound.getClass())
            && Integer.class.isAssignableFrom(upperBound.getClass())) {
            return Range.closed((Integer) interval.get(IntervalMapping.RANGE_LOWER_BOUND),
                                (Integer) interval.get(IntervalMapping.RANGE_UPPER_BOUND));
        } else {
            //Long
            return Range.closed(Math.toIntExact((Long) interval.get(IntervalMapping.RANGE_LOWER_BOUND)),
                                Math.toIntExact((Long) interval.get(IntervalMapping.RANGE_UPPER_BOUND)));
        }
    }

    @Override
    protected boolean isInteger(Class clazz) {
        return Integer.class.isAssignableFrom(clazz) || Long.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean isIntegerArray(Class clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean isIntegerInterval(Class clazz) {
        return Map.class.isAssignableFrom(clazz);
    }
}
