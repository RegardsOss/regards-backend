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
package fr.cnes.regards.framework.gson.adapters.sample6;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 * @author Marc Sordi
 */
@SuppressWarnings("rawtypes")
public class CustomPolymorphicTypeAdapterFactory extends PolymorphicTypeAdapterFactory<AbstractProperty> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomPolymorphicTypeAdapterFactory.class);

    /**
     * Discriminator field
     */
    private static final String DISCRIMINATOR_FIELD_NAME = "name";

    /**
     * Discriminator field
     */
    private static final String VALUE_FIELD = "value";

    /**
     * Namespace separator
     */
    private static final String NS_SEPARATOR = ".";

    /**
     * Regexp escape character
     */
    private static final String REGEXP_ESCAPE = "\\";

    protected CustomPolymorphicTypeAdapterFactory() {
        super(AbstractProperty.class, DISCRIMINATOR_FIELD_NAME, false);
        registerSubtype(DateProperty.class, "date");
        registerSubtype(StringProperty.class, AdapterTest.SAMPLE_ATT);
        registerSubtype(ObjectProperty.class, "GEO");
        registerSubtype(StringProperty.class, "GEO.CRS");
        registerSubtype(DateProperty.class, "GEO." + AdapterTest.SAMPLE_ATT);
        registerSubtype(ObjectProperty.class, "CONTACT");
        registerSubtype(StringProperty.class, "CONTACT.phone");
    }

    @Override
    protected JsonElement beforeRead(JsonElement jsonElement, String discriminator, Class<?> subType) {
        if (subType == ObjectProperty.class) {
            addNamespaceToChildren(jsonElement, discriminator);
        }
        removeParentNamespace(jsonElement);
        return jsonElement;
    }

    /**
     * Add namespace to {@link JsonElement} children of ObjectAttribute
     * @param inJsonElement {@link JsonElement}
     * @param discriminator discriminator value
     */
    protected void addNamespaceToChildren(JsonElement inJsonElement, String discriminator) {

        if (inJsonElement.isJsonObject()) {
            final JsonElement children = inJsonElement.getAsJsonObject().get(VALUE_FIELD);

            if (children == null) {
                throw missingFieldException(inJsonElement, VALUE_FIELD);
            }

            if (children.isJsonArray()) {
                for (JsonElement jsonElement : children.getAsJsonArray()) {
                    addNamespaceToChild(jsonElement, discriminator);
                }
            } else {
                throw objectRequiredException(inJsonElement);
            }
        } else {
            throw objectRequiredException(inJsonElement);
        }
    }

    /**
     * Add namespace to {@link JsonElement} child
     * @param jsonElement {@link JsonElement}
     * @param discriminator discriminator value
     */
    protected void addNamespaceToChild(JsonElement jsonElement, String discriminator) {

        if (jsonElement.isJsonObject()) {

            // Backup for logging
            String logOriginal = jsonElement.toString();
            JsonObject o = jsonElement.getAsJsonObject();

            JsonElement originalElement = o.get(DISCRIMINATOR_FIELD_NAME);
            if (originalElement == null) {
                throw missingFieldException(jsonElement, DISCRIMINATOR_FIELD_NAME);
            }

            // Compute and inject name with its namespace
            String originalName = originalElement.getAsString();
            String nsName = discriminator.concat(NS_SEPARATOR).concat(originalName);
            o.add(DISCRIMINATOR_FIELD_NAME, new JsonPrimitive(nsName));

            LOGGER.debug(String.format("Namespace added : \"%s\" -> \"%s\"", logOriginal, jsonElement.toString()));
        } else {
            throw objectRequiredException(jsonElement);
        }
    }

    /**
     * Remove namespace from {@link JsonElement}
     * @param jsonElement target {@link JsonElement}
     */
    protected void removeParentNamespace(JsonElement jsonElement) {

        if (jsonElement.isJsonObject()) {

            // Backup for logging
            String logOriginal = jsonElement.toString();

            final JsonObject o = jsonElement.getAsJsonObject();
            final JsonElement nsElement = o.get(DISCRIMINATOR_FIELD_NAME);
            if (nsElement == null) {
                throw missingFieldException(jsonElement, DISCRIMINATOR_FIELD_NAME);
            }

            // Compute and inject name without its namespace
            String nsName = nsElement.getAsString();
            String[] splitNsName = nsName.split(REGEXP_ESCAPE + NS_SEPARATOR);
            o.add(DISCRIMINATOR_FIELD_NAME, new JsonPrimitive(splitNsName[splitNsName.length - 1]));

            if (LOGGER.isDebugEnabled()) {
                if (splitNsName.length > 1) {
                    LOGGER.debug(String.format("Namespace removed : \"%s\" -> \"%s\"", logOriginal,
                                               jsonElement.toString()));
                } else {
                    LOGGER.debug(String.format("No namespace to remove : \"%s\" -> \"%s\"", logOriginal,
                                               jsonElement.toString()));
                }
            }
        } else {
            throw objectRequiredException(jsonElement);
        }
    }

    private IllegalArgumentException objectRequiredException(JsonElement jsonElement) {
        String errorMessage = String.format("Unexpected JSON element %s. Object required.", jsonElement.toString());
        LOGGER.error(errorMessage);
        return new IllegalArgumentException(errorMessage);
    }

    private IllegalArgumentException missingFieldException(JsonElement jsonElement, String fieldName) {
        String errorMessage = String.format("JSON element %s must contains a %s field", jsonElement.toString(),
                                            fieldName);
        LOGGER.error(errorMessage);
        return new IllegalArgumentException(errorMessage);
    }
}
