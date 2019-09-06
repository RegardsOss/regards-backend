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
package fr.cnes.regards.modules.storagelight.domain.dto.request.group;

import fr.cnes.regards.modules.storagelight.domain.database.FileReference;

/**
 * @author sbinda
 *
 */
public class GroupRequestInfoDTO {

    private String groupId;

    private String checksum;

    private String storage;

    private FileReference fileReference;

    private String errorCause;

    public static GroupRequestInfoDTO build(String groupId, String checksum, String storage,
            FileReference fileReference, String errorCause) {
        GroupRequestInfoDTO dto = new GroupRequestInfoDTO();
        dto.groupId = groupId;
        dto.checksum = checksum;
        dto.storage = storage;
        dto.fileReference = fileReference;
        dto.errorCause = errorCause;
        return dto;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getStorage() {
        return storage;
    }

    public FileReference getFileReference() {
        return fileReference;
    }

    public String getErrorCause() {
        return errorCause;
    }

}
