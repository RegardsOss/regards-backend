/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.entity.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.google.gson.Gson;

import fr.cnes.regards.modules.processing.entity.FileStatsByDataset;
import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
@ReadingConverter
@AllArgsConstructor
public class JsonbToFileStatsByDatasetConverter implements Converter<Json, FileStatsByDataset> {

    @Autowired
    private final Gson gson;

    @Override
    public FileStatsByDataset convert(Json source) {
        return gson.fromJson(source.asString(), FileStatsByDataset.class);
    }
}
