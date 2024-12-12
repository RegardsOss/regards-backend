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
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;

import java.io.IOException;

/**
 * Serializer to make the jackson rest paged responses follow the gson response pattern so there is a unique response
 * template for all regards rest controllers whether they use jackson or gson to serialize their messages.
 * This template will produce a json object with the following format :
 * <pre>
 * {
 *     "metadata": {
 *         "size": 20,
 *         "totalElements": 1,
 *         "totalPages": 1,
 *         "number": 0
 *     },
 *     "content": [
 *         {
 *             "content": {
 *                 ...
 *             },
 *             "content": {
 *                 ...
 *             }
 *         }
 *     ],
 *     "links": [
 *         {
 *             "rel": "self",
 *             "href": "..."
 *         },
 *         {
 *             "rel": "...",
 *             "href": "..."
 *         }
 * 
 *     ]
 * }
 * </pre>
 *
 * @author Thibaud Michaudel
 */
public class SerializerPagedModel<T> extends JsonSerializer<PagedModel<T>> {

    @Override
    public void serialize(PagedModel<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        writeMetadataBlock(value, gen);

        writeContentBlock(value, gen);

        writeLinksBlock(value, gen);

        gen.writeEndObject();
    }

    public static <T> void writeLinksBlock(PagedModel<T> value, JsonGenerator gen) throws IOException {
        // Write links array
        gen.writeArrayFieldStart("links");
        for (Link link : value.getLinks()) {
            gen.writeObject(link);
        }
        // End links array
        gen.writeEndArray();
    }

    public static <T> void writeContentBlock(PagedModel<T> value, JsonGenerator gen) throws IOException {
        // Start content array
        gen.writeArrayFieldStart("content");

        // Write nested content blocks
        for (T contentItem : value.getContent()) {
            gen.writeObject(contentItem);
        }

        // End the content array
        gen.writeEndArray();
    }

    public static <T> void writeMetadataBlock(PagedModel<T> value, JsonGenerator gen) throws IOException {
        // Start metadata block
        gen.writeObjectFieldStart("metadata");
        PagedModel.PageMetadata metadata = value.getMetadata();
        if (metadata != null) {
            gen.writeNumberField("size", metadata.getSize());
            gen.writeNumberField("totalElements", metadata.getTotalElements());
            gen.writeNumberField("totalPages", metadata.getTotalPages());
            gen.writeNumberField("number", metadata.getNumber());
        }

        // End metadata block
        gen.writeEndObject();
    }
}
