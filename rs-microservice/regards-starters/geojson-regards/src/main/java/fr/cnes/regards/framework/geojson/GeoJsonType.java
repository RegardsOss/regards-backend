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
package fr.cnes.regards.framework.geojson;

/**
 * RFC 7946 -August 2016<br/>
 * All availble GeoJson types
 *
 * @author Marc Sordi
 *
 */
public enum GeoJsonType {

    FEATURE("Feature"),
    FEATURE_COLLECTION("FeatureCollection"),
    // Geometry
    POINT("Point"),
    MULTIPOINT("MultiPoint"),
    LINESTRING("LineString"),
    MULTILINESTRING("MultiLineString"),
    POLYGON("Polygon"),
    MULTIPOLYGON("MultiPolygon"),
    GEOMETRY_COLLECTION("GeometryCollection"),
    // Custom unlocated
    UNLOCATED("Unlocated");

    private String type;

    private GeoJsonType(String name) {
        this.type = name;
    }

    public String getType() {
        return type;
    }
}
