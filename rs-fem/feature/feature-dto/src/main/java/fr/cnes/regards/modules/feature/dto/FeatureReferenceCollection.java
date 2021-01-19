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
package fr.cnes.regards.modules.feature.dto;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.google.gson.JsonObject;

/**
 * Feature collection representation based on GeoJson standard structure.
 *
 * @author Kevin Marchois
 *
 */
public class FeatureReferenceCollection {

    @Valid
    @NotNull(message = "Request metadata is required")
    private FeatureCreationSessionMetadata metadata;

    @NotBlank(message = "Extraction factory identified by a plugin business identifier is required")
    private String factory;

    /**
     * Free parameters that target factory must understand
     */
    @NotEmpty(message = "Extraction parameters must not be empty")
    private Set<JsonObject> parameters;

    /**
     * Create a new {@link FeatureReferenceCollection} <br/>
     * @param metadata {@link FeatureCreationSessionMetadata}
     * @param factory extraction plugin id to create feature from specified parameters
     * @param parameters extraction parameters
     * @return a {@link FeatureReferenceCollection}
     */
    public static FeatureReferenceCollection build(FeatureCreationSessionMetadata metadata, String factory,
            Set<JsonObject> parameters) {
        FeatureReferenceCollection collection = new FeatureReferenceCollection();
        collection.setMetadata(metadata);
        collection.setFactory(factory);
        collection.setParameters(parameters);
        return collection;
    }

    public FeatureCreationSessionMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(FeatureCreationSessionMetadata metadata) {
        this.metadata = metadata;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public Set<JsonObject> getParameters() {
        return parameters;
    }

    public void setParameters(Set<JsonObject> parameters) {
        this.parameters = parameters;
    }

}
