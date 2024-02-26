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
package fr.cnes.regards.modules.fileaccess.plugin.dto;

import fr.cnes.regards.modules.fileaccess.dto.FileReferenceWithoutOwnersDto;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Dto for file cache requests.
 *
 * @author Thibaud Michaudel
 */
public class FileCacheRequestDto {

    @NotNull
    private final Long id;

    @NotNull
    private final Set<String> groupIds;

    @NotNull
    private final FileReferenceWithoutOwnersDto fileReference;

    @NotNull
    private final String checksum;

    @NotNull
    private final String storage;

    @NotNull
    private final Long fileSize;

    @NotNull
    private final String restorationDirectory;

    private final int availabilityHours;

    @NotNull
    private final FileRequestStatus status;

    @Size(max = 512)
    private final String errorCause;

    private final OffsetDateTime creationDate;

    private final String jobId;

    public FileCacheRequestDto(Long id,
                               Set<String> groupIds,
                               FileReferenceWithoutOwnersDto fileReference,
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
        this.groupIds = groupIds;
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

    public FileReferenceWithoutOwnersDto getFileReference() {
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

    public FileRequestStatus getStatus() {
        return status;
    }

    public int getAvailabilityHours() {
        return availabilityHours;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    public String getJobId() {
        return jobId;
    }

    public String getRestorationDirectory() {
        return restorationDirectory;
    }
}
