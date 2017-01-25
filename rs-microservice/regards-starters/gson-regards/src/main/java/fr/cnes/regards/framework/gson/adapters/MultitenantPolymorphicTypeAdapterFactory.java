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
import java.util.HashMap;
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
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

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
public class MultitenantPolymorphicTypeAdapterFactory<E> implements TypeAdapterFactory {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultitenantPolymorphicTypeAdapterFactory.class);

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
    protected final Map<String, Map<String, Class<?>>> discriminatorToSubtype = new LinkedHashMap<>();

    /**
     * Map explicit type to its corresponding discriminator value
     */
    protected final Map<String, Map<Class<?>, String>> subtypeToDiscriminator = new LinkedHashMap<>();

    /**
     * Whether to refresh mapping after factory creation at runtime
     */
    protected Map<String, Boolean> refreshMapping = new LinkedHashMap<>();

    /**
     * Resolve thread tenant at runtime
     */
    protected IRuntimeTenantResolver runtimeTenantResolver;

    /**
     *
     * Constructor
     *
     * @param pTenantResolver
     *            tenant resolver
     * @param pBaseType
     *            base hierarchy type
     * @param pDiscriminatorFieldName
     *            discriminator field name
     * @param pInjectField
     *            do not inject field if already exists else yes.
     */
    protected MultitenantPolymorphicTypeAdapterFactory(IRuntimeTenantResolver pTenantResolver, Class<E> pBaseType,
            String pDiscriminatorFieldName, boolean pInjectField) {
        GSONUtils.assertNotNull(pTenantResolver, "Dynamic tenant resolver is required.");
        GSONUtils.assertNotNull(pBaseType, "Base hierarchy type is required.");
        GSONUtils.assertNotNullOrEmpty(pDiscriminatorFieldName, "Discriminator field name is required.");
        GSONUtils.assertNotNull(pInjectField, "Inject field is required.");

        this.runtimeTenantResolver = pTenantResolver;
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
     * @param pTenantResolver
     *            tenant resolver
     * @param pBaseType
     *            base hierarchy type
     * @param pDiscriminatorFieldName
     *            discriminator field name
     */
    protected MultitenantPolymorphicTypeAdapterFactory(IRuntimeTenantResolver pTenantResolver, Class<E> pBaseType,
            String pDiscriminatorFieldName) {
        this(pTenantResolver, pBaseType, pDiscriminatorFieldName, false);
    }

    /**
     * Inject default discriminator field name in the serialized object.
     *
     * @param pTenantResolver
     *            tenant resolver
     * @param pBaseType
     *            base hierarchy type
     */
    protected MultitenantPolymorphicTypeAdapterFactory(IRuntimeTenantResolver pTenantResolver, Class<E> pBaseType) {
        this(pTenantResolver, pBaseType, DEFAULT_DISCRIMINATOR_FIELD_NAME, true);
    }

    /**
     * Register a mapping between a field value and an explicit type
     *
     * @param pTenant
     *            tenant
     * @param pType
     *            type
     * @param pDiscriminatorFieldValue
     *            field value
     *
     */
    public void registerSubtype(String pTenant, Class<?> pType, String pDiscriminatorFieldValue) {
        setRefreshMapping(pTenant, true);
        GSONUtils.assertNotNull(pType, "Sub type is required.");
        GSONUtils.assertNotNull(pDiscriminatorFieldValue, "Discriminator field value is required.");

        // Check inheritance dynamically
        if (!baseType.isAssignableFrom(pType)) {
            final String errorMessage = String.format("Type %s not a subtype of %s.", pType, baseType);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        LOGGER.info("Subtype \"{}\" mapped to \"{}\" value for tenant \"{}\"", pType, pDiscriminatorFieldValue,
                    pTenant);

        // Retrieve tenant map
        Map<String, Class<?>> tenantDiscriminatorToSubtype = getTenantDiscriminatorToSubtype(pTenant);
        // Check if map not already contains value with a different mapping
        if (tenantDiscriminatorToSubtype.containsKey(pDiscriminatorFieldValue)
                && (pType != tenantDiscriminatorToSubtype.get(pDiscriminatorFieldValue))) {

            final String errorMessage = String.format("Discrimator field value %s must be unique",
                                                      pDiscriminatorFieldValue);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        tenantDiscriminatorToSubtype.put(pDiscriminatorFieldValue, pType);

        // Reverse conversion only useful when injecting data
        if (injectField) {
            // Retrieve tenant map
            Map<Class<?>, String> tenantSubtypeToDiscriminator = getTenantSubtypeToDiscriminator(pTenant);
            if (tenantSubtypeToDiscriminator.containsKey(pType)) {
                final String errorMessage = String.format("Type %s must be unique", pType);
                LOGGER.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            tenantSubtypeToDiscriminator.put(pType, pDiscriminatorFieldValue);
        }
    }

    /**
     * Unregister mapping between a field value and an explicit type
     *
     * @param pTenant
     *            tenant
     * @param pType
     *            type
     * @param pDiscriminatorFieldValue
     *            field value
     */
    public void unregisterSubtype(String pTenant, Class<?> pType, String pDiscriminatorFieldValue) {
        setRefreshMapping(pTenant, true);
        GSONUtils.assertNotNull(pType, "Sub type is required.");
        GSONUtils.assertNotNull(pDiscriminatorFieldValue, "Discriminator field value is required.");

        LOGGER.info("Subtype \"{}\" unmapped to \"{}\" value", pType, pDiscriminatorFieldValue);

        // Retrieve tenant map
        Map<String, Class<?>> tenantDiscriminatorToSubtype = getTenantDiscriminatorToSubtype(pTenant);
        tenantDiscriminatorToSubtype.remove(pDiscriminatorFieldValue);
        if (injectField) {
            // Retrieve tenant map
            Map<Class<?>, String> tenantSubtypeToDiscriminator = getTenantSubtypeToDiscriminator(pTenant);
            tenantSubtypeToDiscriminator.remove(pType);
        }
    }

    /**
     * Register a mapping between an enumeration and an explicit type.
     *
     * @param pType
     *            type
     * @param pEnum
     *            enum value
     * @param pTenant
     *            tenant
     */
    public void registerSubtype(String pTenant, Class<?> pType, Enum<?> pEnum) {
        registerSubtype(pTenant, pType, pEnum.toString());
    }

    public void registerSubtype(String pTenant, Class<?> pType) {
        registerSubtype(pTenant, pType, pType.getCanonicalName());
    }

    protected void setRefreshMapping(String pTenant, Boolean pRefreshMapping) {
        refreshMapping.put(pTenant, pRefreshMapping);
    }

    protected void resetRefreshMapping() {
        for (String tenant : refreshMapping.keySet()) {
            refreshMapping.put(tenant, false);
        }
    }

    protected boolean needRefreshMapping(String pTenant) {
        return refreshMapping.get(pTenant);
    }

    protected Map<String, Class<?>> getTenantDiscriminatorToSubtype(String pTenant) {
        Map<String, Class<?>> map = discriminatorToSubtype.get(pTenant);
        if (map == null) {
            map = new LinkedHashMap<>();
            discriminatorToSubtype.put(pTenant, map);
        }
        return map;
    }

    protected Map<Class<?>, String> getTenantSubtypeToDiscriminator(String pTenant) {
        Map<Class<?>, String> map = subtypeToDiscriminator.get(pTenant);
        if (map == null) {
            map = new LinkedHashMap<>();
            subtypeToDiscriminator.put(pTenant, map);
        }
        return map;
    }

    /**
     * Store mappings
     *
     * @param pGson
     *            GSON
     * @param pDiscriminatorToDelegate
     *            mapping between discriminator value and adapter
     * @param pSubtypeToDelegate
     *            mapping between sub type and adapter
     * @param pTenant
     *            tenant
     */
    protected void doTenantMapping(Gson pGson, Map<String, TypeAdapter<?>> pDiscriminatorToDelegate,
            Map<Class<?>, TypeAdapter<?>> pSubtypeToDelegate, String pTenant) {

        // Clear maps before computing delegation
        pDiscriminatorToDelegate.clear();
        pSubtypeToDelegate.clear();

        /**
         * Register TypeAdapter delegation mapping from discriminator and type
         */
        for (Map.Entry<String, Class<?>> mapping : getTenantDiscriminatorToSubtype(pTenant).entrySet()) {
            final TypeAdapter<?> delegate = pGson.getDelegateAdapter(this, TypeToken.get(mapping.getValue()));
            pDiscriminatorToDelegate.put(mapping.getKey(), delegate);
            pSubtypeToDelegate.put(mapping.getValue(), delegate);
        }
    }

    protected synchronized void doMapping(Gson pGson, Map<String, Map<String, TypeAdapter<?>>> pDiscriminatorToDelegate,
            Map<String, Map<Class<?>, TypeAdapter<?>>> pSubtypeToDelegate, String pTenant) {
        if (pTenant != null) {
            doTenantMapping(pGson, getTenantDiscriminatorToDelegate(pDiscriminatorToDelegate, pTenant),
                            getTenantSubtypeToDelegate(pSubtypeToDelegate, pTenant), pTenant);
        } else {
            for (String tenant : discriminatorToSubtype.keySet()) {
                doTenantMapping(pGson, getTenantDiscriminatorToDelegate(pDiscriminatorToDelegate, tenant),
                                getTenantSubtypeToDelegate(pSubtypeToDelegate, tenant), tenant);
            }
        }
    }

    protected Map<String, TypeAdapter<?>> getTenantDiscriminatorToDelegate(
            Map<String, Map<String, TypeAdapter<?>>> pDiscriminatorToDelegate, String pTenant) {
        Map<String, TypeAdapter<?>> map = pDiscriminatorToDelegate.get(pTenant);
        if (map == null) {
            map = new HashMap<>();
            pDiscriminatorToDelegate.put(pTenant, map);
        }
        return map;
    }

    protected Map<Class<?>, TypeAdapter<?>> getTenantSubtypeToDelegate(
            Map<String, Map<Class<?>, TypeAdapter<?>>> pSubtypeToDelegate, String pTenant) {
        Map<Class<?>, TypeAdapter<?>> map = pSubtypeToDelegate.get(pTenant);
        if (map == null) {
            map = new HashMap<>();
            pSubtypeToDelegate.put(pTenant, map);
        }
        return map;
    }

    /**
     * Default behavior to retrieve discriminator on read.<br/>
     * Override this method to customize discriminator retrieval.
     *
     * @param pJsonElement
     *            parsed JSON
     * @return {@link JsonElement} containing a {@link String} representing the discriminator field value.
     */
    protected JsonElement getOnReadDiscriminator(JsonElement pJsonElement) {
        JsonElement discriminator;
        if (injectField) {
            // Retrieve and remove injected field
            discriminator = pJsonElement.getAsJsonObject().remove(discriminatorFieldName);
        } else {
            // Retrieve but DO NOT REMOVE existing field
            discriminator = pJsonElement.getAsJsonObject().get(discriminatorFieldName);
        }
        return discriminator;
    }

    /**
     * Default behavior before parsing {@link JsonElement} to sub type.<br/>
     * Override this method to manipulate {@link JsonElement} before parsing it into target type.
     *
     * @param pJsonElement
     *            {@link JsonElement}
     * @param pDiscriminator
     *            related discriminator value
     * @param pSubType
     *            target type
     * @return {@link JsonElement} that will be parsed.
     */
    protected JsonElement beforeRead(JsonElement pJsonElement, String pDiscriminator, Class<?> pSubType) { // NOSONAR
        return pJsonElement;
    }

    /**
     * Default behavior before writing {@link JsonElement} to output stream.<br/>
     * Override this method to manipulate {@link JsonElement} before writing it to JSON.
     *
     * @param pJsonElement
     *            {@link JsonElement}
     * @param pSubType
     *            target type
     * @return {@link JsonElement} that will be write on output stream.
     */
    protected JsonElement beforeWrite(JsonElement pJsonElement, Class<?> pSubType) { // NOSONAR

        JsonObject jsonObject = pJsonElement.getAsJsonObject();

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

            String discriminatorFieldValue = getTenantSubtypeToDiscriminator(runtimeTenantResolver.getTenant()).get(pSubType);
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

    // CHECKSTYLE:OFF
    @Override
    public <T> TypeAdapter<T> create(Gson pGson, TypeToken<T> pType) { // NOSONAR
        // If factory not already created, refresh not needed
        resetRefreshMapping();

        final Class<? super T> requestedType = pType.getRawType();
        if (!baseType.isAssignableFrom(requestedType)) {
            return null;
        }

        // Tenant maps
        final Map<String, Map<String, TypeAdapter<?>>> discriminatorToDelegate = new LinkedHashMap<>();
        final Map<String, Map<Class<?>, TypeAdapter<?>>> subtypeToDelegate = new LinkedHashMap<>();

        // Register TypeAdapter delegation mapping from discriminator and type
        doMapping(pGson, discriminatorToDelegate, subtypeToDelegate, null);

        return new TypeAdapter<T>() { // NOSONAR

            /**
             * Delegate writing to default type adapter
             */
            @Override
            public void write(JsonWriter pOut, T pValue) throws IOException {

                if (needRefreshMapping(runtimeTenantResolver.getTenant())) {
                    doMapping(pGson, discriminatorToDelegate, subtypeToDelegate, runtimeTenantResolver.getTenant());
                    setRefreshMapping(runtimeTenantResolver.getTenant(), false);
                }

                final Class<?> srcType = pValue.getClass();

                // registration requires that subtype extends base type
                TypeAdapter<T> delegate = getDelegate(srcType);

                if (delegate == null) {
                    String errorMessage = String.format("Cannot serialize %s. Did you forget to register a subtype?",
                                                        srcType.getName());
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }

                // Raw JSON object
                JsonElement rawJson = delegate.toJsonTree(pValue);
                Streams.write(beforeWrite(rawJson, srcType), pOut);
            }

            @SuppressWarnings("unchecked")
            protected TypeAdapter<T> getDelegate(Class<?> pType) {
                Map<Class<?>, TypeAdapter<?>> map = subtypeToDelegate.get(runtimeTenantResolver.getTenant());
                if (map != null) {
                    return (TypeAdapter<T>) map.get(pType);
                }
                return null;
            }

            /**
             * Delegate reading to type adapter mapped to extracted discriminator value
             */
            @Override
            public T read(JsonReader pIn) throws IOException {

                if (needRefreshMapping(runtimeTenantResolver.getTenant())) {
                    doMapping(pGson, discriminatorToDelegate, subtypeToDelegate, runtimeTenantResolver.getTenant());
                    setRefreshMapping(runtimeTenantResolver.getTenant(), false);
                }

                // Compute raw JSON object
                final JsonElement jsonElement = Streams.parse(pIn);

                // Discriminator value
                JsonElement discriminatorEl = getOnReadDiscriminator(jsonElement);

                // Check value found
                if (discriminatorEl == null) {
                    String errorMessage = String.format(
                                                        "Cannot deserialize %s because it does not define a field named %s.",
                                                        baseType, discriminatorFieldName);
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }

                final String discriminator = discriminatorEl.getAsString();
                // registration requires that sub type extends T
                TypeAdapter<T> delegate = getDelegate(discriminator);

                if (delegate == null) {
                    String errorMessage = String.format(
                                                        "Cannot deserialize %s subtype named %s. Did you forget to register a subtype?",
                                                        baseType, discriminator);
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }

                return delegate.fromJsonTree(beforeRead(jsonElement, discriminator,
                                                        getTenantDiscriminatorToSubtype(runtimeTenantResolver.getTenant())
                                                                .get(discriminator)));
            }

            @SuppressWarnings("unchecked")
            protected TypeAdapter<T> getDelegate(String pDiscriminatorFieldValue) {
                Map<String, TypeAdapter<?>> map = discriminatorToDelegate.get(runtimeTenantResolver.getTenant());
                if (map != null) {
                    return (TypeAdapter<T>) map.get(pDiscriminatorFieldValue);
                }
                return null;
            }
        }.nullSafe();
    }
    // CHECKSTYLE:ON
}
