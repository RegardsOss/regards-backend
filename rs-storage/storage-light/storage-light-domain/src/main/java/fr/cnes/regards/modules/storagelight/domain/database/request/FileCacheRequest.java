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
package fr.cnes.regards.modules.storagelight.domain.database.request;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
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
import javax.persistence.UniqueConstraint;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;

/**
 * @author sbinda
 *
 */
@Entity
@Table(name = "t_file_cache_request", uniqueConstraints = {
        @UniqueConstraint(name = "uk_t_file_cache_request_checksum", columnNames = { "checksum" }) })
public class FileCacheRequest {

    @Id
    @SequenceGenerator(name = "fileCacheRequestSequence", initialValue = 1, sequenceName = "seq_file_cache_request")
    @GeneratedValue(generator = "fileCacheRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "file_ref_id", nullable = false)
    private FileReference fileReference;

    @Column(name = "checksum", length = FileReferenceMetaInfo.CHECKSUM_MAX_LENGTH, nullable = false)
    private String checksum;

    @Column(name = "storage", length = FileLocation.STORAGE_MAX_LENGTH, nullable = false)
    private String storage;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "destination_path", length = FileLocation.URL_MAX_LENGTH, nullable = false)
    private String destinationPath;

    @Column(name = "expiration_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime expirationDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileRequestStatus status = FileRequestStatus.TODO;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    public FileCacheRequest(FileReference fileReference, String destinationPath, OffsetDateTime expirationDate) {
        super();
        this.fileReference = fileReference;
        this.storage = fileReference.getLocation().getStorage();
        this.fileSize = fileReference.getMetaInfo().getFileSize();
        this.checksum = fileReference.getMetaInfo().getChecksum();
        this.destinationPath = destinationPath;
        this.expirationDate = expirationDate;
    }

    public FileCacheRequest() {
        super();
    }

    public Long getId() {
        return id;
    }

    public FileReference getFileReference() {
        return fileReference;
    }

    public String getStorage() {
        return storage;
    }

    public String getChecksum() {
        return checksum;
    }

    public Long getFileSize() {
        return fileSize;
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

    public String getDestinationPath() {
        return destinationPath;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

}
