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

import com.google.gson.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Adapter to serialize/deserialize java optional
 *
 * @author Iliana Ghazali
 **/
public class OptionalAdapter<T> implements JsonSerializer<Optional<T>>, JsonDeserializer<Optional<T>> {

    @Override
    public Optional<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        final T value = context.deserialize(json, ((ParameterizedType) typeOfT).getActualTypeArguments()[0]);
        return Optional.ofNullable(value);
    }

    @Override
    public JsonElement serialize(Optional<T> src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.orElse(null));
    }
}