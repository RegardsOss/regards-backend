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
package fr.cnes.regards.modules.fileaccess.dto.files;

import fr.cnes.regards.modules.fileaccess.dto.request.FileCopyDto;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Information about a list of files going to be copied
 *
 * @author Thibaud Michaudel
 **/
public class FilesCopyDto {

    /**
     * Files to delete information
     */
    private final Set<FileCopyDto> files = new HashSet<>();

    /**
     * Business request identifier
     */
    private String groupId;

    public FilesCopyDto(String groupId, Set<FileCopyDto> files) {
        if (groupId == null) {
            throw new IllegalArgumentException("groupId is required");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files is required");
        }
        this.files.addAll(files);
        this.groupId = groupId;
    }

    public FilesCopyDto() {

    }

    public String getGroupId() {
        return groupId;
    }

    public Set<FileCopyDto> getFiles() {
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
        FilesCopyDto that = (FilesCopyDto) o;
        return Objects.equals(files, that.files) && Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(files, groupId);
    }

    @Override
    public String toString() {
        return "FilesCopyDto [" + "files=" + files + ", " + (groupId != null ? "groupId=" + groupId : "") + "]";
    }
}
