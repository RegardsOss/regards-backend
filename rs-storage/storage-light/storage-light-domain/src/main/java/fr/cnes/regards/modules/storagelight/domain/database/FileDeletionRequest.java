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
import javax.persistence.ForeignKey;
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
        uniqueConstraints = { @UniqueConstraint(name = "uk_t_file_deletion_request_file_reference",
                columnNames = { "file_reference" }) })
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

    @JoinColumn(name = "file_reference", foreignKey = @ForeignKey(name = "fk_t_file_deletion_request_t_file_reference"))
    @OneToOne
    @MapsId
    private FileReference fileReference;

    @Column(name = "force_delete")
    private boolean forceDelete = false;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    public FileDeletionRequest() {
        super();
    }

    public FileDeletionRequest(FileReference fileReference) {
        super();

        Assert.notNull(fileReference, "File reference to delete cannot be null");
        Assert.notNull(fileReference.getId(), "File reference to delete identifier cannot be null");
        Assert.notNull(fileReference.getLocation(), "Unable to delete a file with no location");
        Assert.notNull(fileReference.getLocation().getStorage(), "Unable to delete a file with no location storage.");
        this.fileReference = fileReference;
        this.storage = fileReference.getLocation().getStorage();
    }

    public FileDeletionRequest(FileReference fileReference, boolean forceDelete) {
        this(fileReference);
        this.forceDelete = forceDelete;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FileReference getFileReference() {
        return fileReference;
    }

    public void setFileReference(FileReference fileReference) {
        this.fileReference = fileReference;
    }

    public FileRequestStatus getStatus() {
        return status;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public void setErrorCause(String errorCause) {
        this.errorCause = errorCause;
    }

    public void setStatus(FileRequestStatus status) {
        this.status = status;
    }

    public boolean isForceDelete() {
        return forceDelete;
    }

    public void setForceDelete(boolean forceDelete) {
        this.forceDelete = forceDelete;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FileDeletionRequest other = (FileDeletionRequest) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

}
