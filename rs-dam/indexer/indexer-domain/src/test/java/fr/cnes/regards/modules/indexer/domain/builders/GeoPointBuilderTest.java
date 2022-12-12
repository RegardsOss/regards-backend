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
package fr.cnes.regards.modules.indexer.domain.builders;

import org.elasticsearch.common.geo.GeoPoint;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

/**
 * @author Thibaud Michaudel
 **/
public class GeoPointBuilderTest {

    @Test
    public void testGeoPointBuilder() {
        //No change
        GeoPoint geoPoint1 = new GeoPointBuilder(23.5, 24.8).build();
        GeoPoint expected1 = new GeoPoint(23.5, 24.8);
        Assertions.assertEquals(expected1.getLat(), geoPoint1.getLat(), 0.001);
        Assertions.assertEquals(expected1.getLon(), geoPoint1.getLon(), 0.001);

        //Latitude too high
        GeoPoint geoPoint2 = new GeoPointBuilder(340.6, 24.8).build();
        GeoPoint expected2 = new GeoPoint(-19.4, 24.8);
        Assertions.assertEquals(expected2.getLat(), geoPoint2.getLat(), 0.001);

        //No change with negative longitude
        GeoPoint geoPoint3 = new GeoPointBuilder(23.5, -50.8).build();
        GeoPoint expected3 = new GeoPoint(23.5, -50.8);
        Assertions.assertEquals(expected3.getLon(), geoPoint3.getLon(), 0.001);

        //Longitude too low
        GeoPoint geoPoint4 = new GeoPointBuilder(23.5, -210.8).build();
        GeoPoint expected4 = new GeoPoint(23.5, 149.2);
        Assertions.assertEquals(expected4.getLon(), geoPoint4.getLon(), 0.001);

        //Longitude far too low
        GeoPoint geoPoint5 = new GeoPointBuilder(23.5, -570.8).build();
        GeoPoint expected5 = new GeoPoint(23.5, 149.2);
        Assertions.assertEquals(expected5.getLon(), geoPoint5.getLon(), 0.001);
    }
}
