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
package fr.cnes.regards.modules.fileaccess.dto.availability;

import java.util.Objects;
import java.util.Set;

/**
 * Input request body of availability endpoint
 *
 * @author Thomas GUILLOU
 **/
public class FilesAvailabilityRequestDto {
    
    private final Set<String> checksums;

    public FilesAvailabilityRequestDto(Set<String> checksums) {
        this.checksums = checksums;
    }

    public Set<String> getChecksums() {
        return checksums;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilesAvailabilityRequestDto that = (FilesAvailabilityRequestDto) o;
        return Objects.equals(checksums, that.checksums);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checksums);
    }

    @Override
    public String toString() {
        return "FilesAvailabilityRequestDto{" + "checksums=" + checksums + '}';
    }
}
