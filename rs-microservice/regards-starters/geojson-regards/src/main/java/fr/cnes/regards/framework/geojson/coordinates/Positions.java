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
package fr.cnes.regards.framework.geojson.coordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Not in RFC 7946 -August 2016<br/>
 * GeoJson consistent set of positions representation.<br/>
 * @author Marc Sordi
 */
@SuppressWarnings("serial")
public class Positions extends ArrayList<Position> {

    /**
     * Check that this set of positions is or not a line string. A line string has at least 2 positions.
     * @return true if line string
     */
    public boolean isLineString() {
        return size() > 1;
    }

    /**
     * Check that this set of positions is or not a linear ring. A linear ring has at least 4 positions. Its first and
     * latest positions MUST be equivalents.
     * @return true if linear ring.
     */
    public boolean isLinearRing() {
        // Check size
        if (size() < 4) {
            return false;
        }
        // Check if it's closed
        Position first = get(0);
        Position latest = get(size() - 1);
        return first.equals(latest);
    }

    @Override
    public String toString() {
        return stream().map(Position::toString).collect(Collectors.joining(" ], [ ", "[ ", " ]"));
    }

    /**
     * Return positions as double[][] (array of positions as double[] { longitude, latitude })
     */
    public double[][] toArray() {
        return this.stream().map(Position::toArray).toArray(double[][]::new);
    }

    /**
     * Create a Positions from array { { longitude, latitude }, {}, ... }
     * <B>NOTE: the goal of this method is to ease creation/transformation/computation of geometries so no check is
     * done concerning input values.</B>
     */
    public static Positions fromArray(double[][] lonLats) {
        Positions positions = new Positions();
        Arrays.stream(lonLats).forEach(lonLat -> positions.add(Position.fromArray(lonLat)));
        return positions;
    }
}
