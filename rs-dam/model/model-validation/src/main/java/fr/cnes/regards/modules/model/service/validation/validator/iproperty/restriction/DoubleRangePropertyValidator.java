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
import fr.cnes.regards.modules.model.domain.attributes.restriction.DoubleRangeRestriction;
import fr.cnes.regards.modules.model.dto.properties.DoubleArrayProperty;
import fr.cnes.regards.modules.model.dto.properties.DoubleIntervalProperty;
import fr.cnes.regards.modules.model.dto.properties.DoubleProperty;
import fr.cnes.regards.modules.model.service.validation.validator.common.restriction.AbstractDoubleRangeValidator;

/**
 * Validate {@link DoubleProperty}, {@link DoubleArrayProperty} or {@link DoubleIntervalProperty} value with a
 * {@link DoubleRangeRestriction}
 *
 * @author Marc Sordi
 */
public class DoubleRangePropertyValidator extends AbstractDoubleRangeValidator {

    public DoubleRangePropertyValidator(DoubleRangeRestriction pRestriction, String pAttributeKey) {
        super(pRestriction, pAttributeKey);
    }

    @Override
    protected Double getDoubleValue(Object pTarget) {
        return ((DoubleProperty) pTarget).getValue();
    }

    @Override
    protected Double[] getDoubleArrayValue(Object pTarget) {
        return ((DoubleArrayProperty) pTarget).getValue();
    }

    @Override
    protected Range<Double> getDoubleIntervalValue(Object pTarget) {
        return ((DoubleIntervalProperty) pTarget).getValue();
    }

    @Override
    protected boolean isDouble(Class clazz) {
        return clazz == DoubleProperty.class;
    }

    @Override
    protected boolean isDoubleArray(Class clazz) {
        return clazz == DoubleArrayProperty.class;
    }

    @Override
    protected boolean isDoubleInterval(Class clazz) {
        return clazz == DoubleIntervalProperty.class;
    }
}
