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
package fr.cnes.regards.modules.storagelight.domain.dto.request;

import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.dto.FileLocationDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceMetaInfoDTO;

/**
 * DTO represents results information about request from a group of requests.
 * @author Sébastien Binda
 */
public class RequestResultInfoDTO {

    /**
     * Group request business provided identifier
     */
    private String groupId;

    /**
     * Checksum of the requested file
     */
    private String requestChecksum;

    /**
     * Storage of the requested file
     */
    private String requestStorage;

    /**
     * Request result file
     */
    private FileReferenceDTO resultFile;

    /**
     * Request error cause
     */
    private String errorCause;

    public static RequestResultInfoDTO build(String groupId, String checksum, String storage,
            FileReference fileReference, String errorCause) {
        RequestResultInfoDTO dto = new RequestResultInfoDTO();
        dto.groupId = groupId;
        dto.requestChecksum = checksum;
        dto.requestStorage = storage;
        if (fileReference != null) {
            dto.resultFile = FileReferenceDTO
                    .build(fileReference.getStorageDate(), FileReferenceMetaInfoDTO.build(fileReference.getMetaInfo()),
                           FileLocationDTO.build(fileReference.getLocation()), fileReference.getOwners());
        }
        dto.errorCause = errorCause;
        return dto;
    }

    public static RequestResultInfoDTO build(String groupId, String checksum, String storage,
            FileReferenceDTO resultFile, String errorCause) {
        RequestResultInfoDTO dto = new RequestResultInfoDTO();
        dto.groupId = groupId;
        dto.requestChecksum = checksum;
        dto.requestStorage = storage;
        dto.resultFile = resultFile;
        dto.errorCause = errorCause;
        return dto;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getRequestChecksum() {
        return requestChecksum;
    }

    public String getRequestStorage() {
        return requestStorage;
    }

    public FileReferenceDTO getResultFile() {
        return resultFile;
    }

    public String getErrorCause() {
        return errorCause;
    }

}
