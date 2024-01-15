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

import fr.cnes.regards.modules.filecatalog.dto.FileReferenceWithoutOwnersDto;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestStatus;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Dto for file deletion requests.
 *
 * @author Thibaud Michaudel
 */
public class FileDeletionRequestDto {

    @NotNull
    private final Long id;

    /**
     * Business identifier to regroup file requests.
     */
    @NotNull
    private final String groupId;

    @NotNull
    private final FileRequestStatus status;

    @NotNull
    @Size(max = FileAccessConstants.STORAGE_MAX_LENGTH)
    private String storage;

    @NotNull
    private FileReferenceWithoutOwnersDto fileReference;

    private boolean forceDelete = false;

    @Size(max = 512)
    private String errorCause;

    private final OffsetDateTime creationDate;

    private final String jobId;

    private final String sessionOwner;

    private final String session;

    public FileDeletionRequestDto(Long id,
                                  String groupId,
                                  FileRequestStatus status,
                                  String storage,
                                  FileReferenceWithoutOwnersDto fileReference,
                                  boolean forceDelete,
                                  String errorCause,
                                  OffsetDateTime creationDate,
                                  String jobId,
                                  String sessionOwner,
                                  String session) {
        this.id = id;
        this.groupId = groupId;
        this.status = status;
        this.storage = storage;
        this.fileReference = fileReference;
        this.forceDelete = forceDelete;
        this.errorCause = errorCause;
        this.creationDate = creationDate;
        this.jobId = jobId;
        this.sessionOwner = sessionOwner;
        this.session = session;
    }

    public Long getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public FileRequestStatus getStatus() {
        return status;
    }

    public String getStorage() {
        return storage;
    }

    public FileReferenceWithoutOwnersDto getFileReference() {
        return fileReference;
    }

    public boolean isForceDelete() {
        return forceDelete;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public String getJobId() {
        return jobId;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public String getSession() {
        return session;
    }
}
