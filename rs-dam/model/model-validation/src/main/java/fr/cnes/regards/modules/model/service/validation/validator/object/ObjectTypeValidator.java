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

package fr.cnes.regards.modules.model.service.validation.validator.object;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping;
import org.springframework.validation.Errors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Validates that an object is the right type and is coherent with that type in case of complex objects
 *
 * @author Thibaud Michaudel
 **/
public class ObjectTypeValidator extends AbstractObjectValidator {

    private final PropertyType propertyType;

    public ObjectTypeValidator(PropertyType attributeType, String attributeKey) {
        super(attributeKey);
        propertyType = attributeType;
    }

    @Override
    public void validate(Object target, Errors errors) {
        boolean error = false;
        switch (propertyType) {
            case STRING:
                error = !String.class.isAssignableFrom(target.getClass());
                break;
            case INTEGER:
                error = !isInteger(target);
                break;
            case DOUBLE:
                error = !Double.class.isAssignableFrom(target.getClass());
                break;
            case LONG:
                error = !Long.class.isAssignableFrom(target.getClass());
                break;
            case BOOLEAN:
                error = !Boolean.class.isAssignableFrom(target.getClass());
                break;
            case URL:
                error = !isAnUrl(target);
                break;
            case DATE_ISO8601:
                error = !isADate(target);
                break;
            case STRING_ARRAY:
                error = !isArrayList(target, String.class);
                break;
            case INTEGER_ARRAY:
                error = !isIntegerArrayList(target);
                break;
            case DOUBLE_ARRAY:
                error = !isArrayList(target, Double.class);
                break;
            case LONG_ARRAY:
                error = !isArrayList(target, Long.class);
                break;
            case DATE_ARRAY:
                error = !isDateArray(target);
                break;
            case INTEGER_INTERVAL:
                error = !isIntegerIntervalValid(target);
                break;
            case DOUBLE_INTERVAL:
                error = !isIntervalValid(target, Double.class);
                break;
            case LONG_INTERVAL:
                error = !isIntervalValid(target, Long.class);
                break;
            case DATE_INTERVAL:
                error = !isDateIntervalValid(target);
                break;
            case JSON:
                error = !isJson(target);
                break;
            case OBJECT:
            default:
                break;

        }
        if (error) {
            errors.reject("error.inconsistent.property.type.message",
                          String.format("Property \"%s\" not consistent with model attribute type.", attributeKey));
        }
    }

    private boolean isIntegerIntervalValid(Object target) {
        if (!Map.class.isAssignableFrom(target.getClass())) {
            return false;
        }
        Map interval = (Map) target;
        Object lowerBound = interval.get(IntervalMapping.RANGE_LOWER_BOUND);
        if (lowerBound == null || !isInteger(lowerBound)) {
            return false;
        }
        Object upperBound = interval.get(IntervalMapping.RANGE_UPPER_BOUND);
        if (upperBound == null || !isInteger(upperBound)) {
            return false;
        }
        return true;
    }

    private boolean isInteger(Object target) {
        return (Integer.class.isAssignableFrom(target.getClass()) || (Long.class.isAssignableFrom(target.getClass())
                                                                      && ((Long) target < Integer.MAX_VALUE)
                                                                      && ((Long) target > Integer.MIN_VALUE)));
    }

    private boolean isIntegerArrayList(Object target) {
        if (!List.class.isAssignableFrom(target.getClass())) {
            return false;
        }
        if (((List) target).stream().anyMatch(t -> !isInteger(t))) {
            return false;
        }
        return true;
    }

    private boolean isArrayList(Object target, Class<?> objectType) {
        if (!List.class.isAssignableFrom(target.getClass())) {
            return false;
        }
        if (((List) target).stream().anyMatch(t -> !objectType.isAssignableFrom(t.getClass()))) {
            return false;
        }
        return true;

    }

    private boolean isJson(Object target) {
        if (!Map.class.isAssignableFrom(target.getClass())) {
            return false;
        }
        Gson gson = new Gson();
        try {
            gson.toJson(target);
        } catch (JsonIOException e) {
            return false;
        }
        return true;
    }

    private boolean isDateArray(Object target) {
        if (List.class.isAssignableFrom(target.getClass())) {
            if (((List) target).stream().anyMatch(s -> !isADate(s))) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private boolean isAnUrl(Object target) {
        if (String.class.isAssignableFrom(target.getClass())) {
            try {
                new URL((String) target);
            } catch (MalformedURLException e) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private boolean isADate(Object target) {
        if (String.class.isAssignableFrom(target.getClass())) {
            try {
                OffsetDateTimeAdapter.parse((String) target);
            } catch (JsonIOException e) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private boolean isIntervalValid(Object target, Class<?> clazz) {
        if (!Map.class.isAssignableFrom(target.getClass())) {
            return false;
        }
        Map interval = (Map) target;
        Object lowerBound = interval.get(IntervalMapping.RANGE_LOWER_BOUND);
        if (lowerBound == null || !clazz.isAssignableFrom(lowerBound.getClass())) {
            return false;
        }
        Object upperBound = interval.get(IntervalMapping.RANGE_UPPER_BOUND);
        if (upperBound == null || !clazz.isAssignableFrom(upperBound.getClass())) {
            return false;
        }
        return true;
    }

    private boolean isDateIntervalValid(Object target) {
        if (!Map.class.isAssignableFrom(target.getClass())) {
            return false;
        }
        Map interval = (Map) target;
        Object lowerBound = interval.get(IntervalMapping.RANGE_LOWER_BOUND);
        if (lowerBound == null || !isADate(lowerBound)) {
            return false;
        } else {

        }
        Object upperBound = interval.get(IntervalMapping.RANGE_UPPER_BOUND);
        if (upperBound == null || !isADate(lowerBound)) {
            return false;
        }
        return true;
    }

}
