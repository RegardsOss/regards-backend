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

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
    protected JsonElement beforeRead(JsonElement pJsonElement, String pDiscriminator, Class<?> pSubType) {
        if (pSubType == ObjectProperty.class) {
            addNamespaceToChildren(pJsonElement, pDiscriminator);
        }
        removeParentNamespace(pJsonElement);
        return pJsonElement;
    }

    /**
     * Add namespace to {@link JsonElement} children of {@link ObjectAttribute}
     *
     * @param pJsonElement
     *            {@link JsonElement}
     * @param pDiscriminator
     *            discriminator value
     */
    protected void addNamespaceToChildren(JsonElement pJsonElement, String pDiscriminator) {

        if (pJsonElement.isJsonObject()) {
            final JsonElement children = pJsonElement.getAsJsonObject().get(VALUE_FIELD);

            if (children == null) {
                throw missingFieldException(pJsonElement, VALUE_FIELD);
            }

            if (children.isJsonArray()) {
                final Iterator<JsonElement> childrenIter = children.getAsJsonArray().iterator();
                while (childrenIter.hasNext()) {
                    addNamespaceToChild(childrenIter.next(), pDiscriminator);
                }
            } else {
                throw objectRequiredException(pJsonElement);
            }
        } else {
            throw objectRequiredException(pJsonElement);
        }
    }

    /**
     * Add namespace to {@link JsonElement} child
     *
     * @param pJsonElement
     *            {@link JsonElement}
     * @param pDiscriminator
     *            discriminator value
     */
    protected void addNamespaceToChild(JsonElement pJsonElement, String pDiscriminator) {

        if (pJsonElement.isJsonObject()) {

            // Backup for logging
            String logOriginal = pJsonElement.toString();
            JsonObject o = pJsonElement.getAsJsonObject();

            JsonElement originalElement = o.get(DISCRIMINATOR_FIELD_NAME);
            if (originalElement == null) {
                throw missingFieldException(pJsonElement, DISCRIMINATOR_FIELD_NAME);
            }

            // Compute and inject name with its namespace
            String originalName = originalElement.getAsString();
            String nsName = pDiscriminator.concat(NS_SEPARATOR).concat(originalName);
            o.add(DISCRIMINATOR_FIELD_NAME, new JsonPrimitive(nsName));

            LOGGER.debug(String.format("Namespace added : \"%s\" -> \"%s\"", logOriginal, pJsonElement.toString()));
        } else {
            throw objectRequiredException(pJsonElement);
        }
    }

    /**
     * Remove namespace from {@link JsonElement}
     *
     * @param pJsonElement
     *            target {@link JsonElement}
     */
    protected void removeParentNamespace(JsonElement pJsonElement) {

        if (pJsonElement.isJsonObject()) {

            // Backup for logging
            String logOriginal = pJsonElement.toString();

            final JsonObject o = pJsonElement.getAsJsonObject();
            final JsonElement nsElement = o.get(DISCRIMINATOR_FIELD_NAME);
            if (nsElement == null) {
                throw missingFieldException(pJsonElement, DISCRIMINATOR_FIELD_NAME);
            }

            // Compute and inject name without its namespace
            String nsName = nsElement.getAsString();
            String[] splitNsName = nsName.split(REGEXP_ESCAPE + NS_SEPARATOR);
            o.add(DISCRIMINATOR_FIELD_NAME, new JsonPrimitive(splitNsName[splitNsName.length - 1]));

            if (LOGGER.isDebugEnabled()) {
                if (splitNsName.length > 1) {
                    LOGGER.debug(String.format("Namespace removed : \"%s\" -> \"%s\"", logOriginal,
                                               pJsonElement.toString()));
                } else {
                    LOGGER.debug(String.format("No namespace to remove : \"%s\" -> \"%s\"", logOriginal,
                                               pJsonElement.toString()));
                }
            }
        } else {
            throw objectRequiredException(pJsonElement);
        }
    }

    private IllegalArgumentException objectRequiredException(JsonElement pJsonElement) {
        String errorMessage = String.format("Unexpected JSON element %s. Object required.", pJsonElement.toString());
        LOGGER.error(errorMessage);
        return new IllegalArgumentException(errorMessage);
    }

    private IllegalArgumentException missingFieldException(JsonElement pJsonElement, String pFieldName) {
        String errorMessage = String.format("JSON element %s must contains a %s field", pJsonElement.toString(),
                                            pFieldName);
        LOGGER.error(errorMessage);
        return new IllegalArgumentException(errorMessage);
    }
}
