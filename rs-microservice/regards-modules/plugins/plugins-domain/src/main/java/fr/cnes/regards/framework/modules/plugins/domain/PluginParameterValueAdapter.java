/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.domain;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;

/**
 * Informs GSON how to (de)serialize {@link PluginParameterValue}.
 * @author Marc Sordi
 */
@GsonTypeAdapter(adapted = PluginParameterValue.class)
public class PluginParameterValueAdapter extends TypeAdapter<PluginParameterValue> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterValueAdapter.class);

    /**
     * GSON default instance - Only standard types are recognized. The JSON engine is only used for managing and
     * checking JSON format.
     */
    private final Gson gson = new Gson();

    /**
     * Parse saved value into JSON to normalize output.
     */
    @Override
    public void write(JsonWriter out, PluginParameterValue value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            JsonElement el = gson.fromJson(value.getValue(), JsonElement.class);
            gson.toJson(el, out);
        }
    }

    /**
     * Handle parameter value checking JSON format and managing all JSON types. Parsed input is transformed in
     * {@link String} for persistence.
     */
    @Override
    public PluginParameterValue read(JsonReader in) throws IOException {
        PluginParameterValue result = null;
        JsonToken token = in.peek();
        switch (token) {
            case BEGIN_ARRAY:
            case BEGIN_OBJECT:
            case BOOLEAN:
            case NUMBER:
            case STRING:
                JsonElement object = gson.fromJson(in, JsonElement.class);
                result = PluginParameterValue.create(object.toString());
                break;
            case NULL:
                break;
            default:
                throw new JsonParseException("Unexpected token " + token + " for parameter value! " + in.toString());
        }
        return result;
    }
}
