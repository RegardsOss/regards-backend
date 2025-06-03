/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.geojson.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import fr.cnes.regards.framework.utils.json.JsonSerializationUtils;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Jackson serializer for OffsetDateTime properties.
 * Format to UTC ISO 8601.
 *
 * @author tguillou
 * @see JsonSerializationUtils#format(OffsetDateTime)
 */
public class SerializerOffsetDateTime extends JsonSerializer<OffsetDateTime> {

    @Override
    public void serialize(OffsetDateTime offsetDateTime,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        if (offsetDateTime == null) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(JsonSerializationUtils.format(offsetDateTime));
        }
    }
}
