/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.dto.files;

import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Information about a list of files going to be stored (physically)
 *
 * @author Thibaud Michaudel
 **/
public class FilesStorageRequestDto {

    /**
     * Information about files to store
     */
    private final Set<FileStorageRequestDto> files = new HashSet<>();

    /**
     * Request business identifier
     */
    private String groupId;

    public FilesStorageRequestDto() {

    }

    public FilesStorageRequestDto(String groupId, Set<FileStorageRequestDto> files) {
        this.groupId = groupId;
        this.files.addAll(files);
    }

    public String getGroupId() {
        return groupId;
    }

    public Set<FileStorageRequestDto> getFiles() {
        return files;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilesStorageRequestDto that = (FilesStorageRequestDto) o;
        return Objects.equals(files, that.files) && Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(files, groupId);
    }

    @Override
    public String toString() {
        return "FilesStorageRequestDto [" + (files != null ? "files=" + files + ", " : "") + (groupId != null ?
            "groupId=" + groupId :
            "") + "]";
    }

}
