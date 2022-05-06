/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.gson.adapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to serialize/deserialize list based on project apache jclouds (json/internal/NullFilteringTypeAdapterFactories.java)
 *
 * @author Iliana Ghazali
 **/
public class ListAdapter<E> extends TypeAdapter<List<E>> {


    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<T> rawType = (Class<T>) type.getRawType();
            if (rawType != List.class || !(type instanceof ParameterizedType)) {
                return null;
            }
            final ParameterizedType parameterizedType = (ParameterizedType) type.getType();
            final Type actualType = parameterizedType.getActualTypeArguments()[0];
            final TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(actualType));
            return new ListAdapter(adapter);
        }
    };

    private final TypeAdapter<E> listAdapter;

    public ListAdapter(TypeAdapter<E> listAdapter) {
        this.listAdapter = listAdapter;
    }

    @Override
    public List<E> read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        List<E> ls = new ArrayList<>();
        in.beginArray();
        while (in.hasNext()) {
            E value = listAdapter.read(in);
            if (value != null) {
                ls.add(value);
            }
        }
        in.endArray();
        return ls;
    }

    @Override
    public void write(JsonWriter out, List<E> list) throws IOException {
        if (list == null) {
            out.nullValue();
            return;
        }
        out.beginArray();
        for (E e : list) {
            listAdapter.write(out, e);
        }
        out.endArray();
    }
}

