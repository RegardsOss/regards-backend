/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * File description
 *
 * @author Marc SORDI
 */
public class FeatureFile {

    /**
     * File locations (a file can be stored at several locations)
     */
    @Valid
    @NotEmpty(message = "At least one location is required")
    private Set<FeatureFileLocation> locations = new HashSet<>();

    /**
     * File attributes
     */
    @Valid
    @NotNull(message = "File attributes is requred")
    private FeatureFileAttributes attributes;

    /**
     * Additional fields in JSON format
     */
    @Schema(hidden = true, description = "Additional fields in JSON format")
    private Object additionalFields;

    public static FeatureFile build(FeatureFileAttributes attributes,
                                    Object additionalFields,
                                    FeatureFileLocation... locations) {
        FeatureFile file = new FeatureFile();
        file.setAttributes(attributes);
        file.setLocations(Sets.newHashSet(locations));
        file.setAdditionalFields(additionalFields);
        return file;
    }

    private void setAdditionalFields(Object additionalFields) {
        this.additionalFields = additionalFields;
    }

    public Set<FeatureFileLocation> getLocations() {
        return locations;
    }

    public void setLocations(Set<FeatureFileLocation> locations) {
        this.locations = locations;
    }

    public FeatureFileAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(FeatureFileAttributes attributes) {
        this.attributes = attributes;
    }

    public Object getAdditionalFields() {
        return additionalFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureFile that = (FeatureFile) o;
        return Objects.equals(locations, that.locations)
               && Objects.equals(attributes, that.attributes)
               && Objects.equals(additionalFields, that.additionalFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locations, attributes, additionalFields);
    }

}
