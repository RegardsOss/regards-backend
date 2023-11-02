/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.modules.dam.domain.entities.metadata.DataObjectGroup;
import fr.cnes.regards.modules.dam.domain.entities.metadata.DatasetMetadata;

import java.io.IOException;

/**
 * Jsoniter decoder to deserialize {@link DatasetMetadata} object from elastic search response at json format.
 *
 * @author Sébastien Binda
 **/
public class DatasetMetadataJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new DatasetMetadataJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(DatasetMetadata.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any metadata = iter.readAny();
        DatasetMetadata datasetMetadata = new DatasetMetadata();
        metadata.get("dataObjectsGroups").asMap().forEach((group, dataObjectGroup) -> {
            datasetMetadata.addDataObjectGroup(asOrNull(dataObjectGroup, DataObjectGroup.class));
        });
        return datasetMetadata;
    }

}
