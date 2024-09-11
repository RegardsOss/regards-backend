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
package fr.cnes.regards.modules.fileaccess.dto.request;

import fr.cnes.regards.modules.fileaccess.dto.FileArchiveStatus;

import java.util.Objects;

/**
 * POJO to centralize information about FileStorageRequest execution results.
 *
 * @author Sébastien Binda
 */
public class FileStorageRequestResultDto {

    private FileStorageRequestAggregationDto request;

    private boolean error = false;

    private String storedUrl;

    private Long fileSize;

    private String errorCause;

    private FileArchiveStatus fileArchiveStatus;

    private boolean notifyActionRemainingToAdmin;

    public static FileStorageRequestResultDto build(FileStorageRequestAggregationDto request,
                                                    String storedUrl,
                                                    Long fileSize,
                                                    FileArchiveStatus fileArchiveStatus,
                                                    boolean notifyActionRemainingToAdmin) {
        FileStorageRequestResultDto dto = new FileStorageRequestResultDto();
        dto.request = request;
        dto.storedUrl = storedUrl;
        dto.fileSize = fileSize;
        dto.fileArchiveStatus = fileArchiveStatus;
        dto.notifyActionRemainingToAdmin = notifyActionRemainingToAdmin;
        return dto;
    }

    /**
     * Build for legacy storage where the archive status is just a boolean (STORED / TO_STORE)
     */
    public static FileStorageRequestResultDto build(FileStorageRequestAggregationDto request,
                                                    String storedUrl,
                                                    Long fileSize,
                                                    boolean pendingActionRemaining,
                                                    boolean notifyActionRemainingToAdmin) {
        FileStorageRequestResultDto dto = new FileStorageRequestResultDto();
        dto.request = request;
        dto.storedUrl = storedUrl;
        dto.fileSize = fileSize;
        dto.fileArchiveStatus = pendingActionRemaining ? FileArchiveStatus.TO_STORE : FileArchiveStatus.STORED;
        dto.notifyActionRemainingToAdmin = notifyActionRemainingToAdmin;
        return dto;
    }

    public FileStorageRequestResultDto error(String cause) {
        this.error = true;
        this.errorCause = cause;
        return this;
    }

    public FileStorageRequestAggregationDto getRequest() {
        return request;
    }

    public String getStoredUrl() {
        return storedUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public boolean isError() {
        return error;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public boolean isNotifyActionRemainingToAdmin() {
        return notifyActionRemainingToAdmin;
    }

    public FileArchiveStatus getFileArchiveStatus() {
        return fileArchiveStatus;
    }

    @Override
    public String toString() {
        return "FileStorageRequestResultDto{"
               + "request="
               + request
               + ", error="
               + error
               + ", storedUrl='"
               + storedUrl
               + '\''
               + ", fileSize="
               + fileSize
               + ", errorCause='"
               + errorCause
               + '\''
               + ", fileArchiveStatus="
               + fileArchiveStatus
               + ", notifyActionRemainingToAdmin="
               + notifyActionRemainingToAdmin
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileStorageRequestResultDto that = (FileStorageRequestResultDto) o;
        return error == that.error
               && notifyActionRemainingToAdmin == that.notifyActionRemainingToAdmin
               && Objects.equals(request, that.request)
               && Objects.equals(storedUrl, that.storedUrl)
               && Objects.equals(fileSize, that.fileSize)
               && Objects.equals(errorCause, that.errorCause)
               && fileArchiveStatus == that.fileArchiveStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(request,
                            error,
                            storedUrl,
                            fileSize,
                            errorCause,
                            fileArchiveStatus,
                            notifyActionRemainingToAdmin);
    }

    /**
     * Method for the legacy storage where the archive status is just a boolean (STORED / TO_STORE)
     */
    public boolean isPendingActionRemaining() {
        return FileArchiveStatus.TO_STORE.equals(fileArchiveStatus);
    }
}
