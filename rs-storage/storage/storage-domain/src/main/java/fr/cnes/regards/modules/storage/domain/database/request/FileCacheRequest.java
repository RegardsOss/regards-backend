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

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileCacheRequestDto;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Database definition of the table containing the requests for cache files.
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_file_cache_request",
       indexes = { @Index(name = "idx_file_cache_request_cs", columnList = "checksum"),
                   @Index(name = "idx_file_cache_request_storage", columnList = "storage"),
                   @Index(name = "idx_file_cache_file_ref", columnList = "file_ref_id") })
public class FileCacheRequest {

    @Id
    @SequenceGenerator(name = "fileCacheRequestSequence", initialValue = 1, sequenceName = "seq_file_cache_request")
    @GeneratedValue(generator = "fileCacheRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Business identifier to regroup file requests.
     */
    @Column(name = "group_id")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ta_file_cache_request_group_id",
                     joinColumns = @JoinColumn(name = "file_cache_request_id",
                                               foreignKey = @ForeignKey(name = "fk_ta_file_cache_request_group_id_file_cache_request_id")))

    private final Set<String> groupIds = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "file_ref_id", nullable = false)
    private FileReference fileReference;

    @Column(name = "checksum", length = FileReferenceMetaInfo.CHECKSUM_MAX_LENGTH, nullable = false, unique = true)
    private String checksum;

    @Column(name = "storage", length = FileLocation.STORAGE_MAX_LENGTH, nullable = false)
    private String storage;

    /**
     * File size provided by the request (be careful, this data is not maybe the real size of file).
     * For the unit {@link FileReferenceMetaInfo#fileSize}.
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * Restoration directory for the internal cache.
     * This directory will not be useful for external cache but the request can't know if the file will be restored in
     * internal cache or external cache, so the directory is always set.
     */
    @Column(name = "destination_path", length = FileLocation.URL_MAX_LENGTH, nullable = false)
    private String restorationDirectory;

    /**
     * Duration in hours of available files in the cache internal or external (by default 24h)
     */
    @Column(name = "availability_hours", nullable = false)
    private int availabilityHours = 24;

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

    public FileCacheRequest(FileReference fileReference,
                            String restorationDirectory,
                            int availabilityHours,
                            Set<String> groupIds) {
        super();
        this.fileReference = fileReference;
        this.storage = fileReference.getLocation().getStorage();
        this.fileSize = fileReference.getMetaInfo().getFileSize();
        this.checksum = fileReference.getMetaInfo().getChecksum();
        this.restorationDirectory = restorationDirectory;
        this.availabilityHours = availabilityHours;
        if (groupIds != null) {
            this.groupIds.addAll(groupIds);
        }
        this.creationDate = OffsetDateTime.now();
    }

    public FileCacheRequest(FileReference fileReference,
                            String restorationDirectory,
                            int availabilityHours,
                            String groupId) {
        this(fileReference, restorationDirectory, availabilityHours, Set.of(groupId));
    }

    public FileCacheRequest() {
        super();
        this.creationDate = OffsetDateTime.now();
    }

    private FileCacheRequest(Long id,
                             Set<String> groupIds,
                             FileReference fileReference,
                             String checksum,
                             String storage,
                             Long fileSize,
                             String restorationDirectory,
                             int availabilityHours,
                             FileRequestStatus status,
                             String errorCause,
                             OffsetDateTime creationDate,
                             String jobId) {
        this.id = id;
        if (groupIds != null) {
            this.groupIds.addAll(groupIds);
        }
        this.fileReference = fileReference;
        this.checksum = checksum;
        this.storage = storage;
        this.fileSize = fileSize;
        this.restorationDirectory = restorationDirectory;
        this.availabilityHours = availabilityHours;
        this.status = status;
        this.errorCause = errorCause;
        this.creationDate = creationDate;
        this.jobId = jobId;
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
        if (errorCause != null && errorCause.length() > 512) {
            this.errorCause = errorCause.substring(0, 511);
        } else {
            this.errorCause = errorCause;
        }
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

    public int getAvailabilityHours() {
        return availabilityHours;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setAvailabilityHours(int availabilityHours) {
        this.availabilityHours = availabilityHours;
    }

    public Set<String> getGroupIds() {
        return groupIds;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileCacheRequest that = (FileCacheRequest) o;
        return Objects.equals(groupIds, that.groupIds) && Objects.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupIds, checksum);
    }

    public FileCacheRequestDto toDto() {
        return new FileCacheRequestDto(id,
                                       groupIds,
                                       fileReference.toDtoWithoutOwners(),
                                       checksum,
                                       storage,
                                       fileSize,
                                       restorationDirectory,
                                       availabilityHours,
                                       status,
                                       errorCause,
                                       creationDate,
                                       jobId);
    }

    public static FileCacheRequest fromDto(FileCacheRequestDto dto) {
        return new FileCacheRequest(dto.getId(),
                                    dto.getGroupIds(),
                                    FileReference.fromDto(dto.getFileReference()),
                                    dto.getChecksum(),
                                    dto.getStorage(),
                                    dto.getFileSize(),
                                    dto.getRestorationDirectory(),
                                    dto.getAvailabilityHours(),
                                    dto.getStatus(),
                                    dto.getErrorCause(),
                                    dto.getCreationDate(),
                                    dto.getJobId());
    }
}
