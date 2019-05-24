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
package fr.cnes.regards.modules.storage.domain;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Class PartialDeletionReport
 *
 * @author sbinda
 */
public class PartialDeletionReport {

    private final long dataStorageId;

    /**
     * Number of files successfully scheduled for deletion
     */
    private long nbFilesToDelete = 0L;

    /**
     * Number of files not scheduled for deletion because it is forbidden to delete  file from all storages.
     */
    private long nbFilesWithOnlyOneStorage = 0L;

    /**
     * Number of files not scheduled for deletion because there are not stored on the storage.
     */
    private long nbFilesNotHandled = 0L;

    /**
     * Number of files not scheduled for deletion because it is the last ONLINE storage for a ONLINE mandatory file.
     */
    private long nbFilesNeededOnline = 0L;

    /**
     * Does the partial delete job has been scheduled ?
     */
    private boolean deletionScheduled = true;

    private final Set<String> deletionErrorCauses = Sets.newHashSet();

    public PartialDeletionReport(long dataStorageId) {
        super();
        this.dataStorageId = dataStorageId;
    }

    public void addFileNotHandled() {
        this.nbFilesNotHandled++;
    }

    public void addFileNeededOnline() {
        this.nbFilesNeededOnline++;
    }

    public void addFileWithOnlyOneStorage() {
        this.nbFilesWithOnlyOneStorage++;
    }

    public void addFileToDelete() {
        this.nbFilesToDelete++;
    }

    public long getNbFilesToDelete() {
        return nbFilesToDelete;
    }

    public void setNbFilesToDelete(long nbFilesToDelete) {
        this.nbFilesToDelete = nbFilesToDelete;
    }

    public long getNbFilesWithOnlyOneStorage() {
        return nbFilesWithOnlyOneStorage;
    }

    public void setNbFilesWithOnlyOneStorage(long nbFilesWithOnlyOneStorage) {
        this.nbFilesWithOnlyOneStorage = nbFilesWithOnlyOneStorage;
    }

    public long getNbFilesNotHandled() {
        return nbFilesNotHandled;
    }

    public void setNbFilesNotHandled(long nbFilesNotHandled) {
        this.nbFilesNotHandled = nbFilesNotHandled;
    }

    public long getNbFilesNeededOnline() {
        return nbFilesNeededOnline;
    }

    public void setNbFilesNeededOnline(long nbFilesNeededOnline) {
        this.nbFilesNeededOnline = nbFilesNeededOnline;
    }

    public long getNbFilesNotScheduled() {
        return this.nbFilesNeededOnline + this.nbFilesWithOnlyOneStorage + this.nbFilesNotHandled;
    }

    public long getDataStorageId() {
        return dataStorageId;
    }

    public boolean isDeletionScheduled() {
        return deletionScheduled;
    }

    public void setDeletionScheduled(boolean deletionScheduled) {
        this.deletionScheduled = deletionScheduled;
    }

    public Set<String> getDeletionErrorCauses() {
        return deletionErrorCauses;
    }

    public void addDeletionErrorCause(String deletionErrorCause) {
        this.deletionErrorCauses.add(deletionErrorCause);
    }

    public void append(PartialDeletionReport report) {
        if (report != null) {
            this.nbFilesNeededOnline += report.nbFilesNeededOnline;
            this.nbFilesNotHandled += report.nbFilesNotHandled;
            this.nbFilesToDelete += report.nbFilesToDelete;
            this.nbFilesWithOnlyOneStorage += report.nbFilesWithOnlyOneStorage;
            this.deletionScheduled |= report.deletionScheduled;
            this.deletionErrorCauses.addAll(report.deletionErrorCauses);
        }
    }

}
