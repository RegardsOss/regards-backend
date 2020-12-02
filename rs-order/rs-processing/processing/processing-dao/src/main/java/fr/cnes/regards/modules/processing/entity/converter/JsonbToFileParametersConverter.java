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

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.entity.FileParameters;
import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * This class define a Gson converter for FileParameters
 *
 * @author gandrieu
 */
@ReadingConverter
@AllArgsConstructor
public class JsonbToFileParametersConverter implements Converter<Json, FileParameters> {

    @Autowired private Gson gson;

    @Override public FileParameters convert(Json source) {
        return gson.fromJson(source.asString(), FileParameters.class);
    }
}
