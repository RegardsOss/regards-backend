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
package fr.cnes.regards.modules.order.domain.basket;

import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dto.input.DataTypeLight;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;

/**
 * @author Thomas GUILLOU
 **/
public class FileSelectionDescription {

    private Set<DataTypeLight> fileTypes;

    private String fileNamePattern;

    public FileSelectionDescription(Set<DataTypeLight> fileTypes, String fileNamePattern) {
        this.fileTypes = fileTypes;
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
        FileSelectionDescription that = (FileSelectionDescription) o;
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

    public static boolean validate(@NotNull DataFile dataFile, FileSelectionDescription fileSelectionDescription) {
        return respectsDataTypeFilter(dataFile, fileSelectionDescription) && respectsFileNameFilter(dataFile,
                                                                                                    fileSelectionDescription);
    }

    private static boolean respectsFileNameFilter(@NotNull DataFile dataFile,
                                                  FileSelectionDescription fileSelectionDescription) {
        if (fileSelectionDescription != null) {
            if (StringUtils.isNotBlank(fileSelectionDescription.getFileNamePattern())) {
                return dataFile.getFilename().matches(fileSelectionDescription.getFileNamePattern());
            }
        }
        return true;
    }

    private static boolean respectsDataTypeFilter(@NotNull DataFile dataFile,
                                                  FileSelectionDescription fileSelectionDescription) {
        if (fileSelectionDescription != null) {
            if (fileSelectionDescription.getFileTypes().isEmpty()) {
                // means that all dataTypes are allowed
            } else {
                return fileSelectionDescription.getFileTypes()
                                               .stream()
                                               .anyMatch(dataTypeLight -> dataTypeLight.isEquivalent(dataFile.getDataType()));
            }
        }
        return true;
    }
}
