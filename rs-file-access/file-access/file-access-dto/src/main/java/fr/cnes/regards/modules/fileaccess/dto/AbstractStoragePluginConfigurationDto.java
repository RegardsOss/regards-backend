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
package fr.cnes.regards.modules.fileaccess.dto;

import fr.cnes.regards.modules.fileaccess.dto.output.worker.FileNamingStrategy;

/**
 * Configuration dto abstract to share from the storage plugin to the workers
 *
 * @author Thibaud Michaudel
 **/
public abstract class AbstractStoragePluginConfigurationDto {

    /**
     * Strategy to name the file to store. If the file is an archive, the {@link FileNamingStrategy#FILENAME}
     * strategy must be used.
     */
    protected FileNamingStrategy fileNamingStrategy;

    protected AbstractStoragePluginConfigurationDto(FileNamingStrategy fileNamingStrategy) {
        this.fileNamingStrategy = fileNamingStrategy;
    }

    protected AbstractStoragePluginConfigurationDto() {
        this(FileNamingStrategy.CHECKSUM);
    }

    public FileNamingStrategy getFileNamingStrategy() {
        return fileNamingStrategy;
    }

}
