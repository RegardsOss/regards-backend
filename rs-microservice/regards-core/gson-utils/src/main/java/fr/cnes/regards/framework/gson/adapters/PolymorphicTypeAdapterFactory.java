/*
 * LICENSE_PLACEHOLDER
 */
// CHECKSTYLE:OFF
/**
 *
 * Code inspired from https://github.com/google/gson/blob/master/extras/src/main/java/com/google/gson/typeadapters/RuntimeTypeAdapterFactory.java
 */
// CHECKSTYLE:ON
package fr.cnes.regards.framework.gson.adapters;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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

/**
 * Generic polymorphic adapter factory. This adapter is based on a discriminator field to explicitly map an entity to
 * its implementation.<br/>
 * A hierarchy of class has to embed a discriminator attribute to be able to use this adapter.
 *
 * @author Marc Sordi
 *
 * @param <E>
 *            entity base type
 */
public class PolymorphicTypeAdapterFactory<E> implements TypeAdapterFactory {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PolymorphicTypeAdapterFactory.class);

    /**
     * Base polymorphic class
     */
    private final Class<E> baseType;

    /**
     * JSON discriminator field name
     */
    private final String discriminatorFieldName;

    /**
     * default discriminator field name, only used if discriminatorFieldName is empty
     */
    private static final String DEFAULT_DISCRIMINATOR_FIELD_NAME = "@type@";

    /**
     * Map discriminator value to its corresponding explicit type
     */
    private final Map<String, Class<?>> discriminatorToSubtype = new LinkedHashMap<String, Class<?>>();

    /**
     * Creates a new runtime type adapter using for {@code baseType} using {@code
     * typeFieldName} as the type field name. Type field names are case sensitive.
     */
    public static <T> PolymorphicTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName) {
        return new PolymorphicTypeAdapterFactory<T>(baseType, typeFieldName);
    }

    protected PolymorphicTypeAdapterFactory(Class<E> pBaseType, String pDiscriminatorFieldName) {
        this.baseType = pBaseType;
        this.discriminatorFieldName = pDiscriminatorFieldName;
        LOG.info("Managing polymorphic adapter for class {} and discriminator field {}", baseType.getName(),
                 discriminatorFieldName);
    }

    /**
     * Register a mapping between a field value and an explicit type
     *
     * @param pType
     *            type
     * @param pDiscriminatorFieldValue
     *            field value
     */
    public void registerSubtype(Class<? extends E> pType, String pDiscriminatorFieldValue) {
        assertNotNull(pType, "Sub type is required.");
        assertNotNull(pDiscriminatorFieldValue, "Discriminator field value is required.");

        // Check if map not already contains value
        if (discriminatorToSubtype.containsKey(pDiscriminatorFieldValue)) {
            throw new IllegalArgumentException("Discrimator field value must be unique");
        }
        discriminatorToSubtype.put(pDiscriminatorFieldValue, pType);
    }

    /**
     * Register a mapping between an enumeration and an explicit type.
     *
     * @param pType
     *            type
     * @param pEnum
     *            enum value
     */
    public void registerSubtype(Class<? extends E> pType, Enum<?> pEnum) {
        registerSubtype(pType, pEnum.toString());
    }

    public void registerSubtype(Class<? extends E> pType) {
        registerSubtype(pType, pType.getCanonicalName());
    }

    // CHECKSTYLE:OFF
    @Override
    public <T> TypeAdapter<T> create(Gson pGson, TypeToken<T> pType) {
        if (pType.getRawType() != baseType) {
            return null;
        }

        final Map<String, TypeAdapter<?>> discriminatorToDelegate = new LinkedHashMap<String, TypeAdapter<?>>();
        final Map<Class<?>, TypeAdapter<?>> subtypeToDelegate = new LinkedHashMap<Class<?>, TypeAdapter<?>>();

        /**
         * Register TypeAdapter delegation mapping from discriminator and type
         */
        for (Map.Entry<String, Class<?>> mapping : discriminatorToSubtype.entrySet()) {
            final TypeAdapter<?> delegate = pGson.getDelegateAdapter(this, TypeToken.get(mapping.getValue()));
            discriminatorToDelegate.put(mapping.getKey(), delegate);
            subtypeToDelegate.put(mapping.getValue(), delegate);
        }

        return new TypeAdapter<T>() {

            /**
             * Delegate writing to default type adapter
             */
            @Override
            public void write(JsonWriter pOut, T pValue) throws IOException {

                final Class<?> srcType = pValue.getClass();
                // String label = subtypeToLabel.get(srcType);
                @SuppressWarnings("unchecked") // registration requires that subtype extends base type
                final TypeAdapter<T> delegate = (TypeAdapter<T>) subtypeToDelegate.get(srcType);
                if (delegate == null) {
                    throw new JsonParseException(
                            "Cannot serialize " + srcType.getName() + "; did you forget to register a subtype?");
                }
                JsonElement cloneToWrite = cloneForWrite(delegate, pValue);

                // Streams.write(delegate.toJsonTree(pValue), pOut);
                Streams.write(cloneToWrite, pOut);
            }

            /**
             * clone pValue and add a type field(@type@) only if needed
             *
             * @param delegate
             * @param pValue
             * @return JsonObject fully qualified for correct polymorphic deserialization from json
             */
            private JsonObject cloneForWrite(TypeAdapter<T> delegate, T pValue) {
                JsonObject jsonObject = delegate.toJsonTree(pValue).getAsJsonObject();
                if (discriminatorFieldName.isEmpty()) {
                    final Class<?> srcType = pValue.getClass();
                    final String label = pValue.getClass().getCanonicalName();
                    if (jsonObject.has(DEFAULT_DISCRIMINATOR_FIELD_NAME)) {
                        throw new JsonParseException("cannot serialize " + srcType.getName()
                                + " because it already defines a field named " + DEFAULT_DISCRIMINATOR_FIELD_NAME);
                    }
                    JsonObject clone = new JsonObject();
                    clone.add(DEFAULT_DISCRIMINATOR_FIELD_NAME, new JsonPrimitive(label));
                    for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
                        clone.add(e.getKey(), e.getValue());
                    }
                    return clone;
                }
                return jsonObject;
            }

            /**
             * Delegate reading to type adapter mapped to extracted discriminator value
             */
            @Override
            public T read(JsonReader pIn) throws IOException {
                final JsonElement jsonElement = Streams.parse(pIn);
                JsonElement discriminatorEl;
                if (discriminatorFieldName.isEmpty()) {
                    discriminatorEl = jsonElement.getAsJsonObject().remove(DEFAULT_DISCRIMINATOR_FIELD_NAME);
                    if (discriminatorEl == null) {
                        throw new JsonParseException("Cannot deserialize " + baseType
                                + " because it does not define a field named " + DEFAULT_DISCRIMINATOR_FIELD_NAME);
                    }
                } else {
                    discriminatorEl = jsonElement.getAsJsonObject().get(discriminatorFieldName);
                    if (discriminatorEl == null) {
                        throw new JsonParseException("Cannot deserialize " + baseType
                                + " because it does not define a field named " + discriminatorFieldName);
                    }
                }
                final String discriminator = discriminatorEl.getAsString();
                @SuppressWarnings("unchecked") // registration requires that subtype extends T
                final TypeAdapter<T> delegate = (TypeAdapter<T>) discriminatorToDelegate.get(discriminator);
                if (delegate == null) {
                    throw new JsonParseException("Cannot deserialize " + baseType + " subtype named " + discriminator
                            + "; did you forget to register a subtype?");
                }
                JsonObject toBeRead = readFrom(jsonElement);
                return delegate.fromJsonTree(toBeRead);
            }

            private JsonObject readFrom(JsonElement pJsonElement) {
                final JsonObject jsonObject = pJsonElement.getAsJsonObject();
                if (discriminatorFieldName.isEmpty()) {

                }
                return jsonObject;
            }
        }.nullSafe();
    }
    // CHECKSTYLE:ON

    private void assertNotNull(Object pObject, String pErrorMessage) {
        if (pObject == null) {
            throw new IllegalArgumentException(pErrorMessage);
        }
    }
}
