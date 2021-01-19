/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.dto.gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;
import fr.cnes.regards.modules.feature.dto.Feature;

/**
 *
 * Merge array of properties
 * @author Marc Sordi
 *
 */
@GsonTypeAdapterFactory
public class FeatureAdapterFactory extends PolymorphicTypeAdapterFactory<Feature> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureAdapterFactory.class);

    public FeatureAdapterFactory() {
        super(Feature.class, "type");
        registerSubtype(Feature.class, GeoJsonType.FEATURE.getType(), true);
    }

    @Override
    protected JsonElement beforeWrite(JsonElement jsonElement, Class<?> subType) {

        // Do injection
        JsonElement clone = super.beforeWrite(jsonElement, subType);

        LOGGER.trace("Before write");

        if (!clone.isJsonObject()) {
            throw objectRequiredException(clone);
        }

        JsonObject entity = clone.getAsJsonObject();
        FeatureProperties.beforeWrite(entity);
        return entity;
    }

    @Override
    protected JsonElement beforeRead(JsonElement jsonElement, String discriminator, Class<?> subType) {
        LOGGER.trace("Before read");

        if (!jsonElement.isJsonObject()) {
            throw objectRequiredException(jsonElement);
        }

        JsonObject entity = jsonElement.getAsJsonObject();
        FeatureProperties.beforeRead(entity);
        return entity;
    }

    private static IllegalArgumentException objectRequiredException(JsonElement jsonElement) {
        String errorMessage = String.format("Unexpected JSON element %s. Object required.", jsonElement.toString());
        LOGGER.error(errorMessage);
        return new IllegalArgumentException(errorMessage);
    }
}
