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
package fr.cnes.regards.framework.geojson.gson;

import com.google.gson.JsonElement;
import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.geometry.GeometryCollection;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.LineString;
import fr.cnes.regards.framework.geojson.geometry.MultiLineString;
import fr.cnes.regards.framework.geojson.geometry.MultiPoint;
import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.geojson.geometry.Point;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.framework.geojson.geometry.Unlocated;
import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;

/**
 * Gson adapter for GeoJson geometry
 * @author Marc Sordi
 */
@GsonTypeAdapterFactory
public class GeometryTypeAdapterFactory extends PolymorphicTypeAdapterFactory<IGeometry> {

    public GeometryTypeAdapterFactory() {
        super(IGeometry.class, "type");
        registerSubtype(Point.class, GeoJsonType.POINT.getType());
        registerSubtype(MultiPoint.class, GeoJsonType.MULTIPOINT.getType());
        registerSubtype(LineString.class, GeoJsonType.LINESTRING.getType());
        registerSubtype(MultiLineString.class, GeoJsonType.MULTILINESTRING.getType());
        registerSubtype(Polygon.class, GeoJsonType.POLYGON.getType());
        registerSubtype(MultiPolygon.class, GeoJsonType.MULTIPOLYGON.getType());
        registerSubtype(GeometryCollection.class, GeoJsonType.GEOMETRY_COLLECTION.getType());
        registerSubtype(Unlocated.class, GeoJsonType.UNLOCATED.getType(), true); // Serialize nulls
    }

    /**
     * For unlocated feature, just return null.
     */
    @Override
    protected JsonElement beforeWrite(JsonElement jsonElement, Class<?> subType) {
        if (subType == Unlocated.class) {
            return null;
        } else {
            return super.beforeWrite(jsonElement, subType);
        }
    }
}
