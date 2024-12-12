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
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.io.IOException;

/**
 * Serializer to make the jackson rest entity responses follow the gson response pattern so there is a unique response
 * template for all regards rest controllers whether they use jackson or gson to serialize their messages.
 * This template will produce a json object with the following format :
 * <pre>
 * {
 *     "content": {
 *
 *     },
 *     "links": [
 *         {
 *             "rel": "self",
 *             "href": "..."
 *         },
 *         {
 *             "rel": "...",
 *             "href": "..."
 *         }
 *     ]
 * }
 * </pre>
 *
 * @author Thibaud Michaudel
 */
public class SerializerEntityModel<T> extends JsonSerializer<EntityModel<T>> {

    @Override
    public void serialize(EntityModel<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        writeContentBlock(value, gen);
        writeLinksBlock(value, gen);

        gen.writeEndObject();
    }

    private void writeContentBlock(EntityModel<T> value, JsonGenerator gen) throws IOException {
        gen.writePOJOField("content", value.getContent());
    }

    private static <T> void writeLinksBlock(EntityModel<T> value, JsonGenerator gen) throws IOException {
        // Write links array
        gen.writeArrayFieldStart("links");
        for (Link link : value.getLinks()) {
            gen.writeObject(link);
        }
        // End links array
        gen.writeEndArray();
    }
}
