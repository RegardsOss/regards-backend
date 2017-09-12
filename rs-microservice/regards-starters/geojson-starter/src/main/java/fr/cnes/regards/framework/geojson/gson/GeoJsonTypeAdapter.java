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
package fr.cnes.regards.framework.geojson.gson;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;

/**
 * Gson adapter for {@link GeoJsonType}
 *
 * @author Marc Sordi
 *
 */
@GsonTypeAdapter(adapted = GeoJsonType.class)
public class GeoJsonTypeAdapter extends TypeAdapter<GeoJsonType> {

    @Override
    public void write(JsonWriter out, GeoJsonType value) throws IOException {
        out.value(value.getType());
    }

    @Override
    public GeoJsonType read(JsonReader in) throws IOException {
        String type = in.nextString();
        for (GeoJsonType tmp : GeoJsonType.values()) {
            if (tmp.getType().equals(type)) {
                return tmp;
            }
        }
        throw new IllegalArgumentException("Unknown GeoJson type " + type);
    }
}
