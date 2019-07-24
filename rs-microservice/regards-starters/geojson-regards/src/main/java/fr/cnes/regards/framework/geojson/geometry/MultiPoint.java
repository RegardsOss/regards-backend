/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.coordinates.Positions;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson MultiPoint representation
 * @author Marc Sordi
 */
public class MultiPoint extends AbstractGeometry<Positions> {

    public MultiPoint() {
        super(GeoJsonType.MULTIPOINT);
        coordinates = new Positions();
    }

    public MultiPoint(GeoJsonType type) {
        super(type);
        coordinates = new Positions();
    }

    @Override
    public <T> T accept(IGeometryVisitor<T> visitor) {
        return visitor.visitMultiPoint(this);
    }

    @Override
    public String toString() {
        return "MULTI POINTS ( " + getCoordinates().toString() + " )";
    }

    public double[][] toArray() {
        return coordinates.toArray();
    }

    /**
     * Create a MultiPoint from array  { { longitude, latitude }, {}, ... }
     * <B>NOTE: the goal of this method is to ease creation/transformation/computation of geometries so no check is
     * done concerning input values.</B>
     */
    public static MultiPoint fromArray(double[][] lonLats) {
        MultiPoint multiPoint = new MultiPoint();
        multiPoint.coordinates = Positions.fromArray(lonLats);
        return multiPoint;
    }

}
