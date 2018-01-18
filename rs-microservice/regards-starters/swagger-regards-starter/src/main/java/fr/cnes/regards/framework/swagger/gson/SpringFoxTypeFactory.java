/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.swagger.gson;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import springfox.documentation.spring.web.json.Json;

/**
 * SpringFox factory
 *
 * @author Marc Sordi
 *
 */
public class SpringFoxTypeFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson pGson, TypeToken<T> pType) {

        if (pType.getRawType() != Json.class) {
            return null;
        }

        return new TypeAdapter<T>() {

            @Override
            public void write(JsonWriter pOut, T pValue) throws IOException {
                pOut.jsonValue(((Json) pValue).value());
            }

            @Override
            public T read(JsonReader pIn) throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }

}
