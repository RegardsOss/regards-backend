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
package fr.cnes.regards.modules.order.domain.dto;

import fr.cnes.regards.modules.order.dto.input.DataTypeLight;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * @author Thomas GUILLOU
 **/
public class FileSelectionDescriptionDTO {

    private Set<DataTypeLight> fileTypes;

    private String fileNamePattern;

    public FileSelectionDescriptionDTO(Set<DataTypeLight> fileTypes, String fileNamePattern) {
        this.fileTypes = fileTypes != null ? fileTypes : Collections.emptySet();
        this.fileNamePattern = fileNamePattern;
    }

    public Set<DataTypeLight> getFileTypes() {
        return fileTypes;
    }

    public void setFileTypes(Set<DataTypeLight> fileTypes) {
        this.fileTypes = fileTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileSelectionDescriptionDTO that = (FileSelectionDescriptionDTO) o;
        return Objects.equals(fileTypes, that.fileTypes) && Objects.equals(fileNamePattern, that.fileNamePattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileTypes, fileNamePattern);
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    @Override
    public String toString() {
        return "FileSelectionDescription{"
               + "fileTypes="
               + fileTypes
               + ", fileNamePattern='"
               + fileNamePattern
               + '\''
               + '}';
    }
}
