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
// CHECKSTYLE:OFF
/**
 * Code inspired from
 * https://github.com/google/gson/blob/master/extras/src/main/java/com/google/gson/typeadapters/RuntimeTypeAdapterFactory.java
 */
// CHECKSTYLE:ON
package fr.cnes.regards.framework.gson.adapters;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.gson.utils.GSONUtils;

/**
 * Generic polymorphic adapter factory. This adapter is based on a discriminator field to explicitly map an entity to
 * its implementation.<br/>
 * A hierarchy of class has to embed a discriminator attribute to be able to use this adapter.
 * @param <E> entity base type
 * @author Marc Sordi
 */
public class PolymorphicTypeAdapterFactory<E> implements TypeAdapterFactory {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PolymorphicTypeAdapterFactory.class);

    /**
     * default discriminator field name, only used if discriminatorFieldName is empty
     */
    private static final String DEFAULT_DISCRIMINATOR_FIELD_NAME = "@type@";

    /**
     * Base hierarchy type
     */
    protected final Class<E> baseType;

    /**
     * JSON discriminator field name
     */
    protected final String discriminatorFieldName;

    /**
     * Whether field has to be injected because it doesn't exist in base type
     */
    protected final boolean injectField;

    /**
     * Map discriminator value to its corresponding explicit type
     */
    protected final ConcurrentMap<String, Class<?>> discriminatorToSubtype = new ConcurrentHashMap<>();

    /**
     * Map explicit type to its corresponding discriminator value
     */
    protected final ConcurrentMap<Class<?>, String> subtypeToDiscriminator = new ConcurrentHashMap<>();

    /**
     * Whether to refresh mapping after factory creation at runtime
     */
    protected boolean refreshMapping = false;

    /**
     * Map to identify types serialized even if null value
     */
    protected final ConcurrentMap<Class<?>, Boolean> subtypeSerializeNulls = new ConcurrentHashMap<>();

    /**
     * @param baseType base hierarchy type
     * @param discriminatorFieldName discriminator field name
     * @param injectField do not inject field if already exists else yes.
     */
    protected PolymorphicTypeAdapterFactory(Class<E> baseType, String discriminatorFieldName, boolean injectField) {
        GSONUtils.assertNotNull(baseType, "Base hierarchy type is required.");
        GSONUtils.assertNotNullOrEmpty(discriminatorFieldName, "Discriminator field name is required.");
        GSONUtils.assertNotNull(injectField, "Inject field is required.");

        this.baseType = baseType;
        this.discriminatorFieldName = discriminatorFieldName;
        this.injectField = injectField;

        LOGGER.debug("Managing polymorphic adapter for class \"{}\" and discriminator field \"{}\".",
                     this.baseType.getName(), this.discriminatorFieldName);
        if (this.injectField) {
            LOGGER.debug("Discriminator field will be injected dynamically.");
        }
    }

    /**
     * Init a {@link TypeAdapterFactory} with an existing discriminator field (so field is not injected)
     * @param baseType base hierarchy type
     * @param discriminatorFieldName discriminator field name
     */
    protected PolymorphicTypeAdapterFactory(Class<E> baseType, String discriminatorFieldName) {
        this(baseType, discriminatorFieldName, false);
    }

    /**
     * Inject default discriminator field name in the serialized object.
     * @param baseType base hierarchy type
     */
    protected PolymorphicTypeAdapterFactory(Class<E> baseType) {
        this(baseType, DEFAULT_DISCRIMINATOR_FIELD_NAME, true);
    }

    /**
     * Creates a new runtime type adapter using for {@code baseType} using {@code
     * typeFieldName} as the type field name. Type field names are case sensitive.
     * @param <T> base polymorphic type
     * @param baseType Base polymorphic class
     * @param discriminatorFieldName discriminator field name
     * @param injectField do not inject field if already exists else yes.
     * @return {@link PolymorphicTypeAdapterFactory}
     */
    public static <T> PolymorphicTypeAdapterFactory<T> of(Class<T> baseType, String discriminatorFieldName,
            boolean injectField) {
        return new PolymorphicTypeAdapterFactory<>(baseType, discriminatorFieldName, injectField);
    }

    /**
     * Create a {@link TypeAdapterFactory} injecting a default discriminator field in the {@link JsonObject}
     * @param <T> base polymorphic type
     * @param baseType base hierarchy type
     * @return {@link PolymorphicTypeAdapterFactory}
     */
    public static <T> PolymorphicTypeAdapterFactory<T> of(Class<T> baseType) {
        return new PolymorphicTypeAdapterFactory<>(baseType);
    }

    /**
     * Register a mapping between a field value and an explicit type
     * @param type type
     * @param discriminatorFieldValue field value
     * @param serializeNulls Enable null serialization for current type.<br/>
     * Warning: this action disables a global null serialization configuration! Do not call this method if
     * such
     * configuration is set globally.
     */
    public final void registerSubtype(Class<?> type, String discriminatorFieldValue, boolean serializeNulls) {
        refreshMapping = true;
        GSONUtils.assertNotNull(type, "Sub type is required.");
        GSONUtils.assertNotNull(discriminatorFieldValue, "Discriminator field value is required.");

        // Check inheritance dynamically
        if (!baseType.isAssignableFrom(type)) {
            final String errorMessage = String.format("Type %s not a subtype of %s.", type, baseType);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        LOGGER.debug("Subtype \"{}\" mapped to \"{}\" value", type, discriminatorFieldValue);

        // Check if map not already contains value with a different mapping
        if (discriminatorToSubtype.containsKey(discriminatorFieldValue) && (type != discriminatorToSubtype
                .get(discriminatorFieldValue))) {

            final String errorMessage = String
                    .format("Discrimator field value %s must be unique", discriminatorFieldValue);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        discriminatorToSubtype.put(discriminatorFieldValue, type);
        subtypeSerializeNulls.put(type, serializeNulls);

        // Reverse conversion only useful when injecting data
        if (injectField) {
            if (subtypeToDiscriminator.containsKey(type)) {
                final String errorMessage = String.format("Type %s must be unique", type);
                LOGGER.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            subtypeToDiscriminator.put(type, discriminatorFieldValue);
        }
    }

    public final void registerSubtype(Class<?> type, String discriminatorFieldValue) {
        registerSubtype(type, discriminatorFieldValue, false);
    }

    public final void registerSubtype(Class<?> type, Enum<?> enumDiscrimanatorValue, boolean serializeNulls) {
        registerSubtype(type, enumDiscrimanatorValue.toString(), serializeNulls);
    }

    public final void registerSubtype(Class<?> type, Enum<?> enumDiscrimanatorValue) {
        registerSubtype(type, enumDiscrimanatorValue.toString());
    }

    public final void registerSubtype(Class<?> type) {
        registerSubtype(type, type.getCanonicalName());
    }

    public final void registerSubtype(Class<?> type, boolean serializeNulls) {
        registerSubtype(type, type.getCanonicalName(), serializeNulls);
    }

    /**
     * Unregister mapping between a field value and an explicit type
     * @param type type
     * @param discriminatorFieldValue field value
     */
    public void unregisterSubtype(Class<?> type, String discriminatorFieldValue) {
        refreshMapping = true;
        GSONUtils.assertNotNull(type, "Sub type is required.");
        GSONUtils.assertNotNull(discriminatorFieldValue, "Discriminator field value is required.");

        LOGGER.debug("Subtype \"{}\" unmapped to \"{}\" value", type, discriminatorFieldValue);

        discriminatorToSubtype.remove(discriminatorFieldValue);
        if (injectField) {
            subtypeToDiscriminator.remove(type);
        }
    }

    /**
     * Store mappings
     * @param gson GSON
     * @param discriminatorAdapterMap mapping between discriminator value and adapter
     * @param subTypeAdapterMap mapping between sub type and adapter
     */
    protected void doMapping(Gson gson, Map<String, TypeAdapter<?>> discriminatorAdapterMap,
            Map<Class<?>, TypeAdapter<?>> subTypeAdapterMap) {

        // Clear maps before computing delegation
        discriminatorAdapterMap.clear();
        subTypeAdapterMap.clear();

        // Register TypeAdapter delegation mapping from discriminator and type
        for (Map.Entry<String, Class<?>> mapping : discriminatorToSubtype.entrySet()) {
            final TypeAdapter<?> delegate = gson.getDelegateAdapter(this, TypeToken.get(mapping.getValue()));
            discriminatorAdapterMap.put(mapping.getKey(), delegate);
            subTypeAdapterMap.put(mapping.getValue(), delegate);
        }

    }

    /**
     * Default behavior to retrieve discriminator on read.<br/>
     * Override this method to customize discriminator retrieval.
     * @param jsonElement parsed JSON
     * @return {@link JsonElement} containing a {@link String} representing the discriminator field value.
     */
    protected JsonElement getOnReadDiscriminator(JsonElement jsonElement) {
        JsonElement discriminator;
        if (injectField) {
            // Retrieve and remove injected field
            discriminator = jsonElement.getAsJsonObject().remove(discriminatorFieldName);
        } else {
            // Retrieve but DO NOT REMOVE existing field
            discriminator = jsonElement.getAsJsonObject().get(discriminatorFieldName);
        }
        return discriminator;
    }

    /**
     * Default behavior before parsing {@link JsonElement} to sub type.<br/>
     * Override this method to manipulate {@link JsonElement} before parsing it into target type.
     * @param jsonElement {@link JsonElement}
     * @param discriminator related discriminator value
     * @param subType target type
     * @return {@link JsonElement} that will be parsed.
     */
    protected JsonElement beforeRead(JsonElement jsonElement, String discriminator, Class<?> subType) {
        return jsonElement;
    }

    /**
     * Default behavior before writing {@link JsonElement} to output stream.<br/>
     * Override this method to manipulate {@link JsonElement} before writing it to JSON.
     * @param jsonElement {@link JsonElement}
     * @param subType target type
     * @return {@link JsonElement} that will be write on output stream.
     */
    protected JsonElement beforeWrite(JsonElement jsonElement, Class<?> subType) {

        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Clone object and inject field if needed
        if (injectField) {

            // Check field not already exists
            if (jsonObject.has(discriminatorFieldName)) {
                String format = "Discriminator field %s already exists. Change it or deny field injection.";
                String errorMessage = String.format(format, discriminatorFieldName);
                LOGGER.error(errorMessage);
                throw new JsonParseException(errorMessage);
            }

            // Inject discriminator field
            JsonObject clone = new JsonObject();

            String discriminatorFieldValue = subtypeToDiscriminator.get(subType);
            clone.add(discriminatorFieldName, new JsonPrimitive(discriminatorFieldValue));
            for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
                clone.add(e.getKey(), e.getValue());
            }
            return clone;
        }

        // Check field already exists
        if (!jsonObject.has(discriminatorFieldName)) {
            String format = "Discriminator field %s must exist. Change it or allow field injection.";
            String errorMessage = String.format(format, discriminatorFieldName);
            LOGGER.error(errorMessage);
            throw new JsonParseException(errorMessage);
        }

        return jsonObject;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        final Class<? super T> requestedType = typeToken.getRawType();
        if (!baseType.isAssignableFrom(requestedType)) {
            return null;
        }

        // If factory not already created, refresh not needed
        refreshMapping = false;

        final Map<String, TypeAdapter<?>> discriminatorToDelegate = new LinkedHashMap<>();
        final Map<Class<?>, TypeAdapter<?>> subtypeToDelegate = new LinkedHashMap<>();

        // Register TypeAdapter delegation mapping from discriminator and type
        doMapping(gson, discriminatorToDelegate, subtypeToDelegate);

        return new TypeAdapter<T>() { // NOSONAR

            /**
             * Delegate writing to default type adapter
             */
            @SuppressWarnings("unchecked")
            @Override
            public void write(JsonWriter out, T value) throws IOException {

                if (refreshMapping) {
                    doMapping(gson, discriminatorToDelegate, subtypeToDelegate);
                    refreshMapping = false;
                }

                final Class<?> srcType = value.getClass();

                // registration requires that subtype extends base type
                TypeAdapter<T> delegate = (TypeAdapter<T>) subtypeToDelegate.get(srcType);

                if (delegate == null) {
                    String errorMessage = String
                            .format("Cannot serialize %s. Did you forget to register a subtype?", srcType.getName());
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }

                // Raw JSON object
                JsonElement rawJson = delegate.toJsonTree(value);

                if (subtypeSerializeNulls.get(srcType)) {
                    out.setSerializeNulls(true);
                }
                Streams.write(beforeWrite(rawJson, srcType), out);
                if (subtypeSerializeNulls.get(srcType)) {
                    out.setSerializeNulls(false);
                }
            }

            /**
             * Delegate reading to type adapter mapped to extracted discriminator value
             */
            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) {

                if (refreshMapping) {
                    doMapping(gson, discriminatorToDelegate, subtypeToDelegate);
                    refreshMapping = false;
                }

                // Compute raw JSON object
                final JsonElement jsonElement = Streams.parse(in);

                // Discriminator value
                JsonElement discriminatorEl = getOnReadDiscriminator(jsonElement);

                // Check value found
                if (discriminatorEl == null) {
                    String errorMessage = String
                            .format("Cannot deserialize %s because it does not define a field named %s.", baseType,
                                    discriminatorFieldName);
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }

                final String discriminator = discriminatorEl.getAsString();
                // registration requires that sub type extends T
                TypeAdapter<T> delegate = (TypeAdapter<T>) discriminatorToDelegate.get(discriminator);

                if (delegate == null) {
                    String errorMessage = String
                            .format("Cannot deserialize %s subtype named %s. Did you forget to register a subtype?",
                                    baseType, discriminator);
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }

                return delegate.fromJsonTree(
                        beforeRead(jsonElement, discriminator, discriminatorToSubtype.get(discriminator)));
            }
        }.nullSafe();
    }
}
