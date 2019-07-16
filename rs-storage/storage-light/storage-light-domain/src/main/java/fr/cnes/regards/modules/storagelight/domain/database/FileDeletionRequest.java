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
package fr.cnes.regards.modules.storagelight.domain.database;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.springframework.util.Assert;

import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;

/**
 * @author sbinda
 *
 */
@Entity
@Table(name = "t_file_deletion_request",
        indexes = { @Index(name = "idx_file_deletion_request", columnList = "storage") },
        uniqueConstraints = { @UniqueConstraint(columnNames = { "file_reference" }) })
public class FileDeletionRequest {

    @Id
    @SequenceGenerator(name = "fileDeletioneSequence", initialValue = 1, sequenceName = "seq_file_deletion")
    @GeneratedValue(generator = "fileDeletioneSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileRequestStatus status = FileRequestStatus.TODO;

    @Column(nullable = false, length = FileLocation.STORAGE_MAX_LENGTH)
    private String storage;

    @JoinColumn(name = "file_reference")
    @OneToOne
    @MapsId
    private FileReference fileReference;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    public FileDeletionRequest() {
        super();
    }

    public FileDeletionRequest(FileReference fileReference) {
        super();

        Assert.notNull(fileReference, "File reference to deleted cannot be null");
        Assert.notNull(fileReference.getLocation(), "Unable to delete a file with no location");
        Assert.notNull(fileReference.getLocation().getStorage(), "Unable to delete a file with no location storage.");
        this.fileReference = fileReference;
        this.storage = fileReference.getLocation().getStorage();
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the fileReference
     */
    public FileReference getFileReference() {
        return fileReference;
    }

    /**
     * @param fileReference the fileReference to set
     */
    public void setFileReference(FileReference fileReference) {
        this.fileReference = fileReference;
    }

    /**
     * @return the status
     */
    public FileRequestStatus getStatus() {
        return status;
    }

    /**
     * @return the storage
     */
    public String getStorage() {
        return storage;
    }

    /**
     * @param storage the storage to set
     */
    public void setStorage(String storage) {
        this.storage = storage;
    }

    /**
     * @return the errorCause
     */
    public String getErrorCause() {
        return errorCause;
    }

    /**
     * @param errorCause the errorCause to set
     */
    public void setErrorCause(String errorCause) {
        this.errorCause = errorCause;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(FileRequestStatus status) {
        this.status = status;
    }

}
