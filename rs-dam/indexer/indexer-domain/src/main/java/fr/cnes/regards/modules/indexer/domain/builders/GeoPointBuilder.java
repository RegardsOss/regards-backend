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
package fr.cnes.regards.modules.indexer.domain.builders;

import org.elasticsearch.common.geo.GeoPoint;

/**
 * Builder used to ensure {@link GeoPoint} will be created with -90 < lat < 90 and -180 < lon < 180
 *
 * @author Thibaud Michaudel
 **/
public class GeoPointBuilder {

    private double lat;

    private double lon;

    public GeoPointBuilder(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public GeoPoint build() {
        double normalizedLat = lat % 180;
        if (normalizedLat < -90) {
            normalizedLat += 180;
        } else if (normalizedLat > 90) {
            normalizedLat -= 180;
        }
        double normalizedLon = lon % 360;
        if (normalizedLon < -180) {
            normalizedLon += 360;
        } else if (normalizedLon > 180) {
            normalizedLon -= 360;
        }
        return new GeoPoint(normalizedLat, normalizedLon);
    }

}
