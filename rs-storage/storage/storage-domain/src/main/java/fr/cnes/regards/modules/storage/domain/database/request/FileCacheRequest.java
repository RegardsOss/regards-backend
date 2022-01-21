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
package fr.cnes.regards.modules.storage.domain.database.request;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;

/**
 * Database definition of the table containing the requests for cache files.
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_file_cache_request",
        indexes = { @Index(name = "idx_file_cache_request_grp", columnList = "group_id"),
                @Index(name = "idx_file_cache_request_cs", columnList = "checksum"),
                @Index(name = "idx_file_cache_request_storage", columnList = "storage"),
                @Index(name = "idx_file_cache_file_ref", columnList = "file_ref_id") },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_t_file_cache_request_checksum", columnNames = { "checksum" }) })
public class FileCacheRequest {

    @Id
    @SequenceGenerator(name = "fileCacheRequestSequence", initialValue = 1, sequenceName = "seq_file_cache_request")
    @GeneratedValue(generator = "fileCacheRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Business identifier to regroup file requests.
     */
    @Column(name = "group_id", nullable = false, length = 128)
    private String groupId;

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
    private String restorationDirectory;

    @Column(name = "expiration_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime expirationDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileRequestStatus status = FileRequestStatus.TO_DO;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    @Column(name = "creation_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private final OffsetDateTime creationDate;

    @Column(name = "job_id")
    private String jobId;

    public FileCacheRequest(FileReference fileReference, String restorationDirectory, OffsetDateTime expirationDate,
            String groupId) {
        super();
        this.fileReference = fileReference;
        this.storage = fileReference.getLocation().getStorage();
        this.fileSize = fileReference.getMetaInfo().getFileSize();
        this.checksum = fileReference.getMetaInfo().getChecksum();
        this.restorationDirectory = restorationDirectory;
        this.expirationDate = expirationDate;
        this.groupId = groupId;
        this.creationDate = OffsetDateTime.now();
    }

    public FileCacheRequest() {
        super();
        this.creationDate = OffsetDateTime.now();
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

    public String getRestorationDirectory() {
        return restorationDirectory;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

}
