/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.dto;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * DTO to represent a file referenced in storage catalog.
 * @author Sébastien Binda
 */
public class FileReferenceDTO {

    /**
     * File storage date
     */
    private OffsetDateTime storageDate;

    /**
     * Owners of the file
     */
    private final List<String> owners = Lists.newArrayList();

    /**
     * file meta information
     */
    private FileReferenceMetaInfoDTO metaInfo;

    /**
     * File location information
     */
    private FileLocationDTO location;

    public static FileReferenceDTO build(OffsetDateTime storageDate, FileReferenceMetaInfoDTO metaInfo,
            FileLocationDTO location, Collection<String> owners) {
        FileReferenceDTO dto = new FileReferenceDTO();
        dto.storageDate = storageDate;
        dto.metaInfo = metaInfo;
        dto.location = location;
        dto.owners.addAll(owners);
        return dto;
    }

    public OffsetDateTime getStorageDate() {
        return storageDate;
    }

    public List<String> getOwners() {
        return owners;
    }

    public FileReferenceMetaInfoDTO getMetaInfo() {
        return metaInfo;
    }

    public FileLocationDTO getLocation() {
        return location;
    }

}
