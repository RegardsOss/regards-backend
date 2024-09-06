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
package fr.cnes.regards.framework.geojson.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;

import java.io.IOException;

/**
 * Jackson serializer for IGeometry properties.
 *
 * @author Thomas GUILLOU
 **/
public class SerializerIGeometry extends JsonSerializer<IGeometry> {

    private final JsonSerializer<Object> defaultSerializer;

    public SerializerIGeometry(JsonSerializer<Object> serializer) {
        defaultSerializer = serializer;
    }

    @Override
    public void serialize(IGeometry geometry, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
        throws IOException {
        if (geometry.getType() == GeoJsonType.UNLOCATED) {
            jsonGenerator.writeObject(null);
        } else {
            defaultSerializer.serialize(geometry, jsonGenerator, serializerProvider);
        }
    }
}
