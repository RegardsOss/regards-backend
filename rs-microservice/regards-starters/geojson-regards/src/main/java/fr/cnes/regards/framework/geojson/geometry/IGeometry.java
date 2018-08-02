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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;
import fr.cnes.regards.framework.geojson.coordinates.Position;
import fr.cnes.regards.framework.geojson.coordinates.Positions;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson geometry declaration and geometry builder.
 * @author Marc Sordi
 */
public interface IGeometry { // NOSONAR

    Logger LOGGER = LoggerFactory.getLogger(IGeometry.class);

    GeoJsonType getType();

    void setCrs(String crs);

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
     * Create a {@link Positions}
     */
    static Positions positions(Position[] positions) {
        Positions positions1 = new Positions();
        positions1.addAll(Arrays.asList(positions));
        return positions1;
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
        assertNotNull(positions, "Positions cannot be null.");
        assertMinLength(positions, 2, "At least two positions is required! Use point otherwise.");

        MultiPoint geometry = new MultiPoint();
        geometry.getCoordinates().addAll(Arrays.asList(positions));
        return geometry;
    }

    /**
     * Utility method to create {@link LineString} coordinates. Useful for {@link MultiLineString} creation.
     * @param positions positions representing the line string. At least 2 positions is required.
     * @return {@link Positions} (i.e a list of at least 2 {@link Position})
     */
    static Positions toLineStringCoordinates(Position... positions) {
        assertNotNull(positions, "Positions cannot be null.");
        assertMinLength(positions, 2, "At least two positions is required to make a line string!");

        Positions result = new Positions();
        result.addAll(Arrays.asList(positions));
        assertLineString(result, "Invalid line string!");
        return result;
    }

    /**
     * Create a new {@link LineString} geometry
     * @param lineString positions representing the line string. Use
     * {@link IGeometry#toLineStringCoordinates(Position...)}
     * to create line string coordinates.
     * @return {@link LineString}
     */
    static LineString lineString(Positions lineString) {
        assertNotNull(lineString, "Line string cannot be null.");
        LineString geometry = new LineString();
        geometry.setCoordinates(lineString);
        return geometry;
    }

    /**
     * Create a new {@link MultiLineString} geometry
     * @param lineStrings list of line strings. Use {@link IGeometry#toLineStringCoordinates(Position...)} to create
     * each
     * line string coordinates.
     * @return {@link MultiLineString}
     */
    static MultiLineString multiLineString(Positions... lineStrings) {
        assertNotNull(lineStrings, "Line string cannot be null.");
        assertMinLength(lineStrings, 2, "At least two line strings is required! Use lineString otherwise.");

        MultiLineString geometry = new MultiLineString();
        geometry.setCoordinates(Arrays.asList(lineStrings));
        return geometry;
    }

    /**
     * Utility method to create closed {@link LineString} coordinates also called <b>linear ring</b>. Useful for
     * {@link Polygon} coordinates creation.
     * @param positions positions representing the linear string. At least 4 positions is required. The first and
     * last
     * positions MUST be equivalent.
     * @return {@link Positions} (i.e a list of at least 4 {@link Position})
     */
    static Positions toLinearRingCoordinates(Position... positions) {
        assertNotNull(positions, "Positions for linear ring cannot be null.");
        assertMinLength(positions, 4, "At least four positions is required to make a linear ring!");

        Positions result = new Positions();
        result.addAll(Arrays.asList(positions));
        assertLinearRing(result,
                         "At least four positions is required to make a linear ring with first and latest positions equivalents.");
        return result;
    }

    /**
     * Utility method to create {@link Polygon} coordinates. Useful for {@link Polygon} creation.
     * @param exteriorRing counterclockwise exterior ring. Use {@link IGeometry#toLinearRingCoordinates(Position...)} to
     * create this ring.
     * @param holes clockwise interior rings. Use {@link IGeometry#toLinearRingCoordinates(Position...)} to create
     * holes.
     * @return {@link Polygon}
     */
    static PolygonPositions toPolygonCoordinates(Positions exteriorRing, Positions... holes) {

        // Manage exterior ring
        assertNotNull(exteriorRing, "At least exterior ring is required to make a polygon!");

        PolygonPositions result = new PolygonPositions();
        result.add(exteriorRing);

        // Manage holes
        if (holes != null) {
            result.addAll(Arrays.asList(holes));
        }

        return result;
    }

    /**
     * Create a new {@link Polygon} geometry
     * @param linearRings counterclockwise exterior ring + clockwise holes. Use
     * {@link IGeometry#toPolygonCoordinates(Positions, Positions...)} to
     * create the polygon coordinates
     * @return {@link Polygon}
     */
    static Polygon polygon(PolygonPositions linearRings) {
        Polygon geometry = new Polygon();
        geometry.setCoordinates(linearRings);
        return geometry;
    }

    /**
     * Create a new {@link MultiPolygon} geometry
     * @param polygons list of polygons coordinates
     * @return {@link MultiPolygon}
     */
    static MultiPolygon multiPolygon(PolygonPositions... polygons) {
        MultiPolygon geometry = new MultiPolygon();
        geometry.setCoordinates(Arrays.asList(polygons));
        return geometry;
    }

    /**
     * Create a new {@link GeometryCollection}
     * @param geometries list of geometries. Use other geometry construction method to fill this collection.
     * @return {@link GeometryCollection}
     */
    static GeometryCollection geometryCollection(AbstractGeometry<?>... geometries) {
        assertNotNull(geometries, "Geometries cannot be null");

        GeometryCollection geoCollection = new GeometryCollection();
        geoCollection.setGeometries(Arrays.asList(geometries));
        return geoCollection;
    }

    static void assertNotNull(Object object, String errorMessage) {
        if (object == null) {
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    static void assertMinLength(Object[] objects, int length, String errorMessage) {
        if (objects.length < length) {
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    static void assertLineString(Positions positions, String errorMessage) {
        if (!positions.isLineString()) {
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    static void assertLinearRing(Positions positions, String errorMessage) {
        if (!positions.isLinearRing()) {
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Create a new {@link Point} geometry.<br/>
     * As parameters are doubles instead of Doubles, int values can also be used.<br/>
     * Intent of this method is principaly to be used for tests
     */
    static Point point(double lon, double lat) {
        return point(position(lon, lat));
    }

    /**
     * Create a new {@link LineString} geometry.<br/>
     * As parameters are doubles instead of Doubles, int values can also be used.<br/>
     * Intent of this method is principaly to be used for tests     */
    static LineString lineString(double... lonLats) {
        Preconditions.checkNotNull(lonLats);
        Preconditions.checkArgument(lonLats.length > 2);
        Preconditions.checkArgument(lonLats.length % 2 == 0);

        double[][] array = new double[lonLats.length / 2][2];
        for (int i = 0; i < lonLats.length; i += 2) {
            array[i / 2][0] = lonLats[i];
            array[i / 2][1] = lonLats[i + 1];
        }
        return lineString(Positions.fromArray(array));
    }

    /**
     * Create a simple polygon (no hole, only exterior ring), specifying alternatively longitude and latitude of all
     * points. No need to close the polygon by specifying last two values as first twos.<br/>
     * As parameter is double[] instead of Double[], int values can also be used.<br/>
     * Intent of this method is principaly to be used for tests
     * @param lonLats point1 longitude, point2 latitude, point2 long, point2 latitude, ...
     */
    static Polygon simplePolygon(double... lonLats) {
        Preconditions.checkNotNull(lonLats);
        Preconditions.checkArgument(lonLats.length > 2);
        Preconditions.checkArgument(lonLats.length % 2 == 0);
        Preconditions.checkArgument(
                (lonLats[0] != lonLats[lonLats.length - 2]) || (lonLats[1] != lonLats[lonLats.length - 1]));

        Position[] positions = new Position[lonLats.length / 2 + 1];
        for (int i = 0; i < lonLats.length; i += 2) {
            positions[i / 2] = IGeometry.position(lonLats[i], lonLats[i + 1]);
        }
        positions[positions.length - 1] = position(lonLats[0], lonLats[1]);
        return polygon(toPolygonCoordinates(positions(positions)));
    }

    <T extends IGeometry> T withCrs(String crs);

    <T> T accept(IGeometryVisitor<T> visitor);
}
