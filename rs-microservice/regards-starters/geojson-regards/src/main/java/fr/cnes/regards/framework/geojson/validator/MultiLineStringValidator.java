/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.geojson.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.MultiLineString;
import fr.cnes.regards.framework.geojson.geometry.Polygon;

/**
 * Validate {@link Polygon} structure
 * @author Marc Sordi
 */
public class MultiLineStringValidator implements ConstraintValidator<MultiLineStringConstraints, MultiLineString> {

    @Override
    public void initialize(MultiLineStringConstraints constraintAnnotation) {
        // Nothing to do
    }

    @Override
    public boolean isValid(MultiLineString multiLineString, ConstraintValidatorContext context) {
        for (Positions lineString : multiLineString.getCoordinates()) {
            if (!lineString.isLineString()) {
                return false;
            }
        }
        return true;
    }

}
