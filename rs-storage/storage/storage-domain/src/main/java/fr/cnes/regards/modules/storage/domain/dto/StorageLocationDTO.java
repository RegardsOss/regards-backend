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
package fr.cnes.regards.modules.storage.domain.dto;

import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;

/**
 * @author sbinda
 */
public class StorageLocationDTO {

    private StorageLocationConfiguration configuration;

    private String name;

    /**
     * Number of files stored into this storage location
     */
    private long nbFilesStored = 0L;

    /**
     * Number of deletion requests in error for this storage location
     */
    private long nbDeletionError = 0L;

    /**
     * Number of storage requests in error for this storage location
     */
    private long nbStorageError = 0L;

    /**
     * Number of files stored into this storage location witch a remaining pending action to do to finalise storage.
     */
    private long nbFilesStoredWithPendingActionRemaining = 0L;

    private long totalStoredFilesSizeKo = 0L;

    /**
     * Indicates if at least one storage request  associated to this storage location is running
     */
    private boolean storageRunning = false;

    /**
     * Indicates if at least one deletion request  associated to this storage location is running
     */
    private boolean deletionRunning = false;

    /**
     * Indicates if at least one copy request  associated to this storage location is running
     */
    private boolean copyRunning = false;

    /**
     * Indicates if a pending action job is running fot this storage location. Pending Action Job is a job used to run
     * specific asynchronous actions of storage locations.
     */
    private boolean pendingActionRunning = false;

    /**
     * Does this location allows to physically delete files ?
     */
    private boolean allowsPhysicalDeletion = false;

    public static StorageLocationDTO build(String name, StorageLocationConfiguration configuration) {
        StorageLocationDTO dto = new StorageLocationDTO();
        dto.name = name;
        dto.configuration = configuration;
        return dto;
    }

    public StorageLocationDTO withErrorInformation(long nbStorageError, long nbDeletionError) {
        this.nbDeletionError = nbDeletionError;
        this.nbStorageError = nbStorageError;
        return this;
    }

    public StorageLocationDTO withFilesInformation(long nbFilesStored,
                                                   long nbFilesStoredWithPendingActionRemaining,
                                                   long totalStoredFilesSizeKo) {
        this.nbFilesStored = nbFilesStored;
        this.nbFilesStoredWithPendingActionRemaining = nbFilesStoredWithPendingActionRemaining;
        this.totalStoredFilesSizeKo = totalStoredFilesSizeKo;
        return this;
    }

    public StorageLocationDTO withAllowPhysicalDeletion() {
        this.allowsPhysicalDeletion = true;
        return this;
    }

    public StorageLocationDTO withAllowPhysicalDeletion(boolean allowsPhysicalDeletion) {
        this.allowsPhysicalDeletion = allowsPhysicalDeletion;
        return this;
    }

    public StorageLocationDTO withRunningProcessesInformation(boolean storageRunning,
                                                              boolean deletionRunning,
                                                              boolean copyRunning,
                                                              boolean pendingActionRunning) {
        this.storageRunning = storageRunning;
        this.deletionRunning = deletionRunning;
        this.copyRunning = copyRunning;
        this.pendingActionRunning = pendingActionRunning;
        return this;
    }

    public String getName() {
        return name;
    }

    public long getNbFilesStored() {
        return nbFilesStored;
    }

    public long getTotalStoredFilesSizeKo() {
        return totalStoredFilesSizeKo;
    }

    public long getNbStorageError() {
        return nbStorageError;
    }

    public long getNbDeletionError() {
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

    public boolean isPendingActionRunning() {
        return pendingActionRunning;
    }

    public boolean isAllowsPhysicalDeletion() {
        return allowsPhysicalDeletion;
    }

    public long getNbFilesStoredWithPendingActionRemaining() {
        return nbFilesStoredWithPendingActionRemaining;
    }
}
