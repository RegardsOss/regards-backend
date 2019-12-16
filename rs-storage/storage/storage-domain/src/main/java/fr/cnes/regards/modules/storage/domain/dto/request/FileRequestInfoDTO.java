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
package fr.cnes.regards.modules.storage.domain.dto.request;

import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;

/**
 * DTO to handle common informations about file requests of all types {@link FileRequestType}s
 *
 * @author Sébastien Binda
 *
 */
public class FileRequestInfoDTO {

    private String fileName;

    /**
     * Request type
     */
    private FileRequestType type;

    /**
     * Requests status
     */
    private FileRequestStatus status;

    /**
     * Requests error cause if any
     */
    private String errorCause;

    public static FileRequestInfoDTO build(String fileName, FileRequestType type, FileRequestStatus status,
            String errorCause) {
        FileRequestInfoDTO dto = new FileRequestInfoDTO();
        dto.type = type;
        dto.status = status;
        dto.errorCause = errorCause;
        dto.fileName = fileName;
        return dto;
    }

    public FileRequestType getType() {
        return type;
    }

    public FileRequestStatus getStatus() {
        return status;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public String getFileName() {
        return fileName;
    }

}
