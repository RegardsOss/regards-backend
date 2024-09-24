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
package fr.cnes.regards.framework.geojson.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.geometry.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom geometry deserializer for Jackson.
 *
 * @author Thomas GUILLOU
 */
public class GenericGeometryDeserializer<T extends IGeometry> extends JsonDeserializer<T> {

    protected final Map<String, Class<?>> jsonPropertyNameToClass;

    public GenericGeometryDeserializer() {
        jsonPropertyNameToClass = new HashMap<>() {

            {
                put(GeoJsonType.POINT, Point.class);
                put(GeoJsonType.MULTIPOINT, MultiPoint.class);
                put(GeoJsonType.LINESTRING, LineString.class);
                put(GeoJsonType.MULTILINESTRING, MultiLineString.class);
                put(GeoJsonType.POLYGON, Polygon.class);
                put(GeoJsonType.MULTIPOLYGON, MultiPolygon.class);
                put(GeoJsonType.GEOMETRY_COLLECTION, GeometryCollection.class);
            }
        };
    }

    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        final JsonNode node = jsonParser.readValueAsTree();
        final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        return (T) mapper.treeToValue(node, jsonPropertyNameToClass.get(node.get("type").textValue()));
    }
}
