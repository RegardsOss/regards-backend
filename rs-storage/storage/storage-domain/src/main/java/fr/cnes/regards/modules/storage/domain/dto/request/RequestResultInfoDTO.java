/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.dto.FileLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceMetaInfoDTO;

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
     * Store path of the request if any
     */
    private String requestStorePath;

    /**
     * Storage request file owners.
     */
    private final Set<String> requestOwners = Sets.newHashSet();

    /**
     * Request result file
     */
    private FileReferenceDTO resultFile;

    /**
     * Request error cause
     */
    private String errorCause;

    public static RequestResultInfoDTO build(String groupId, String checksum, String storage, String storePath,
            Collection<String> owners, FileReference fileReference, String errorCause) {
        RequestResultInfoDTO dto = new RequestResultInfoDTO();
        dto.groupId = groupId;
        dto.requestChecksum = checksum;
        dto.requestStorage = storage;
        dto.requestStorePath = storePath;
        if ((owners != null) && !owners.isEmpty()) {
            dto.requestOwners.addAll(owners);
        }
        if (fileReference != null) {
            dto.resultFile = FileReferenceDTO
                    .build(fileReference.getStorageDate(), FileReferenceMetaInfoDTO.build(fileReference.getMetaInfo()),
                           FileLocationDTO.build(fileReference.getLocation()), fileReference.getOwners());
        }
        dto.errorCause = errorCause;
        return dto;
    }

    public static RequestResultInfoDTO build(String groupId, String checksum, String storage, String storePath,
            Collection<String> owners, FileReferenceDTO resultFile, String errorCause) {
        RequestResultInfoDTO dto = new RequestResultInfoDTO();
        dto.groupId = groupId;
        dto.requestChecksum = checksum;
        dto.requestStorage = storage;
        dto.requestStorePath = storePath;
        dto.resultFile = resultFile;
        dto.errorCause = errorCause;
        if ((owners != null) && !owners.isEmpty()) {
            dto.requestOwners.addAll(owners);
        }
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

    public String getRequestStorePath() {
        return requestStorePath;
    }

    public Set<String> getRequestOwners() {
        return requestOwners;
    }

}
