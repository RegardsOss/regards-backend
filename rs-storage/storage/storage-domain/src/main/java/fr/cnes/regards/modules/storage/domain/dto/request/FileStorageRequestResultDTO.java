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
package fr.cnes.regards.modules.storage.domain.dto.request;

import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;

/**
 * POJO to centralize information about {@link FileStorageRequest} execution results.
 *
 * @author Sébastien Binda
 *
 */
public class FileStorageRequestResultDTO {

    private FileStorageRequest request;

    private boolean error = false;

    private String storedUrl;

    private Long fileSize;

    private String errorCause;

    public static FileStorageRequestResultDTO build(FileStorageRequest request, String storedUrl, Long fileSize) {
        FileStorageRequestResultDTO dto = new FileStorageRequestResultDTO();
        dto.request = request;
        dto.storedUrl = storedUrl;
        dto.fileSize = fileSize;
        return dto;
    }

    public FileStorageRequestResultDTO error(String cause) {
        this.error = true;
        this.errorCause = cause;
        return this;
    }

    public FileStorageRequest getRequest() {
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

}
