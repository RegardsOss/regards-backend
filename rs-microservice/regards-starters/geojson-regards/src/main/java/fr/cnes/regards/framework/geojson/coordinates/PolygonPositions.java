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
package fr.cnes.regards.framework.geojson.coordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import fr.cnes.regards.framework.geojson.validator.PolygonPositionsConstraints;

/**
 * Not in RFC 7946 -August 2016<br/>
 * GeoJson set of positions representation for polygon.<br/>
 *
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
@PolygonPositionsConstraints
public class PolygonPositions extends ArrayList<Positions> {

    public Positions getExteriorRing() {
        if (size() > 0) {
            return get(0);
        }
        return null; // Should not occur!
    }

    public List<Positions> getHoles() {
        if (size() > 1) {
            return subList(1, size());
        }
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("EXTERIOR ( ").append(getExteriorRing().toString()).append(" )");
        List<Positions> holes = getHoles();
        if (!holes.isEmpty()) {
            buf.append(", HOLES ( ");
            buf.append(holes.stream().map(Positions::toString).collect(Collectors.joining(", ", "HOLE ( ", " )")));
        }
        return buf.toString();
    }

    /**
     * Return polygon position as double[][][] (array of positions as double[][] where first is exterion ring and others
     * holes)
     */
    public double[][][] toArray() {
        return this.stream().map(Positions::toArray).toArray(n -> new double[n][][]);
    }

    /**
     * Create a PolygonPositions from array { { { longitude, latitude }, {}, ... } } (first is exterior ring, others holes)
     * <B>NOTE: the goal of this method is to ease creation/transformation/computation of geometries so no check is
     * done concerning input values.</B>
     */
    public static PolygonPositions fromArray(double[][][] lonLatsArray) {
        PolygonPositions polygonPositions = new PolygonPositions();
        Arrays.stream(lonLatsArray).forEach(lonLats -> polygonPositions.add(Positions.fromArray(lonLats)));
        return polygonPositions;
    }
}
