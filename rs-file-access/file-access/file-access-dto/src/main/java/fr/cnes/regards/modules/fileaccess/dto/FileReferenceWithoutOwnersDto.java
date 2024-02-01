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
package fr.cnes.regards.modules.fileaccess.dto;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Dto to represent a file referenced in storage catalog.
 * The owners are costly to load. This dto is the light version of {@link FileReferenceDto}
 *
 * @author Sébastien Binda
 */
public class FileReferenceWithoutOwnersDto {

    private Long id;

    private String checksum;

    private String originStorage;

    /**
     * File storage date
     */
    private OffsetDateTime storageDate;

    /**
     * File location information
     */
    private FileLocationDto location;

    /**
     * file meta information
     */
    private FileReferenceMetaInfoDto metaInfo;

    /**
     * Business request identifier associated to the FileReference. Those identifiers are the identifier of file request.
     * See FilesStorageRequestEvent, FilesDeletionEvent and FilesReferenceEvent for more information about
     * file requests.
     */
    private final Set<String> groupIds = new HashSet<>();

    private boolean referenced = false;

    private boolean nearlineConfirmed;

    public FileReferenceWithoutOwnersDto() {
    }

    public FileReferenceWithoutOwnersDto(OffsetDateTime storageDate,
                                         FileReferenceMetaInfoDto metaInfo,
                                         FileLocationDto location) {
        if (metaInfo == null) {
            throw new IllegalArgumentException("metaInfo is required");
        }
        if (location == null) {
            throw new IllegalArgumentException("location is required");
        }

        this.storageDate = storageDate;
        this.checksum = metaInfo.getChecksum();
        this.metaInfo = metaInfo;
        this.location = location;
    }

    public FileReferenceWithoutOwnersDto(Long id,
                                         OffsetDateTime storageDate,
                                         FileReferenceMetaInfoDto metaInfo,
                                         FileLocationDto location,
                                         boolean referenced,
                                         boolean nearlineConfirmed) {
        this(storageDate, metaInfo, location);
        this.checksum = metaInfo.getChecksum();
        this.id = id;
        this.referenced = referenced;
        this.nearlineConfirmed = nearlineConfirmed;
    }

    public FileReferenceWithoutOwnersDto(String checksum,
                                         String originStorage,
                                         FileReferenceMetaInfoDto metaInfo,
                                         FileLocationDto location,
                                         Collection<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            throw new IllegalArgumentException("groupIds is required");
        }
        this.checksum = checksum;
        this.originStorage = originStorage;
        this.metaInfo = metaInfo;
        this.location = location;
        this.groupIds.addAll(groupIds);
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getStorageDate() {
        return storageDate;
    }

    public FileReferenceMetaInfoDto getMetaInfo() {
        return metaInfo;
    }

    public FileLocationDto getLocation() {
        return location;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getOriginStorage() {
        return originStorage;
    }

    public boolean isReferenced() {
        return referenced;
    }

    public boolean isNearlineConfirmed() {
        return nearlineConfirmed;
    }
}
