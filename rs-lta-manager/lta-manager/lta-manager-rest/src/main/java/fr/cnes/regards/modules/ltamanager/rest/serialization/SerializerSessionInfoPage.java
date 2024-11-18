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
package fr.cnes.regards.modules.ltamanager.rest.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import fr.cnes.regards.framework.geojson.serializers.SerializerPagedModel;
import fr.cnes.regards.modules.ltamanager.dto.submission.session.SessionInfoPageDto;
import org.springframework.hateoas.PagedModel;

import java.io.IOException;

/**
 * Custom Jackson serializer for {@link SessionInfoPageDto} which is a subclass of {@link PagedModel}
 * This template will produce a json object with the following format :
 * <pre>
 * {
 *     "globalStatus": "RUNNING|ERROR|DONE",
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
 *         }
 *     ]
 * }
 * </pre>
 *
 * @author Thibaud Michaudel
 */
public class SerializerSessionInfoPage<T> extends JsonSerializer<SessionInfoPageDto<T>> {

    @Override
    public void serialize(SessionInfoPageDto<T> value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        gen.writeStartObject();

        // Write the global status at the same level at the hateoas blocks
        gen.writePOJOField("globalStatus", value.getGlobalStatus());

        SerializerPagedModel.writeMetadataBlock(value, gen);

        SerializerPagedModel.writeContentBlock(value, gen);

        SerializerPagedModel.writeLinksBlock(value, gen);

        gen.writeEndObject();
    }
}
