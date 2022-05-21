/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.utils.gson;

import com.google.auto.service.AutoService;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import io.vavr.collection.List;
import io.vavr.collection.Map;

/**
 * This class is a type adapter for {@link ProcessesByDatasetsDTO}.
 *
 * @author gandrieu
 */
@AutoService(TypedGsonTypeAdapter.class)
public class ProcessesByDatasetsDTOTypeAdapter implements TypedGsonTypeAdapter<ProcessesByDatasetsDTO> {

    @Override
    public Class<ProcessesByDatasetsDTO> type() {
        return ProcessesByDatasetsDTO.class;
    }

    @Override
    public JsonDeserializer<ProcessesByDatasetsDTO> deserializer() {
        return (json, typeOfT, context) -> {
            Map<String, List<ProcessLabelDTO>> map = context.deserialize(json,
                                                                         new TypeToken<Map<String, List<ProcessLabelDTO>>>() {

                                                                         }.getType());
            return new ProcessesByDatasetsDTO(map);
        };
    }

    @Override
    public JsonSerializer<ProcessesByDatasetsDTO> serializer() {
        return (src, typeOfSrc, context) -> context.serialize(src.getMap());
    }
}
