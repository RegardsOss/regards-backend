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
package fr.cnes.regards.modules.acquisition.domain.chain;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Storage information
 * @author Léo Mieulet
 */
public class StorageMetadataProvider {

    private static final int STORAGE_MAX_LENGTH = 128;

    private static final int URL_MAX_LENGTH = 2048;

    private static final String MISSING_STORAGE = "Destination location cannot be null";

    @NotBlank(message = StorageMetadataProvider.MISSING_STORAGE)
    @Size(min=1, max= STORAGE_MAX_LENGTH)
    private String storage;

    @Size(min=1, max= URL_MAX_LENGTH)
    private String storageSubDirectory;

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getStorageSubDirectory() {
        return storageSubDirectory;
    }

    public void setStorageSubDirectory(String storageSubDirectory) {
        this.storageSubDirectory = storageSubDirectory;
    }

    public static StorageMetadataProvider build(String storage, String storageSubDirectory) {
        StorageMetadataProvider storageMetadata = new StorageMetadataProvider();
        storageMetadata.setStorage(storage);
        storageMetadata.setStorageSubDirectory(storageSubDirectory);
        return storageMetadata;
    }
}
