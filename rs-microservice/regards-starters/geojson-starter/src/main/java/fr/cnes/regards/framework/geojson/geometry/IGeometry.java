/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Arrays;

import fr.cnes.regards.framework.geojson.GeoJsonType;

/**
 *
 * RFC 7946 -August 2016<br/>
 * GeoJson geometry declaration and geometry builder.
 *
 * @author Marc Sordi
 *
 */
@FunctionalInterface
public interface IGeometry {

    GeoJsonType getType();

    /**
     * Define a GeoJson without geometry
     * @return {@link Unlocated}
     */
    static Unlocated unlocated() {
        return new Unlocated();
    }

    /**
     * Create a new {@link Position}
     * @param longitude longitude
     * @param latitude latitude
     * @param altitude altitude
     * @return {@link Position}
     */
    static Position position(Double longitude, Double latitude, Double altitude) {
        return new Position(longitude, latitude, altitude);
    }

    /**
     * Create a new {@link Position}
     * @param longitude longitude
     * @param latitude latitude
     * @return {@link Position}
     */
    static Position position(Double longitude, Double latitude) {
        return new Position(longitude, latitude);
    }

    /**
     * Create a new {@link Point} geometry
     * @param single single position required
     * @return {@link Point}
     */
    static Point point(Position single) {
        Point geometry = new Point();
        geometry.setCoordinates(single);
        return geometry;
    }

    /**
     * Create new {@link MultiPoint} geometry
     * @param positions multiple points. At least two is required.
     * @return {@link MultiPoint}
     */
    static MultiPoint multiPoint(Position... positions) {
        if ((positions == null) || (positions.length < 2)) {
            throw new IllegalArgumentException("At least two positions is required! Use point otherwise.");
        }
        MultiPoint geometry = new MultiPoint();
        geometry.getCoordinates().addAll(Arrays.asList(positions));
        return geometry;
    }

    /**
     * Utility method to create {@link LineString} coordinates. Useful for {@link MultiLineString} creation.
     * @param positions positions representing the line string. At least 2 positions is required.
     * @return a list of {@link Position}
     */
    static Positions lineStringCoordinates(Position... positions) {
        if ((positions == null) || (positions.length < 2)) {
            throw new IllegalArgumentException("At least two positions is required to make a line string!");
        }
        Positions result = new Positions();
        result.addAll(Arrays.asList(positions));
        return result;
    }

    /**
     * Create a new {@link LineString} geometry
     * @param positions positions representing the line string. Use {@link IGeometry#lineStringCoordinates(Position...)}
     *            to create line string coordinates.
     * @return {@link LineString}
     */
    static LineString lineString(Positions positions) {
        LineString geometry = new LineString();
        geometry.getCoordinates().addAll(positions);
        return geometry;
    }

    /**
     * Create a new {@link MultiLineString} geometry
     * @param lineStrings list of line strings. Use {@link IGeometry#lineStringCoordinates(Position...)} to create each
     *            line string coordinates.
     * @return {@link MultiLineString}
     */
    static MultiLineString multiLineString(Positions... lineStrings) {
        if ((lineStrings == null) || (lineStrings.length < 2)) {
            throw new IllegalArgumentException("At least two line strings is required! Use lineString otherwise.");
        }
        MultiLineString geometry = new MultiLineString();
        geometry.getCoordinates().addAll(Arrays.asList(lineStrings));
        return geometry;
    }
}
