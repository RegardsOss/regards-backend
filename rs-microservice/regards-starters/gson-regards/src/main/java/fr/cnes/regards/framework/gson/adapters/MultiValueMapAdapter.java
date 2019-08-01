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
package fr.cnes.regards.framework.gson.adapters;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * {@link MultiValueMap} Gson adapter
 * @author Sébastien Binda
 */
public class MultiValueMapAdapter
        implements JsonDeserializer<MultiValueMap<Object, Object>>, JsonSerializer<MultiValueMap<Object, Object>> {

    @Override
    public MultiValueMap<Object, Object> deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        final MultiValueMap<Object, Object> result = new LinkedMultiValueMap<>();
        final Map<Object, List<Object>> map = context.deserialize(json, multimapTypeToMapType(type));
        result.putAll(map);
        return result;
    }

    @Override
    public JsonElement serialize(MultiValueMap<Object, Object> src, Type type, JsonSerializationContext context) {
        final Map<Object, List<Object>> map = new HashMap<>();
        src.forEach((key, value) -> map.put(key, value));
        return context.serialize(map);
    }

    private <KK, V> Type multimapTypeToMapType(Type type) {
        final Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
        assert typeArguments.length == 2;
        @SuppressWarnings({ "unchecked", "serial" })
        final TypeToken<Map<KK, Collection<V>>> mapTypeToken = new TypeToken<Map<KK, Collection<V>>>() {

        }.where(new TypeParameter<KK>() {

        }, (TypeToken<KK>) TypeToken.of(typeArguments[0])).where(new TypeParameter<V>() {

        }, (TypeToken<V>) TypeToken.of(typeArguments[1]));
        return mapTypeToken.getType();
    }

}
