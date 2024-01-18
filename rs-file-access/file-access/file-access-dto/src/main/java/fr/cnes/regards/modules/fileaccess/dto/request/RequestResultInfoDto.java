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
package fr.cnes.regards.modules.fileaccess.dto.request;

import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Dto represents results information about request from a group of requests.
 *
 * @author Sébastien Binda
 */
public class RequestResultInfoDto {

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
    private final Set<String> requestOwners = new HashSet<>();

    /**
     * Request result file
     */
    private FileReferenceDto resultFile;

    /**
     * Request error cause
     */
    private String errorCause;

    public static RequestResultInfoDto build(String groupId,
                                             String checksum,
                                             String storage,
                                             String storePath,
                                             Collection<String> owners,
                                             FileReferenceDto resultFile,
                                             String errorCause) {
        RequestResultInfoDto dto = new RequestResultInfoDto();
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

    public static RequestResultInfoDto build(String groupId, String errorCause) {
        RequestResultInfoDto dto = new RequestResultInfoDto();
        dto.groupId = groupId;
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

    public FileReferenceDto getResultFile() {
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

    @Override
    public String toString() {
        return "RequestResultInfoDTO{"
               + "groupId='"
               + groupId
               + '\''
               + ", requestChecksum='"
               + requestChecksum
               + '\''
               + ", requestStorage='"
               + requestStorage
               + '\''
               + ", requestStorePath='"
               + requestStorePath
               + '\''
               + ", requestOwners="
               + requestOwners
               + ", resultFile="
               + resultFile
               + ", errorCause='"
               + errorCause
               + '\''
               + '}';
    }
}
