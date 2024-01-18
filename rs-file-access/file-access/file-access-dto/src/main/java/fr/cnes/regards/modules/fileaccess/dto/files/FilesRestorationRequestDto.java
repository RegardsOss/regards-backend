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

import java.util.HashSet;
import java.util.Set;

/**
 * @author Thibaud Michaudel
 **/
public class FilesRestorationRequestDto {

    /**
     * Checksums of files to make available for download
     */
    private final Set<String> checksums = new HashSet<>();

    /**
     * Number of hours for files availability
     */
    private int availabilityHours;

    /**
     * Request business identifier
     */
    private String groupId;

    public FilesRestorationRequestDto(int availabilityHours, String groupId, Set<String> checksums) {
        if (groupId == null) {
            throw new RuntimeException("groupId is required");
        }
        if (checksums == null || checksums.isEmpty()) {
            throw new RuntimeException("checksums is required");
        }
        this.availabilityHours = availabilityHours;
        this.groupId = groupId;
        this.checksums.addAll(checksums);
    }

    public FilesRestorationRequestDto() {

    }

    public Set<String> getChecksums() {
        return checksums;
    }

    public int getAvailabilityHours() {
        return availabilityHours;
    }

    public String getGroupId() {
        return groupId;
    }
}
