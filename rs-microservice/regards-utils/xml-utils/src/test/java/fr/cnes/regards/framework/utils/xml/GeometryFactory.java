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
package fr.cnes.regards.framework.utils.xml;

import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;

/**
 * @author Stephane Cortine
 **/
public final class GeometryFactory {

    private GeometryFactory() {
    }

    public static IGeometry createPoint() {
        return IGeometry.point(IGeometry.position(-77.0, 88.0));
    }

    public static IGeometry createLineString() {
        Positions lineStringCoordinates = IGeometry.toLineStringCoordinates(IGeometry.position(-100.0, -70.0),
                                                                            IGeometry.position(-88.0, -66.0),
                                                                            IGeometry.position(-11.0, -10.0));

        return IGeometry.lineString(lineStringCoordinates);
    }

    public static IGeometry createPolygon() {
        Positions exteriorRing = IGeometry.toLinearRingCoordinates(IGeometry.position(48.1367666796927,
                                                                                      2.219238281249998),
                                                                   IGeometry.position(47.60616304386872,
                                                                                      8.063964843750005),
                                                                   IGeometry.position(46.07323062540834, 9.51416015625),
                                                                   IGeometry.position(43.9295499356146,
                                                                                      10.349121093750002),
                                                                   IGeometry.position(42.09822241118974,
                                                                                      9.689941406250002),
                                                                   IGeometry.position(40.245991504199026,
                                                                                      6.481933593749999),
                                                                   IGeometry.position(39.707186656826565,
                                                                                      1.0327148437500053),
                                                                   IGeometry.position(41.77131167976407,
                                                                                      -4.59228515625),
                                                                   IGeometry.position(45.08903556483102,
                                                                                      -5.383300781249999),
                                                                   IGeometry.position(47.546871598922365,
                                                                                      -3.8891601562499982),
                                                                   IGeometry.position(49.2104204456503,
                                                                                      -2.3950195312499973),
                                                                   IGeometry.position(51.261914853084505,
                                                                                      -0.0659179687499982),
                                                                   IGeometry.position(48.1367666796927,
                                                                                      2.219238281249998));

        Positions hole0 = IGeometry.toLinearRingCoordinates(IGeometry.position(47.39834920035926, -1.6918945312499998),
                                                            IGeometry.position(45.49094569262732, -3.1860351562499964),
                                                            IGeometry.position(44.68427737181224, -2.3950195312499973),
                                                            IGeometry.position(44.71551373202132, -1.1645507812499976),
                                                            IGeometry.position(46.13417004624324, -0.41748046874999695),
                                                            IGeometry.position(46.34692761055675, -1.4282226562499984),
                                                            IGeometry.position(47.39834920035926, -1.6918945312499998));

        Positions hole1 = IGeometry.toLinearRingCoordinates(IGeometry.position(44.18220395771567, 0.8129882812499987),
                                                            IGeometry.position(42.261049162113835, 0.5493164062499976),
                                                            IGeometry.position(41.86956082699456, 1.9995117187499996),
                                                            IGeometry.position(42.42345651793829, 3.7573242187500018),
                                                            IGeometry.position(43.54854811091286, 3.6254882812499973),
                                                            IGeometry.position(44.18220395771567, 0.8129882812499987));

        return IGeometry.polygon(IGeometry.toPolygonCoordinates(exteriorRing, hole0, hole1));
    }

    public static IGeometry createPolygon_with_sampling_10() {
        Positions exteriorRing = IGeometry.toLinearRingCoordinates(IGeometry.position(48.1367666796927,
                                                                                      2.219238281249998),
                                                                   IGeometry.position(47.60616304386872,
                                                                                      8.063964843750005),
                                                                   IGeometry.position(46.07323062540834, 9.51416015625),
                                                                   IGeometry.position(43.9295499356146,
                                                                                      10.349121093750002),
                                                                   IGeometry.position(42.09822241118974,
                                                                                      9.689941406250002),
                                                                   IGeometry.position(40.245991504199026,
                                                                                      6.481933593749999),
                                                                   IGeometry.position(41.77131167976407,
                                                                                      -4.59228515625),
                                                                   IGeometry.position(47.546871598922365,
                                                                                      -3.8891601562499982),
                                                                   IGeometry.position(51.261914853084505,
                                                                                      -0.0659179687499982),
                                                                   IGeometry.position(48.1367666796927,
                                                                                      2.219238281249998));

        Positions hole0 = IGeometry.toLinearRingCoordinates(IGeometry.position(47.39834920035926, -1.6918945312499998),
                                                            IGeometry.position(45.49094569262732, -3.1860351562499964),
                                                            IGeometry.position(44.68427737181224, -2.3950195312499973),
                                                            IGeometry.position(44.71551373202132, -1.1645507812499976),
                                                            IGeometry.position(46.13417004624324, -0.41748046874999695),
                                                            IGeometry.position(46.34692761055675, -1.4282226562499984),
                                                            IGeometry.position(47.39834920035926, -1.6918945312499998));

        Positions hole1 = IGeometry.toLinearRingCoordinates(IGeometry.position(44.18220395771567, 0.8129882812499987),
                                                            IGeometry.position(42.261049162113835, 0.5493164062499976),
                                                            IGeometry.position(41.86956082699456, 1.9995117187499996),
                                                            IGeometry.position(42.42345651793829, 3.7573242187500018),
                                                            IGeometry.position(43.54854811091286, 3.6254882812499973),
                                                            IGeometry.position(44.18220395771567, 0.8129882812499987));

        return IGeometry.polygon(IGeometry.toPolygonCoordinates(exteriorRing, hole0, hole1));
    }

}
