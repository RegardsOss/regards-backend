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
package fr.cnes.regards.framework.geojson;

import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;

/**
 * @author Stephane Cortine
 **/
public final class GeometryFactory {

    private GeometryFactory() {
    }

    public static IGeometry createPoint() {
        Double longitude = -10.0;
        Double latitude = 20.0;

        return IGeometry.point(IGeometry.position(longitude, latitude));
    }

    public static IGeometry createMultiPoint() {
        return IGeometry.multiPoint(IGeometry.position(-170.0, 25.25), IGeometry.position(70.0, 10.10));
    }

    public static IGeometry createLineString() {
        Positions lineStringCoordinates = IGeometry.toLineStringCoordinates(IGeometry.position(-170.0, 25.25),
                                                                            IGeometry.position(70.0, 10.10));

        return IGeometry.lineString(lineStringCoordinates);
    }

    public static IGeometry createMultiLineString() {
        Positions lineStringCoordinates1 = IGeometry.toLineStringCoordinates(IGeometry.position(-170.0, 25.25),
                                                                             IGeometry.position(70.0, 10.10));

        Positions lineStringCoordinates2 = IGeometry.toLineStringCoordinates(IGeometry.position(-90.0, 90.0),
                                                                             IGeometry.position(33.33, 77.77));

        return IGeometry.multiLineString(lineStringCoordinates1, lineStringCoordinates2);
    }

    public static IGeometry createPolygon() {
        Positions exteriorRing = IGeometry.toLinearRingCoordinates(IGeometry.position(-170.0, 20.0),
                                                                   IGeometry.position(-170.0, 10.0),
                                                                   IGeometry.position(-140.0, 10.0),
                                                                   IGeometry.position(-140.0, 20.0),
                                                                   IGeometry.position(-170.0, 20.0));

        Positions hole = IGeometry.toLinearRingCoordinates(IGeometry.position(-160.0, 17.0),
                                                           IGeometry.position(-150.0, 17.0),
                                                           IGeometry.position(-150.0, 13.0),
                                                           IGeometry.position(-160.0, 13.0),
                                                           IGeometry.position(-160.0, 17.0));

        return IGeometry.polygon(IGeometry.toPolygonCoordinates(exteriorRing, hole));

    }

    public static IGeometry createMultiPolygon() {
        Positions exteriorRing = IGeometry.toLinearRingCoordinates(IGeometry.position(-170.0, 20.0),
                                                                   IGeometry.position(-170.0, 10.0),
                                                                   IGeometry.position(-140.0, 10.0),
                                                                   IGeometry.position(-140.0, 20.0),
                                                                   IGeometry.position(-170.0, 20.0));

        Positions hole = IGeometry.toLinearRingCoordinates(IGeometry.position(-160.0, 17.0),
                                                           IGeometry.position(-150.0, 17.0),
                                                           IGeometry.position(-150.0, 13.0),
                                                           IGeometry.position(-160.0, 13.0),
                                                           IGeometry.position(-160.0, 17.0));

        return IGeometry.multiPolygon(IGeometry.toPolygonCoordinates(exteriorRing, hole),
                                      IGeometry.toPolygonCoordinates(exteriorRing, hole));
    }

    public static IGeometry createGeometryCollection() {
        return IGeometry.geometryCollection(IGeometry.point(IGeometry.position(0.0, 10.0)),
                                            IGeometry.lineString(IGeometry.toLineStringCoordinates(IGeometry.position(
                                                10.0,
                                                13.0), IGeometry.position(25.0, 39.0))));
    }
}
