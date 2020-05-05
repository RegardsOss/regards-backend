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
package fr.cnes.regards.modules.feature.dto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

/**
 * Feature collection representation based on GeoJson standard structure.
 *
 * @author Kevin Marchois
 *
 */
public class FeatureReferenceCollection {

    @Valid
    private FeatureCreationSessionMetadata metadata;

    private final Set<String> locations = new HashSet<>();

    private String pluginBusinessId;

    /**
     * Create a new {@link FeatureReferenceCollection} <br/>
     * @param metadata {@link FeatureCreationSessionMetadata}
     * @param locations collection of location of {@link Feature}
     * @param pluginBusinessId plugin id to create feature from location
     * @return a {@link FeatureReferenceCollection}
     */
    public static FeatureReferenceCollection build(FeatureCreationSessionMetadata metadata,
            Collection<String> locations, String pluginBusinessId) {
        FeatureReferenceCollection collection = new FeatureReferenceCollection();
        collection.setMetadata(metadata);
        collection.addAll(locations);
        collection.setPluginBusinessId(pluginBusinessId);
        return collection;
    }

    public void addAll(Collection<String> locations) {
        locations.addAll(locations);
    }

    public FeatureCreationSessionMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(FeatureCreationSessionMetadata metadata) {
        this.metadata = metadata;
    }

    public Set<String> getLocations() {
        return locations;
    }

    public String getPluginBusinessId() {
        return pluginBusinessId;
    }

    public void setPluginBusinessId(String pluginBusinessId) {
        this.pluginBusinessId = pluginBusinessId;
    }

}
