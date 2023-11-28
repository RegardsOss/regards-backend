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
package fr.cnes.regards.modules.filecatalog.dto.files;

import fr.cnes.regards.modules.filecatalog.dto.request.FileCopyRequestDto;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Thibaud Michaudel
 **/
public class FilesCopyDto {

    /**
     * Files to delete information
     */
    private final Set<FileCopyRequestDto> files = new HashSet<>();

    /**
     * Business request identifier
     */
    private String groupId;

    public FilesCopyDto(String groupId, Set<FileCopyRequestDto> files) {
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

    public Set<FileCopyRequestDto> getFiles() {
        return files;
    }

    @Override
    public String toString() {
        return "DeleteFileRefFlowItem ["
               + "files="
               + files
               + ", "
               + (groupId != null ? "groupId=" + groupId : "")
               + "]";
    }
}
