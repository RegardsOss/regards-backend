/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.domain.metadata.dto;

import java.util.HashSet;
import java.util.Set;

import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;

/**
 * {@link Set} of {@link MetaFileDto} 
 * 
 * @author Christophe Mertz
 *
 */
public class SetOfMetaFileDto {

    private Set<MetaFileDto> setOfMetaFiles = new HashSet<MetaFileDto>();

    public void addMetaFileDto(MetaFileDto dto) {
        setOfMetaFiles.add(dto);
    }

    public Set<MetaFileDto> getSetOfMetaFiles() {
        return setOfMetaFiles;
    }

    public static SetOfMetaFileDto fromSetOfMetaFile(Set<MetaFile> metaFiles) {
        SetOfMetaFileDto dto = new SetOfMetaFileDto();

        for (MetaFile metaFile : metaFiles) {
            dto.addMetaFileDto(MetaFileDto.fromMetaFile(metaFile));
        }

        return dto;
    }

}