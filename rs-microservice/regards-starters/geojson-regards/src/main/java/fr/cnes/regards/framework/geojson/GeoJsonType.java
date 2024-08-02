/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * =at your option) any later version.
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
 * @author Stephane Cortine
 */
// constant class in order to serialize/deserialize easy with GSON or JACKSON framework
public final class GeoJsonType {

    public static final String FEATURE = "Feature";

    public static final String FEATURE_COLLECTION = "FeatureCollection";

    // Geometry
    public static final String POINT = "Point";

    public static final String MULTIPOINT = "MultiPoint";

    public static final String LINESTRING = "LineString";

    public static final String MULTILINESTRING = "MultiLineString";

    public static final String POLYGON = "Polygon";

    public static final String MULTIPOLYGON = "MultiPolygon";

    public static final String GEOMETRY_COLLECTION = "GeometryCollection";

    // Custom unlocated
    public static final String UNLOCATED = "Unlocated";

    private GeoJsonType() {
    }

}
