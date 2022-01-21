/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import fr.cnes.regards.framework.geojson.AbstractFeatureCollection;

/**
 * Feature collection representation based on GeoJson standard structure.
 *
 * @author Kevin Marchois
 *
 */
public class FeatureUpdateCollection extends AbstractFeatureCollection<Feature> {

    @NotBlank(message = "Request owner is required")
    private String requestOwner;

    @Valid
    private FeatureMetadata metadata;

    /**
     * Create a new {@link FeatureUpdateCollection} <br/>
     * @param metadata {@link FeatureMetadata}
     * @param features collection of {@link Feature}
     * @return a {@link FeatureUpdateCollection}
     */
    public static FeatureUpdateCollection build(String requestOwner, FeatureMetadata metadata,
            Collection<Feature> features) {
        FeatureUpdateCollection collection = new FeatureUpdateCollection();
        collection.setMetadata(metadata);
        collection.addAll(features);
        collection.setRequestOwner(requestOwner);
        return collection;
    }

    public FeatureMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(FeatureMetadata metadata) {
        this.metadata = metadata;
    }

    public String getRequestOwner() {
        return requestOwner;
    }

    public void setRequestOwner(String requestOwner) {
        this.requestOwner = requestOwner;
    }

}
