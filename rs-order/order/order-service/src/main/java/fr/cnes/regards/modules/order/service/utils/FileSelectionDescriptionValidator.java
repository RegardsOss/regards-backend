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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.order.service.utils;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dto.dto.FileSelectionDescription;
import fr.cnes.regards.modules.order.dto.input.DataTypeLight;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

/**
 * @author tguillou
 */
public final class FileSelectionDescriptionValidator {

    private FileSelectionDescriptionValidator() {
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
                                               .anyMatch(dataTypeLight -> isDataTypeEquivalent(dataTypeLight,
                                                                                               dataFile.getDataType()));
            }
        }
        return true;
    }

    public static boolean isDataTypeEquivalent(DataTypeLight dataTypeLight, DataType dataType) {
        return switch (dataType) {
            case QUICKLOOK_SD, QUICKLOOK_HD, QUICKLOOK_MD -> dataTypeLight == DataTypeLight.QUICKLOOK;
            case RAWDATA -> dataTypeLight == DataTypeLight.RAWDATA;
            default -> false;
        };
    }
}
