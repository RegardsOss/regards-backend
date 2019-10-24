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
// CHECKSTYLE:OFF
/**
 * Code inspired from
 * https://github.com/google/gson/blob/master/extras/src/main/java/com/google/gson/typeadapters/RuntimeTypeAdapterFactory.java
 */
// CHECKSTYLE:ON
package fr.cnes.regards.framework.gson.adapters;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
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
 * @param <E> entity base type
 * @author Marc Sordi
 * @author oroussel
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
    protected final ConcurrentMap<String, Map<String, Class<?>>> discToSubtypeMap = new ConcurrentHashMap<>();

    /**
     * Map explicit type to its corresponding discriminator value
     */
    protected final ConcurrentMap<String, Map<Class<?>, String>> subtypeToDiscMap = new ConcurrentHashMap<>();

    /**
     * Whether to refresh mapping after factory creation at runtime
     */
    protected ConcurrentMap<String, Boolean> refreshMappingMap = new ConcurrentHashMap<>();

    /**
     * Resolve thread tenant at runtime
     */
    protected IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Map discriminator value to its corresponding type adapter often called delegate
     */
    protected final ConcurrentMap<String, Map<String, TypeAdapter<?>>> discToDelegateMap = new ConcurrentHashMap<>();

    /**
     * Map explicit type to its corresponding type adapter often called delegate
     */
    protected final ConcurrentMap<String, Map<Class<?>, TypeAdapter<?>>> subtypeToDelegateMap = new ConcurrentHashMap<>();

    /**
     * Constructor
     * @param tenantResolver tenant resolver
     * @param baseType base hierarchy type
     * @param discriminatorFieldName discriminator field name
     * @param injectField do not inject field if already exists else yes.
     */
    protected MultitenantPolymorphicTypeAdapterFactory(IRuntimeTenantResolver tenantResolver, Class<E> baseType,
            String discriminatorFieldName, boolean injectField) {
        GSONUtils.assertNotNull(tenantResolver, "Dynamic tenant resolver is required.");
        GSONUtils.assertNotNull(baseType, "Base hierarchy type is required.");
        GSONUtils.assertNotNullOrEmpty(discriminatorFieldName, "Discriminator field name is required.");
        GSONUtils.assertNotNull(injectField, "Inject field is required.");

        this.runtimeTenantResolver = tenantResolver;
        this.baseType = baseType;
        this.discriminatorFieldName = discriminatorFieldName;
        this.injectField = injectField;

        LOGGER.info("Managing polymorphic adapter for class \"{}\" and discriminator field \"{}\".",
                    this.baseType.getName(), this.discriminatorFieldName);
        if (this.injectField) {
            LOGGER.info("Discriminator field will be injected dynamically.");
        }
    }

    /**
     * Init a {@link TypeAdapterFactory} with an existing discriminator field (so field is not injected)
     * @param tenantResolver tenant resolver
     * @param baseType base hierarchy type
     * @param discriminatorFieldName discriminator field name
     */
    protected MultitenantPolymorphicTypeAdapterFactory(IRuntimeTenantResolver tenantResolver, Class<E> baseType,
            String discriminatorFieldName) {
        this(tenantResolver, baseType, discriminatorFieldName, false);
    }

    /**
     * Inject default discriminator field name in the serialized object.
     * @param tenantResolver tenant resolver
     * @param baseType base hierarchy type
     */
    protected MultitenantPolymorphicTypeAdapterFactory(IRuntimeTenantResolver tenantResolver, Class<E> baseType) {
        this(tenantResolver, baseType, DEFAULT_DISCRIMINATOR_FIELD_NAME, true);
    }

    /**
     * Register a mapping between a field value and an explicit type
     * @param tenant tenant
     * @param type type
     * @param discriminatorFieldValue field value
     */
    public void registerSubtype(String tenant, Class<?> type, String discriminatorFieldValue) {
        setRefreshMapping(tenant, true);
        GSONUtils.assertNotNull(type, "Sub type is required.");
        GSONUtils.assertNotNull(discriminatorFieldValue, "Discriminator field value is required.");

        // Check inheritance dynamically
        if (!baseType.isAssignableFrom(type)) {
            final String errorMessage = String.format("Type %s not a subtype of %s.", type, baseType);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        LOGGER.debug("Subtype \"{}\" mapped to \"{}\" value for tenant \"{}\"", type, discriminatorFieldValue, tenant);

        // Retrieve tenant map
        Map<String, Class<?>> tenantDiscriminatorToSubtype = getTenantDiscriminatorToSubtype(tenant);
        // Check if map not already contains value with a different mapping
        if (tenantDiscriminatorToSubtype.containsKey(discriminatorFieldValue)
                && type != tenantDiscriminatorToSubtype.get(discriminatorFieldValue)) {

            final String errorMessage = String.format("Discrimator field value %s must be unique",
                                                      discriminatorFieldValue);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        tenantDiscriminatorToSubtype.put(discriminatorFieldValue, type);

        // Reverse conversion only useful when injecting data
        if (injectField) {
            // Retrieve tenant map
            Map<Class<?>, String> tenantSubtypeToDiscriminator = getTenantSubtypeToDiscriminator(tenant);
            if (tenantSubtypeToDiscriminator.containsKey(type)) {
                final String errorMessage = String.format("Type %s must be unique", type);
                LOGGER.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            tenantSubtypeToDiscriminator.put(type, discriminatorFieldValue);
        }
    }

    /**
     * Unregister mapping between a field value and an explicit type
     * @param tenant tenant
     * @param type type
     * @param discriminatorFieldValue field value
     */
    public void unregisterSubtype(String tenant, Class<?> type, String discriminatorFieldValue) {
        setRefreshMapping(tenant, true);
        GSONUtils.assertNotNull(type, "Sub type is required.");
        GSONUtils.assertNotNull(discriminatorFieldValue, "Discriminator field value is required.");

        LOGGER.debug("Subtype \"{}\" unmapped to \"{}\" value", type, discriminatorFieldValue);

        // Retrieve tenant map
        Map<String, Class<?>> tenantDiscriminatorToSubtype = getTenantDiscriminatorToSubtype(tenant);
        tenantDiscriminatorToSubtype.remove(discriminatorFieldValue);
        if (injectField) {
            // Retrieve tenant map
            Map<Class<?>, String> tenantSubtypeToDiscriminator = getTenantSubtypeToDiscriminator(tenant);
            tenantSubtypeToDiscriminator.remove(type);
        }
    }

    /**
     * Register a mapping between an enumeration and an explicit type.
     * @param type type
     * @param enumDiscriminatorValue enum value
     * @param tenant tenant
     */
    public void registerSubtype(String tenant, Class<?> type, Enum<?> enumDiscriminatorValue) {
        registerSubtype(tenant, type, enumDiscriminatorValue.toString());
    }

    public void registerSubtype(String tenant, Class<?> type) {
        registerSubtype(tenant, type, type.getCanonicalName());
    }

    protected void setRefreshMapping(String tenant, Boolean refreshMapping) {
        refreshMappingMap.put(tenant, refreshMapping);
    }

    protected void resetRefreshMapping() {
        for (String tenant : refreshMappingMap.keySet()) {
            refreshMappingMap.put(tenant, false);
        }
    }

    /**
     * @return whether the mapping should be refreshed for the given tenant
     */
    protected Boolean needRefreshMapping(String tenant) {
        if (!refreshMappingMap.containsKey(tenant)) {
            LOGGER.warn("Empty mapping for tenant {}", tenant);
            return Boolean.FALSE;
        }
        return refreshMappingMap.get(tenant);
    }

    protected Map<String, Class<?>> getTenantDiscriminatorToSubtype(String tenant) {
        Map<String, Class<?>> map = discToSubtypeMap.get(tenant);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            discToSubtypeMap.put(tenant, map);
        }
        return map;
    }

    protected Map<Class<?>, String> getTenantSubtypeToDiscriminator(String tenant) {
        Map<Class<?>, String> map = subtypeToDiscMap.get(tenant);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            subtypeToDiscMap.put(tenant, map);
        }
        return map;
    }

    /**
     * Store mappings
     * @param gson GSON
     * @param discriminatorAdapterMap mapping between discriminator value and adapter
     * @param subTypeAdapterMap mapping between sub type and adapter
     * @param tenant tenant
     */
    protected void doTenantMapping(Gson gson, Map<String, TypeAdapter<?>> discriminatorAdapterMap,
            Map<Class<?>, TypeAdapter<?>> subTypeAdapterMap, String tenant) {

        // Clear maps before computing delegation
        discriminatorAdapterMap.clear();
        subTypeAdapterMap.clear();

        Map<String, Class<?>> tenantDiscriminatorToSubtype = getTenantDiscriminatorToSubtype(tenant);
        // Register TypeAdapter delegation mapping from discriminator and type
        for (Map.Entry<String, Class<?>> mapping : tenantDiscriminatorToSubtype.entrySet()) {
            final TypeAdapter<?> delegate = gson.getDelegateAdapter(this, TypeToken.get(mapping.getValue()));
            discriminatorAdapterMap.put(mapping.getKey(), delegate);
            subTypeAdapterMap.put(mapping.getValue(), delegate);
        }
    }

    protected void doMapping(Gson gson, Map<String, Map<String, TypeAdapter<?>>> discriminatorAdapterMap,
            Map<String, Map<Class<?>, TypeAdapter<?>>> subTypeAdapterMap, String inTenant) {
        if (inTenant != null) {
            doTenantMapping(gson, getTenantDiscriminatorToDelegate(discriminatorAdapterMap, inTenant),
                            getTenantSubtypeToDelegate(subTypeAdapterMap, inTenant), inTenant);
        } else {
            for (String tenant : discToSubtypeMap.keySet()) {
                doTenantMapping(gson, getTenantDiscriminatorToDelegate(discriminatorAdapterMap, tenant),
                                getTenantSubtypeToDelegate(subTypeAdapterMap, tenant), tenant);
            }
        }
    }

    protected Map<String, TypeAdapter<?>> getTenantDiscriminatorToDelegate(
            Map<String, Map<String, TypeAdapter<?>>> discriminatorAdapterMap, String tenant) {
        Map<String, TypeAdapter<?>> map = discriminatorAdapterMap.get(tenant);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            discriminatorAdapterMap.put(tenant, map);
        }
        return map;
    }

    protected Map<Class<?>, TypeAdapter<?>> getTenantSubtypeToDelegate(
            Map<String, Map<Class<?>, TypeAdapter<?>>> subTypeAdapterMap, String tenant) {
        Map<Class<?>, TypeAdapter<?>> map = subTypeAdapterMap.get(tenant);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            subTypeAdapterMap.put(tenant, map);
        }
        return map;
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

            String discriminatorFieldValue = getTenantSubtypeToDiscriminator(runtimeTenantResolver.getTenant())
                    .get(subType);
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

    // The create() is called many types and return null for unmanaged types.
    // A factory is typically called once per type, but the returned type adapter may be used many times.
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        final Class<? super T> requestedType = type.getRawType();
        if (!baseType.isAssignableFrom(requestedType)) {
            return null;
        }

        return new TypeAdapter<T>() { // NOSONAR

            /**
             * Delegate writing to default type adapter
             */
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                String tenant = runtimeTenantResolver.getTenant();
                if (needRefreshMapping(tenant)) {
                    doMapping(gson, discToDelegateMap, subtypeToDelegateMap, tenant);
                    setRefreshMapping(tenant, false);
                }

                final Class<?> srcType = value.getClass();

                // registration requires that subtype extends base type
                TypeAdapter<T> delegate = getDelegate(srcType);

                if (delegate == null) {
                    String errorMessage = String
                            .format("Cannot serialize attribute '%s' of type %s. Did you forget to register a subtype ? (tenant = %s)",
                                    value, srcType.getName(), tenant);
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }

                // Raw JSON object
                JsonElement rawJson = delegate.toJsonTree(value);
                out.setSerializeNulls(true);
                Streams.write(beforeWrite(rawJson, srcType), out);
                out.setSerializeNulls(false);
            }

            @SuppressWarnings("unchecked")
            protected TypeAdapter<T> getDelegate(Class<?> type) {
                Map<Class<?>, TypeAdapter<?>> map = subtypeToDelegateMap.get(runtimeTenantResolver.getTenant());
                if (map != null) {
                    return (TypeAdapter<T>) map.get(type);
                }
                return null;
            }

            /**
             * Delegate reading to type adapter mapped to extracted discriminator value
             */
            @Override
            public T read(JsonReader in) {
                String tenant = runtimeTenantResolver.getTenant();
                if (needRefreshMapping(tenant)) {
                    doMapping(gson, discToDelegateMap, subtypeToDelegateMap, tenant);
                    setRefreshMapping(runtimeTenantResolver.getTenant(), false);
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
                TypeAdapter<T> delegate = getDelegate(discriminator);

                if (delegate == null) {
                    String errorMessage = String
                            .format("Cannot deserialize %s subtype named %s. Did you forget to register a subtype?",
                                    baseType, discriminator);
                    LOGGER.error(errorMessage);
                    throw new JsonParseException(errorMessage);
                }

                try {
                    return delegate
                            .fromJsonTree(beforeRead(jsonElement, discriminator,
                                                     getTenantDiscriminatorToSubtype(runtimeTenantResolver.getTenant())
                                                             .get(discriminator)));
                } catch (JsonIOException e) {
                    String errorMessage = String.format("Unexpected JSON format (%s)", jsonElement.toString());
                    LOGGER.error(errorMessage, e);
                    throw new JsonParseException(errorMessage);
                }
            }

            @SuppressWarnings("unchecked")
            protected TypeAdapter<T> getDelegate(String discriminatorFieldValue) {
                Map<String, TypeAdapter<?>> map = discToDelegateMap.get(runtimeTenantResolver.getTenant());
                if (map != null) {
                    return (TypeAdapter<T>) map.get(discriminatorFieldValue);
                }
                return null;
            }
        }.nullSafe();
    }
}
