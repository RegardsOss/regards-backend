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

import fr.cnes.regards.modules.filecatalog.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestStatus;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Dto for file copy requests.
 *
 * @author Thibaud Michaudel
 */
public class FileCopyRequestDto {

    @NotNull
    private final Long id;

    /**
     * Business identifier to regroup file requests.
     */
    @NotNull
    private final String groupId;

    private final FileReferenceMetaInfoDto metaInfo;

    @Size(max = FileAccessConstants.URL_MAX_LENGTH)
    private final String storageSubDirectory;

    @Size(max = FileAccessConstants.STORAGE_MAX_LENGTH)
    private final String storage;

    @Size(max = 128)
    private final String fileCacheGroupId;

    @Size(max = 128)
    private final String fileStorageGroupId;

    @NotNull
    private final FileRequestStatus status;

    @Size(max = 512)
    private final String errorCause;

    private final OffsetDateTime creationDate;

    private final String sessionOwner;

    private final String session;

    public FileCopyRequestDto(Long id,
                              String groupId,
                              FileReferenceMetaInfoDto metaInfo,
                              String storageSubDirectory,
                              String storage,
                              String fileCacheGroupId,
                              String fileStorageGroupId,
                              FileRequestStatus status,
                              String errorCause,
                              OffsetDateTime creationDate,
                              String sessionOwner,
                              String session) {
        this.id = id;
        this.groupId = groupId;
        this.metaInfo = metaInfo;
        this.storageSubDirectory = storageSubDirectory;
        this.storage = storage;
        this.fileCacheGroupId = fileCacheGroupId;
        this.fileStorageGroupId = fileStorageGroupId;
        if (status == null) {
            this.status = FileRequestStatus.TO_DO;
        } else {
            this.status = status;
        }
        this.errorCause = errorCause;
        this.creationDate = creationDate;
        this.sessionOwner = sessionOwner;
        this.session = session;
    }

    public Long getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public FileReferenceMetaInfoDto getMetaInfo() {
        return metaInfo;
    }

    public String getStorageSubDirectory() {
        return storageSubDirectory;
    }

    public String getStorage() {
        return storage;
    }

    public String getFileCacheGroupId() {
        return fileCacheGroupId;
    }

    public String getFileStorageGroupId() {
        return fileStorageGroupId;
    }

    public FileRequestStatus getStatus() {
        return status;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public String getSession() {
        return session;
    }
}
