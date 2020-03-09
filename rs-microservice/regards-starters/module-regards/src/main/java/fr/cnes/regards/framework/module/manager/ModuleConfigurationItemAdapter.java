/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.module.manager;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Automatic (de)serializer for configuration POJO. Only non generic POJOs are accepted!
 * @author Marc Sordi
 */
@SuppressWarnings("rawtypes")
public class ModuleConfigurationItemAdapter extends TypeAdapter<ModuleConfigurationItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleConfigurationItemAdapter.class);

    private Gson gson;

    /**
     * @param gsonWithoutMyself all GSON adapters and factories but the present adapter! Else stackoverflow will occurs!
     */
    public ModuleConfigurationItemAdapter(Gson gsonWithoutMyself) {
        gson = gsonWithoutMyself;
    }

    @Override
    public void write(JsonWriter out, ModuleConfigurationItem value) {
        gson.toJson(value, ModuleConfigurationItem.class, out);
    }

    @Override
    public ModuleConfigurationItem read(JsonReader in) throws IOException {
        JsonElement jsonElement = Streams.parse(in);

        JsonElement keyEl = jsonElement.getAsJsonObject().get("key");
        String key = keyEl.getAsString();

        try {
            Class<?> typeClass = Class.forName(key);
            TypeToken<?> typeToken = TypeToken.getParameterized(ModuleConfigurationItem.class, typeClass);
            return gson.fromJson(jsonElement, typeToken.getType());
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class \"{}\" not found during deserialization", key);
            throw new IOException(e);
        }
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }
}
