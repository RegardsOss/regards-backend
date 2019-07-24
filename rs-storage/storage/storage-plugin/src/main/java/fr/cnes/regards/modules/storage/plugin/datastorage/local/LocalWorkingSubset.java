/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.plugin.datastorage.local;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

/**
 * {@link IWorkingSubset} implementation for {@link LocalDataStorage}
 * @author svissier
 */
public class LocalWorkingSubset implements IWorkingSubset {

    /**
     * Data files from this working subset
     */
    private Set<StorageDataFile> dataFiles;

    /**
     * Default constructor
     */
    public LocalWorkingSubset() {
    }

    /**
     * Constructor setting the parameter as attribute
     * @param dataFiles
     */
    public LocalWorkingSubset(Set<StorageDataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }

    @Override
    public Set<StorageDataFile> getDataFiles() {
        return dataFiles;
    }

    /**
     * Set the data files
     * @param dataFiles
     */
    public void setDataFiles(Set<StorageDataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }
}
