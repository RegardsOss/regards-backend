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
package fr.cnes.regards.modules.acquisition.domain.chain;

import java.util.Set;

import javax.persistence.Column;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.urn.DataType;

/**
 * Storage information
 * @author Léo Mieulet
 */
public class StorageMetadataProvider {

    private static final int STORAGE_MAX_LENGTH = 128;

    private static final int URL_MAX_LENGTH = 2048;

    private static final String MISSING_STORAGE = "Destination location cannot be null";

    private static final String MISSING_TARGET_TYPES = "Data type list should be provided";

    /**
     * Storage identifier.
     * To use a plugin from storage, this identifier must match a plugin configuration business identifier.
     */
    @NotBlank(message = StorageMetadataProvider.MISSING_STORAGE)
    @Size(min = 1, max = STORAGE_MAX_LENGTH)
    private String pluginBusinessId;

    /**
     * Optional path identifying the base directory in which to store related files
     */
    @Size(max = URL_MAX_LENGTH)
    private String storePath;

    /**
     * List of data object types accepted by this storage location (when storing AIPs)
     */
    @Valid
    @NotNull(message = MISSING_TARGET_TYPES)
    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<DataType> targetTypes;

    public String getPluginBusinessId() {
        return pluginBusinessId;
    }

    public void setPluginBusinessId(String pluginBusinessId) {
        this.pluginBusinessId = pluginBusinessId;
    }

    public String getStorePath() {
        return storePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    public Set<DataType> getTargetTypes() {
        return targetTypes;
    }

    public void setTargetTypes(Set<DataType> targetTypes) {
        this.targetTypes = targetTypes;
    }

    public static StorageMetadataProvider build(String storage, String storageSubDirectory, Set<DataType> targetTypes) {
        StorageMetadataProvider storageMetadata = new StorageMetadataProvider();
        storageMetadata.setPluginBusinessId(storage);
        storageMetadata.setStorePath(storageSubDirectory);
        storageMetadata.setTargetTypes(targetTypes);
        return storageMetadata;
    }
}
