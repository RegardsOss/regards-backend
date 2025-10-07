/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Entity representing a feature with the feature field serialized as a JSON String.
 *
 * @author Thibaud Michaudel
 * @see AbstractFeatureEntity for the version with the feature field deserialized
 **/
@MappedSuperclass
public abstract class AbstractFeatureRawEntity extends AbstractFeatureEntity {

    @Column(columnDefinition = "jsonb", name = "feature")
    protected String feature;

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

}
