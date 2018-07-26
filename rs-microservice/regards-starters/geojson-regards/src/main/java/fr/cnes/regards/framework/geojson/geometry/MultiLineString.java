/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.geojson.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.validator.MultiLineStringConstraints;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson MultiLineString representation
 * @author Marc Sordi
 */
@MultiLineStringConstraints
public class MultiLineString extends AbstractGeometry<List<Positions>> {

    public MultiLineString() {
        super(GeoJsonType.MULTILINESTRING);
        coordinates = new ArrayList<>();
    }

    @Override
    public <T> T accept(IGeometryVisitor<T> visitor) {
        return visitor.visitMultiLineString(this);
    }

    public double[][][] toArray() {
        return coordinates.stream().map(Positions::toArray).toArray(n -> new double[n][][]);
    }

    /**
     * Create a MultiLineString from array { { { longitude, latitude }, {}, ... } } (first is exterior ring, others holes)
     * <B>NOTE: the goal of this method is to ease creation/transformation/computation of geometries so no check is
     * done concerning input values.</B>
     */
    public static MultiLineString fromArray(double[][][] lonLatsArray) {
        MultiLineString multiLineString = new MultiLineString();
        multiLineString.coordinates.addAll(Arrays.asList(
                Arrays.stream(lonLatsArray).map(Positions::fromArray).toArray(n -> new Positions[n])));
        return multiLineString;
    }

}
