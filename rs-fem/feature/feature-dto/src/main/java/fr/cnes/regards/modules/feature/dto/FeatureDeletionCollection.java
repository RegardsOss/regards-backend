/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

/**
 * Feature collection representation based on GeoJson standard structure.
 *
 * @author Kevin Marchois
 *
 */
public class FeatureDeletionCollection {

    private final Set<FeatureUniformResourceName> featuresUrns = new HashSet<>();

    private PriorityLevel priority;

    /**
     * Create a new {@link FeatureDeletionCollection} <br/>
     * @param urns collection of {@link FeatureUniformResourceName}
     * @return a {@link FeatureDeletionCollection}
     */
    public static FeatureDeletionCollection build(Collection<FeatureUniformResourceName> urns, PriorityLevel priority) {
        FeatureDeletionCollection collection = new FeatureDeletionCollection();
        collection.addAll(urns);
        collection.setPriority(priority);
        return collection;
    }

    public Set<FeatureUniformResourceName> getFeaturesUrns() {
        return featuresUrns;
    }

    public void addAll(Collection<FeatureUniformResourceName> urns) {
        featuresUrns.addAll(urns);
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

}
