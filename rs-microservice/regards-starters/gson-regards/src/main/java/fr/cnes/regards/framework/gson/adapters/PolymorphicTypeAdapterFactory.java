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

import fr.cnes.regards.framework.gson.utils.GSONUtils;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(PolymorphicTypeAdapterFactory.class);

    /**
     * default discriminator field name, only used if discriminatorFieldName is empty
     */
    private static final String DEFAULT_DISCRIMINATOR_FIELD_NAME = "@type@";

    /**
     * Base hierarchy type
     */
    private final Class<E> baseType;

    /**
     * JSON discriminator field name
     */
    private final String discriminatorFieldName;

    /**
     * Whether field has to be injected because it doesn't exist in base type
     */
    private final boolean injectField;

    /**
     * Map discriminator value to its corresponding explicit type
     */
    private final Map<String, Class<?>> discriminatorToSubtype = new LinkedHashMap<>();

    /**
     * Map explicit type to its corresponding discriminator value
     */
    private final Map<Class<?>, String> subtypeToDiscriminator = new LinkedHashMap<>();

    /**
     *
     * @param pBaseType
     *            base hierarchy type
     * @param pDiscriminatorFieldName
     *            discriminator field name
     * @param pInjectField
     *            do not inject field if already exists else yes.
     */
    protected PolymorphicTypeAdapterFactory(Class<E> pBaseType, String pDiscriminatorFieldName, boolean pInjectField) {
        GSONUtils.assertNotNull(pBaseType, "Base hierarchy type is required.");
        GSONUtils.assertNotNullOrEmpty(pDiscriminatorFieldName, "Discriminator field name is required.");
        GSONUtils.assertNotNull(pInjectField, "Inject field is required.");

        this.baseType = pBaseType;
        this.discriminatorFieldName = pDiscriminatorFieldName;
        this.injectField = pInjectField;

        LOGGER.info("Managing polymorphic adapter for class \"{}\" and discriminator field \"{}\".", baseType.getName(),
                    discriminatorFieldName);
        if (injectField) {
            LOGGER.info("Discriminator field will be injected dynamically.");
        }
    }

    /**
     * Init a {@link TypeAdapterFactory} with an existing discriminator field (so field is not injected)
     *
     * @param pBaseType
     *            base hierarchy type
     * @param pDiscriminatorFieldName
     *            discriminator field name
     */
    protected PolymorphicTypeAdapterFactory(Class<E> pBaseType, String pDiscriminatorFieldName) {
        this(pBaseType, pDiscriminatorFieldName, false);
    }

    /**
     * Inject default discriminator field name in the serialized object.
     *
     * @param pBaseType
     *            base hierarchy type
     */
    protected PolymorphicTypeAdapterFactory(Class<E> pBaseType) {
        this(pBaseType, DEFAULT_DISCRIMINATOR_FIELD_NAME, true);
    }

    /**
     * Creates a new runtime type adapter using for {@code baseType} using {@code
     * typeFieldName} as the type field name. Type field names are case sensitive.
     *
     * @param <T>
     *            base polymorphic type
     *
     * @param pBaseType
     *            Base polymorphic class
     * @param pDiscriminatorFieldName
     *            discriminator field name
     * @param pInjectField
     *            do not inject field if already exists else yes.
     * @return {@link PolymorphicTypeAdapterFactory}
     *
     */
    public static <T> PolymorphicTypeAdapterFactory<T> of(Class<T> pBaseType, String pDiscriminatorFieldName,
            boolean pInjectField) {
        return new PolymorphicTypeAdapterFactory<>(pBaseType, pDiscriminatorFieldName, pInjectField);
    }

    /**
     * Create a {@link TypeAdapterFactory} injecting a default discriminator field in the {@link JsonObject}
     *
     * @param <T>
     *            base polymorphic type
     * @param pBaseType
     *            base hierarchy type
     * @return {@link PolymorphicTypeAdapterFactory}
     */
    public static <T> PolymorphicTypeAdapterFactory<T> of(Class<T> pBaseType) {
        return new PolymorphicTypeAdapterFactory<>(pBaseType);
    }

    /**
     * Register a mapping between a field value and an explicit type
     *
     * @param pType
     *            type
     * @param pDiscriminatorFieldValue
     *            field value
     */
    public void registerSubtype(Class<?> pType, String pDiscriminatorFieldValue) {
        GSONUtils.assertNotNull(pType, "Sub type is required.");
        GSONUtils.assertNotNull(pDiscriminatorFieldValue, "Discriminator field value is required.");

        // Check inheritance dynamically
        if (!baseType.isAssignableFrom(pType)) {
            final String errorMessage = String.format("Type %s not a subtype of %s.", pType, baseType);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        LOGGER.info("Subtype \"{}\" mapped to \"{}\" value", pType, pDiscriminatorFieldValue);

        // Check if map not already contains value
        if (discriminatorToSubtype.containsKey(pDiscriminatorFieldValue)) {
            final String errorMessage = String.format("Discrimator field value %s must be unique",
                                                      pDiscriminatorFieldValue);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        if (subtypeToDiscriminator.containsKey(pType)) {
            final String errorMessage = String.format("Type %s must be unique", pType);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        discriminatorToSubtype.put(pDiscriminatorFieldValue, pType);
        subtypeToDiscriminator.put(pType, pDiscriminatorFieldValue);
    }

    /**
     * Register a mapping between an enumeration and an explicit type.
     *
     * @param pType
     *            type
     * @param pEnum
     *            enum value
     */
    public void registerSubtype(Class<?> pType, Enum<?> pEnum) {
        registerSubtype(pType, pEnum.toString());
    }

    public void registerSubtype(Class<?> pType) {
        registerSubtype(pType, pType.getCanonicalName());
    }

    // CHECKSTYLE:OFF
    @Override
    public <T> TypeAdapter<T> create(Gson pGson, TypeToken<T> pType) { // NOSONAR
        final Class<? super T> requestedType = pType.getRawType();
        if (!baseType.isAssignableFrom(requestedType)) {
            return null;
        }

        final Map<String, TypeAdapter<?>> discriminatorToDelegate = new LinkedHashMap<>();
        final Map<Class<?>, TypeAdapter<?>> subtypeToDelegate = new LinkedHashMap<>();

        /**
         * Register TypeAdapter delegation mapping from discriminator and type
         */
        for (Map.Entry<String, Class<?>> mapping : discriminatorToSubtype.entrySet()) {
            final TypeAdapter<?> delegate = pGson.getDelegateAdapter(this, TypeToken.get(mapping.getValue()));
            discriminatorToDelegate.put(mapping.getKey(), delegate);
            subtypeToDelegate.put(mapping.getValue(), delegate);
        }

        return new TypeAdapter<T>() { // NOSONAR

            /**
             * Delegate writing to default type adapter
             */
            @Override
            public void write(JsonWriter pOut, T pValue) throws IOException {

                final Class<?> srcType = pValue.getClass();

                @SuppressWarnings("unchecked") // registration requires that subtype extends base type
                final TypeAdapter<T> delegate = (TypeAdapter<T>) subtypeToDelegate.get(srcType);
                if (delegate == null) {
                    String errorMessage = String.format("Cannot serialize %s. Did you forget to register a subtype?",
                                                        srcType.getName());
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }
                Streams.write(getJsonObject(delegate, pValue), pOut);
            }

            /**
             * Compute {@link JsonObject} to write. If field injection is required, clone the target object and add
             * inject the field dynamically.
             *
             * @param delegate
             *            {@link TypeAdapter} to transform object in JSON
             * @param pValue
             *            object to transform
             * @return JsonObject fully qualified for correct polymorphic deserialization from json
             */
            private JsonObject getJsonObject(TypeAdapter<T> delegate, T pValue) {

                // Compute raw JSON object
                JsonObject jsonObject = delegate.toJsonTree(pValue).getAsJsonObject();

                // Clone object and inject field if needed
                if (injectField) {

                    // Check field not already exists
                    if (jsonObject.has(discriminatorFieldName)) {
                        String errorMessage = String.format(
                                                            "Discriminator field %s already exists. Change it or deny field injection.",
                                                            discriminatorFieldName);
                        LOGGER.error(errorMessage);
                        throw new JsonParseException(errorMessage);
                    }

                    // Inject discriminator field
                    JsonObject clone = new JsonObject();

                    String discriminatorFieldValue = subtypeToDiscriminator.get(pValue.getClass());
                    clone.add(discriminatorFieldName, new JsonPrimitive(discriminatorFieldValue));
                    for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
                        clone.add(e.getKey(), e.getValue());
                    }
                    return clone;
                }

                // Check field already exists
                if (!jsonObject.has(discriminatorFieldName)) {
                    String errorMessage = String.format(
                                                        "Discriminator field %s must exist. Change it or allow field injection.",
                                                        discriminatorFieldName);
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }

                return jsonObject;
            }

            /**
             * Delegate reading to type adapter mapped to extracted discriminator value
             */
            @Override
            public T read(JsonReader pIn) throws IOException {

                // Compute raw JSON object
                final JsonElement jsonElement = Streams.parse(pIn);

                // Discriminator value
                JsonElement discriminatorEl;
                if (injectField) {
                    // Retrieve and remove injected field
                    discriminatorEl = jsonElement.getAsJsonObject().remove(discriminatorFieldName);
                } else {
                    // Retrieve but DO NOT REMOVE existing field
                    discriminatorEl = jsonElement.getAsJsonObject().get(discriminatorFieldName);
                }

                // Check value found
                if (discriminatorEl == null) {
                    String errorMessage = String.format(
                                                        "Cannot deserialize %s because it does not define a field named %s.",
                                                        baseType, discriminatorFieldName);
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }

                final String discriminator = discriminatorEl.getAsString();
                @SuppressWarnings("unchecked") // registration requires that sub type extends T
                final TypeAdapter<T> delegate = (TypeAdapter<T>) discriminatorToDelegate.get(discriminator);

                if (delegate == null) {
                    String errorMessage = String.format(
                                                        "Cannot deserialize %s subtype named %s. Did you forget to register a subtype?",
                                                        baseType, discriminator);
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }
                return delegate.fromJsonTree(jsonElement);
            }

        }.nullSafe();
    }
    // CHECKSTYLE:ON
}
