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
package fr.cnes.regards.modules.storage.domain.dto;

import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;

/**
 * @author sbinda
 *
 */
public class StorageLocationDTO {

    private String name;

    private Long nbFilesStored;

    private Long totalStoredFilesSizeKo;

    private Long nbStorageError;

    private Long nbDeletionError;

    private boolean storageRunning = false;

    private boolean deletionRunning = false;

    private boolean copyRunning = false;

    private boolean allowsPhysicalDeletion = false;

    private StorageLocationConfiguration configuration;

    public StorageLocationDTO(String name, Long nbFilesStored, Long totalStoredFilesSizeKo, Long nbStorageError,
            Long nbDeletionError, boolean storageRunning, boolean deletionRunning, boolean copyRunning,
            StorageLocationConfiguration configuration, boolean allowPhysicalDeletion) {
        this.name = name;
        this.nbFilesStored = nbFilesStored;
        this.totalStoredFilesSizeKo = totalStoredFilesSizeKo;
        this.nbStorageError = nbStorageError;
        this.nbDeletionError = nbDeletionError;
        this.storageRunning = storageRunning;
        this.deletionRunning = deletionRunning;
        this.copyRunning = copyRunning;
        this.configuration = configuration;
        this.allowsPhysicalDeletion = allowPhysicalDeletion;
    }

    public String getName() {
        return name;
    }

    public Long getNbFilesStored() {
        return nbFilesStored;
    }

    public Long getTotalStoredFilesSizeKo() {
        return totalStoredFilesSizeKo;
    }

    public Long getNbStorageError() {
        return nbStorageError;
    }

    public Long getNbDeletionError() {
        return nbDeletionError;
    }

    public StorageLocationConfiguration getConfiguration() {
        return configuration;
    }

    public boolean isStorageRunning() {
        return storageRunning;
    }

    public boolean isDeletionRunning() {
        return deletionRunning;
    }

    public boolean isCopyRunning() {
        return copyRunning;
    }

    public boolean isAllowsPhysicalDeletion() {
        return allowsPhysicalDeletion;
    }
}
