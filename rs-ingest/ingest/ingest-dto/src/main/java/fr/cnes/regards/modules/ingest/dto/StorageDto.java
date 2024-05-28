/*

 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.ingest.dto;

import fr.cnes.regards.framework.urn.DataType;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

/**
 * Storage metadata for ingest requests
 *
 * @author mnguyen0
 */
public class StorageDto {

    private static final String MISSING_STORAGE_ERROR = "Storage identifier is required";

    /**
     * Storage identifier.
     * To use a plugin from storage, this identifier must match a plugin configuration business identifier.
     */
    @NotBlank(message = MISSING_STORAGE_ERROR)
    private String pluginBusinessId;

    /**
     * Optional path identifying the base directory in which to store related files
     */
    private String storePath;

    /**
     * List of data object types accepted by this storage location (when storing AIPs)
     */
    private Set<DataType> targetTypes = new HashSet<>();

    @Nullable
    private StorageSize size;

    public StorageDto() {
    }

    public StorageDto(String pluginBusinessId) {
        this.pluginBusinessId = pluginBusinessId;
        this.targetTypes = new HashSet<>();
    }

    public StorageDto(String pluginBusinessId, String storePath, Set<DataType> targetTypes) {
        this.pluginBusinessId = pluginBusinessId;
        this.storePath = storePath;
        this.targetTypes = targetTypes;
    }

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

    @Nullable
    public StorageSize getSize() {
        return size;
    }

    public void setSize(@Nullable StorageSize size) {
        this.size = size;
    }
}
