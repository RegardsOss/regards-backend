/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.entity.converters;

import java.io.IOException;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;
import fr.cnes.regards.modules.ingest.domain.entity.AipStateManager;
import fr.cnes.regards.modules.storage.domain.IAipState;

/**
 * Adapter for extended AIP state
 *
 * @author Marc Sordi
 *
 */
@GsonTypeAdapter(adapted = IAipState.class)
public class AipStateAdapter extends TypeAdapter<IAipState> {

    @Override
    public void write(JsonWriter out, IAipState value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.getName());
        }
    }

    @Override
    public IAipState read(JsonReader in) throws IOException {
        IAipState state = null;
        JsonToken token = in.peek();
        if (JsonToken.STRING.equals(token)) {
            state = AipStateManager.fromName(in.nextString());
        } else if (JsonToken.NULL.equals(token)) {
            // Nothing to do
        } else {
            throw new JsonParseException("Unexpected value for product SIP state");
        }
        return state;
    }
}
