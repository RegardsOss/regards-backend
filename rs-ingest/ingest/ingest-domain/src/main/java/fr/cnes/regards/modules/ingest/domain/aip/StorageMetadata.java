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
package fr.cnes.regards.modules.ingest.domain.aip;

import javax.validation.constraints.NotBlank;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Metadata for storage
 *
 * @author Marc SORDI
 *
 */
public class StorageMetadata {

    private static final String MISSING_STORAGE_ERROR = "Storage identifier is required";

    /**
     * Storage identifier.
     * To use a plugin from storage, this identifier must match a plugin configuration business identifier.
     */
    @NotBlank(message = MISSING_STORAGE_ERROR)
    private String storage;

    /**
     * Optional path identifying the base directory in which to store related files
     */
    private String storePath;

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getStorePath() {
        return storePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    /**
     * Build storage metadata
     * @param storage storage identifier
     * @param storePath storage path
     */
    public static StorageMetadata build(String storage, @Nullable String storePath) {
        Assert.hasLength(storage, MISSING_STORAGE_ERROR);
        StorageMetadata m = new StorageMetadata();
        m.setStorage(storage);
        m.setStorePath(storePath);
        return m;
    }

}
