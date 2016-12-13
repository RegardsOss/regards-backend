/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample6;

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
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 * @author Marc Sordi
 *
 */
@SuppressWarnings("rawtypes")
public class CustomPolymorphicTypeAdapterFactory extends PolymorphicTypeAdapterFactory<AbstractProperty> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomPolymorphicTypeAdapterFactory.class);

    protected CustomPolymorphicTypeAdapterFactory() {
        super(AbstractProperty.class, "name", false);
        registerSubtype(DateProperty.class, "date");
        registerSubtype(StringProperty.class, "string");
        registerSubtype(StringProperty.class, "CRS");
        registerSubtype(ObjectProperty.class, "GEO");
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory#create(com.google.gson.Gson,
     * com.google.gson.reflect.TypeToken)
     */
    @Override
    public <T> TypeAdapter<T> create(Gson pGson, TypeToken<T> pType) {
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
}
