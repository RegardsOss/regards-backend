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
package fr.cnes.regards.modules.storage.domain.dto;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;

import java.util.Collection;
import java.util.Set;

/**
 * POJO to handle copy files requests parameters.
 *
 * @author Sébastien Binda
 */
public class CopyFilesParametersDTO {

    /**
     * Information about files to copy source location
     */
    private FileLocationDTO from;

    /**
     * Information about files to copy destination location
     */
    private FileLocationDTO to;

    /**
     * List of {@link FileReferenceMetaInfo#getType()} to copy
     */
    private final Set<String> types = Sets.newHashSet();

    /**
     * Build a copy request parameters object.
     *
     * @param sourceStorage      source storage location name
     * @param sourcePath         source path recursively copy
     * @param destinationStorage destination storage location name
     * @param destinationPath    destination path to copy to
     * @return {@link CopyFilesParametersDTO}
     */
    public static CopyFilesParametersDTO build(String sourceStorage,
                                               String sourcePath,
                                               String destinationStorage,
                                               String destinationPath,
                                               Collection<String> types) {
        CopyFilesParametersDTO dto = new CopyFilesParametersDTO();
        dto.from = FileLocationDTO.build(sourceStorage, sourcePath);
        dto.to = FileLocationDTO.build(destinationStorage, destinationPath);
        if (types != null) {
            dto.types.addAll(types);
        }
        return dto;
    }

    public FileLocationDTO getFrom() {
        return from;
    }

    public FileLocationDTO getTo() {
        return to;
    }

    public Set<String> getTypes() {
        return types;
    }

}
