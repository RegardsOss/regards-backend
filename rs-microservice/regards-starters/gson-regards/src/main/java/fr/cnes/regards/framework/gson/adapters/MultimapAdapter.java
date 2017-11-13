package fr.cnes.regards.framework.gson.adapters;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Multimap Gson adapter
 * @author oroussel
 */
public class MultimapAdapter implements JsonDeserializer<Multimap<?, ?>>, JsonSerializer<Multimap<?, ?>> {

    @Override
    public Multimap<?, ?> deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        final HashMultimap<Object, Object> result = HashMultimap.create();
        final Map<?, Collection<?>> map = context.deserialize(json, multimapTypeToMapType(type));
        for (final Map.Entry<?, ?> e : map.entrySet()) {
            final Collection<?> value = (Collection<?>) e.getValue();
            result.putAll(e.getKey(), value);
        }
        return result;
    }

    @Override
    public JsonElement serialize(Multimap<?, ?> src, Type type, JsonSerializationContext context) {
        final Map<?, ?> map = src.asMap();
        return context.serialize(map);
    }

    private <KK,V> Type multimapTypeToMapType(Type type) {
        final Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
        assert typeArguments.length == 2;
        @SuppressWarnings("unchecked")
        final TypeToken<Map<KK, Collection<V>>> mapTypeToken = new TypeToken<Map<KK, Collection<V>>>() {

        }.where(new TypeParameter<KK>() {

        }, (TypeToken<KK>) TypeToken.of(typeArguments[0])).where(new TypeParameter<V>() {

        }, (TypeToken<V>) TypeToken.of(typeArguments[1]));
        return mapTypeToken.getType();
    }

}
