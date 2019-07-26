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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;

/**
 * @author sbinda
 *
 */
@Entity
@Table(name = "t_file_restoration_request")
public class FileRestorationRequest {

    /**
     * Internal database unique identifier
     */
    @Id
    @SequenceGenerator(name = "fileRestorationRequestSequence", initialValue = 1,
            sequenceName = "seq_file_restoration_request")
    @GeneratedValue(generator = "fileRestorationRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "file_ref_id")
    private FileReference fileReference;

    @Column(name = "origin_storage", length = FileLocation.STORAGE_MAX_LENGTH)
    private String originStorage;

    @Column(name = "file_destination_path")
    private String fileDestinationPath;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileRequestStatus status = FileRequestStatus.TODO;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    public FileRestorationRequest(FileReference fileReference, String fileDestinationPath) {
        super();
        this.fileReference = fileReference;
        this.originStorage = fileReference.getLocation().getStorage();
        this.fileDestinationPath = fileDestinationPath;
    }

    public FileRestorationRequest() {
        super();
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

    public String getFileDestinationPath() {
        return fileDestinationPath;
    }

    public void setFileDestinationPath(String fileDestinationPath) {
        this.fileDestinationPath = fileDestinationPath;
    }

    public String getOriginStorage() {
        return originStorage;
    }

    public void setOriginStorage(String originStorage) {
        this.originStorage = originStorage;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public void setErrorCause(String errorCause) {
        this.errorCause = errorCause;
    }

    public FileRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FileRequestStatus status) {
        this.status = status;
    }

}
