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
package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.ValueType;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Jsoniter decoder for {@link DataFile}.
 * Registers itself via {@link #selfRegister()}.
 */
public class DataFileJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new DataFileJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(DataFile.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any dataFile = iter.readAny();
        DataFile result = DataFile.build(DataType.valueOf(dataFile.toString("dataType")),
                                         stringOrNull(dataFile, "filename"),
                                         stringOrNull(dataFile, "uri"),
                                         dataFile.get("mimeType").as(MimeType.class),
                                         dataFile.toBoolean("online"),
                                         dataFile.toBoolean("reference"));
        result.setFilesize(longOrNull(dataFile, "filesize"));
        result.setChecksum(stringOrNull(dataFile, "checksum"));
        result.setDigestAlgorithm(stringOrNull(dataFile, "digestAlgorithm"));
        result.setImageHeight(doubleOrNull(dataFile, "imageHeight"));
        result.setImageWidth(doubleOrNull(dataFile, "imageWidth"));
        result.setAdditionalFields(readAdditionalFields(dataFile));
        result.setCrc32(stringOrNull(dataFile, "crc32"));
        result.setTypes(readTypes(dataFile));
        return result;
    }

    /*
     * Reads additional fields from the data file. Additional fields can be of any type (json array or json object), so we return it as an Object.
     */
    private Object readAdditionalFields(Any dataFile) {
        Any addFields = dataFile.get("additionalFields");
        if (addFields == null
            || addFields.valueType() == ValueType.NULL
            || addFields.valueType() == ValueType.INVALID) {
            return null;
        } else {
            return addFields.as(Object.class);
        }
    }

    private Set<String> readTypes(Any parent) {
        Any typesAny = parent.get("types");
        if (typesAny == null || typesAny.valueType() == ValueType.NULL || typesAny.valueType() == ValueType.INVALID) {
            return Collections.emptySet();
        }
        Set<String> types = new LinkedHashSet<>();
        if (typesAny.valueType() == ValueType.ARRAY) {
            for (Any item : typesAny.asList()) {
                if (item != null && item.valueType() != ValueType.NULL) {
                    types.add(item.toString());
                }
            }
        }
        return types;
    }
}
