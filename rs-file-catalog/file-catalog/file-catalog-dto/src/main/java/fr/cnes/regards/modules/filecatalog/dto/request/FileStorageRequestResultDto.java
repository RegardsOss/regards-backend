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
package fr.cnes.regards.modules.filecatalog.dto.request;

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

    private boolean pendingActionRemaining;

    private boolean notifyActionRemainingToAdmin;

    public static FileStorageRequestResultDto build(FileStorageRequestAggregationDto request,
                                                    String storedUrl,
                                                    Long fileSize,
                                                    boolean pendingActionRemaining,
                                                    boolean notifyActionRemainingToAdmin) {
        FileStorageRequestResultDto dto = new FileStorageRequestResultDto();
        dto.request = request;
        dto.storedUrl = storedUrl;
        dto.fileSize = fileSize;
        dto.pendingActionRemaining = pendingActionRemaining;
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

    public boolean isPendingActionRemaining() {
        return pendingActionRemaining;
    }

    public boolean isNotifyActionRemainingToAdmin() {
        return notifyActionRemainingToAdmin;
    }
}
