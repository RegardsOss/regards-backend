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
package fr.cnes.regards.modules.filecatalog.dto;

import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;

/**
 * @author sbinda
 */
public class StorageLocationDto {

    /**
     * Configuration Dto
     */
    private StorageLocationConfigurationDto configuration;

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
    //FIXME: to delete when rs-storage microservice migration will be completed
    private boolean copyRunning = false;

    /**
     * Indicates if a pending action job is running fot this storage location. Pending Action Job is a job used to run
     * specific asynchronous actions of storage locations.
     */
    private boolean pendingActionRunning = false;

    /**
     * Indicates if the location needs to run an asynchronous remaining action to perform actions on stored files.
     */
    private boolean pendingActionRemaining = false;

    /**
     * Does this location allows to physically delete files ?
     */
    private boolean allowsPhysicalDeletion = false;

    public static StorageLocationDto build(String name, StorageLocationConfigurationDto configuration) {
        StorageLocationDto dto = new StorageLocationDto();
        dto.name = name;
        dto.configuration = configuration;
        return dto;
    }

    public StorageLocationDto withErrorInformation(long nbStorageError, long nbDeletionError) {
        this.nbDeletionError = nbDeletionError;
        this.nbStorageError = nbStorageError;
        return this;
    }

    public StorageLocationDto withFilesInformation(long nbFilesStored,
                                                   long nbFilesStoredWithPendingActionRemaining,
                                                   long totalStoredFilesSizeKo) {
        this.nbFilesStored = nbFilesStored;
        this.nbFilesStoredWithPendingActionRemaining = nbFilesStoredWithPendingActionRemaining;
        this.totalStoredFilesSizeKo = totalStoredFilesSizeKo;
        return this;
    }

    public StorageLocationDto withPendingActionRemaining(boolean pendingActionRemaining) {
        this.pendingActionRemaining = pendingActionRemaining;
        return this;
    }

    public StorageLocationDto withAllowPhysicalDeletion() {
        this.allowsPhysicalDeletion = true;
        return this;
    }

    public StorageLocationDto withAllowPhysicalDeletion(boolean allowsPhysicalDeletion) {
        this.allowsPhysicalDeletion = allowsPhysicalDeletion;
        return this;
    }

    public StorageLocationDto withRunningProcessesInformation(boolean storageRunning,
                                                              boolean deletionRunning,
                                                              boolean copyRunning,
                                                              boolean pendingActionRunning) {
        this.storageRunning = storageRunning;
        this.deletionRunning = deletionRunning;
        this.copyRunning = copyRunning;
        this.pendingActionRunning = pendingActionRunning;
        return this;
    }

    public StorageLocationDto withRunningProcessesInformation(boolean storageRunning,
                                                              boolean deletionRunning,
                                                              boolean pendingActionRunning) {
        this.storageRunning = storageRunning;
        this.deletionRunning = deletionRunning;
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

    public StorageLocationConfigurationDto getConfiguration() {
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

    public boolean isPendingActionRemaining() {
        return pendingActionRemaining;
    }

    @Override
    public String toString() {
        return "StorageLocationDto{"
               + "configuration="
               + configuration
               + ", name='"
               + name
               + '\''
               + ", nbFilesStored="
               + nbFilesStored
               + ", nbDeletionError="
               + nbDeletionError
               + ", nbStorageError="
               + nbStorageError
               + ", nbFilesStoredWithPendingActionRemaining="
               + nbFilesStoredWithPendingActionRemaining
               + ", totalStoredFilesSizeKo="
               + totalStoredFilesSizeKo
               + ", storageRunning="
               + storageRunning
               + ", deletionRunning="
               + deletionRunning
               + ", copyRunning="
               + copyRunning
               + ", pendingActionRunning="
               + pendingActionRunning
               + ", pendingActionRemaining="
               + pendingActionRemaining
               + ", allowsPhysicalDeletion="
               + allowsPhysicalDeletion
               + '}';
    }
}
